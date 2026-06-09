package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InteractiveWorkspace {

    private final BufferedReader input;
    private final PrintStream output;
    private final Session session;
    private final WorkspaceDashboardRenderer renderer;

    public InteractiveWorkspace(InputStream input, OutputStream output) {
        this(input, output, defaultSession());
    }

    public InteractiveWorkspace(InputStream input, OutputStream output, Session session) {
        this(new BufferedReader(new InputStreamReader(input)), new PrintStream(output), session, new WorkspaceDashboardRenderer());
    }

    private InteractiveWorkspace(BufferedReader input, PrintStream output, Session session, WorkspaceDashboardRenderer renderer) {
        this.input = input;
        this.output = output;
        this.session = session;
        this.renderer = renderer;
    }

    public int run() throws IOException {
        output.print(renderer.render(session.dashboardState()));
        String line;
        while ((line = input.readLine()) != null) {
            WorkspaceCommand command = WorkspaceCommand.parse(line);
            if (command.type() == WorkspaceCommand.Type.HELP) {
                output.println(WorkspaceDashboardRenderer.commandHints());
            } else if (command.type() == WorkspaceCommand.Type.EXIT) {
                output.println("Goodbye");
                return 0;
            } else if (command.type() == WorkspaceCommand.Type.CONNECTIONS) {
                output.print(session.renderConnections());
            } else if (command.type() == WorkspaceCommand.Type.USE) {
                output.println(session.useConnectionCommand(command));
            } else if (command.type() == WorkspaceCommand.Type.NEW_CONNECTION) {
                output.println(session.createConnectionCommand(command));
            } else if (command.type() == WorkspaceCommand.Type.BUFFER) {
                output.println(session.bufferCommand(command));
            } else if (command.type() == WorkspaceCommand.Type.RUN) {
                output.println(session.runCurrentBuffer(command.arguments()));
            } else if (isLibrary(command.type())) {
                output.println(session.libraryCommand(command));
            } else if (command.type() == WorkspaceCommand.Type.HISTORY) {
                output.print(session.renderHistory());
            } else if (isMetadata(command.type())) {
                output.println(session.metadataCommand(command));
            } else if (command.type() != WorkspaceCommand.Type.EMPTY) {
                output.println("Command is not implemented yet: " + command.type());
            }
            output.print(renderer.render(session.dashboardState()));
        }
        output.println("Goodbye");
        return 0;
    }

    private static boolean isMetadata(WorkspaceCommand.Type type) {
        return (
            type == WorkspaceCommand.Type.TABLES ||
            type == WorkspaceCommand.Type.SEARCH ||
            type == WorkspaceCommand.Type.DESC ||
            type == WorkspaceCommand.Type.DETAILS ||
            type == WorkspaceCommand.Type.INDEXES ||
            type == WorkspaceCommand.Type.CONSTRAINTS ||
            type == WorkspaceCommand.Type.FK_IN ||
            type == WorkspaceCommand.Type.FK_OUT ||
            type == WorkspaceCommand.Type.EXPLAIN ||
            type == WorkspaceCommand.Type.COUNT ||
            type == WorkspaceCommand.Type.SAMPLE
        );
    }

    private static boolean isLibrary(WorkspaceCommand.Type type) {
        return (
            type == WorkspaceCommand.Type.LIB_SAVE ||
            type == WorkspaceCommand.Type.LIB_LIST ||
            type == WorkspaceCommand.Type.LIB_SEARCH ||
            type == WorkspaceCommand.Type.LIB_LOAD ||
            type == WorkspaceCommand.Type.LIB_DELETE ||
            type == WorkspaceCommand.Type.LIB_FAVORITE ||
            type == WorkspaceCommand.Type.LIB_UNFAVORITE
        );
    }

    public static Session defaultSession() {
        File baseDirectory = new File(System.getProperty("user.home"), ".oracle-script-cli/connections");
        ConnectionRegistry registry = new ConnectionRegistry(baseDirectory, new ProtectedSecretStore(new File(baseDirectory, "secrets")));
        EditorStateStore editorStore = new EditorStateStore(
            new File(System.getProperty("user.home"), ".oracle-script-cli/editor.properties"),
            30
        );
        try {
            return new Session(registry, editorStore, new DefaultWorkspaceSqlExecutor(), new QueryLibraryStore());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize workspace session", ex);
        }
    }

    public interface WorkspaceSqlExecutor {
        SqlExecutionResult executeSingle(ConnectionConfig config, String statement) throws SQLException;
    }

    private static final class DefaultWorkspaceSqlExecutor implements WorkspaceSqlExecutor {

        private final SqlScriptRunner runner = new SqlScriptRunner();

        @Override
        public SqlExecutionResult executeSingle(ConnectionConfig config, String statement) throws SQLException {
            return runner.executeSingle(config, statement);
        }
    }

    public static final class Session {

        private final Map<String, ConnectionConfig> connections = new LinkedHashMap<>();
        private final ConnectionRegistry registry;
        private final EditorStateStore editorStore;
        private final WorkspaceSqlExecutor executor;
        private final QueryLibraryStore queryLibraryStore;
        private String activeConnectionName;
        private String buffer = "";
        private List<String> history = Collections.emptyList();
        private SqlExecutionResult lastExecutionResult;
        private String lastResult;
        private String lastError;
        private String statusMessage = "Ready";

        public Session() {
            this.registry = null;
            this.editorStore = null;
            this.executor = new DefaultWorkspaceSqlExecutor();
            this.queryLibraryStore = new QueryLibraryStore();
        }

        public Session(ConnectionRegistry registry, EditorStateStore editorStore, WorkspaceSqlExecutor executor) throws IOException {
            this(registry, editorStore, executor, new QueryLibraryStore());
        }

        public Session(
            ConnectionRegistry registry,
            EditorStateStore editorStore,
            WorkspaceSqlExecutor executor,
            QueryLibraryStore queryLibraryStore
        ) throws IOException {
            this.registry = registry;
            this.editorStore = editorStore;
            this.executor = executor == null ? new DefaultWorkspaceSqlExecutor() : executor;
            this.queryLibraryStore = queryLibraryStore == null ? new QueryLibraryStore() : queryLibraryStore;
            EditorStateStore.EditorState state = editorStore.load();
            this.buffer = state.buffer();
            this.history = state.history();
            selectFirstSavedConnection();
        }

        public void addConnection(String name, ConnectionConfig config) {
            connections.put(name, config);
            if (activeConnectionName == null) {
                activeConnectionName = name;
            }
        }

        public List<String> connectionNames() {
            return new ArrayList<>(connections.keySet());
        }

        public void useConnection(String name) {
            if (!connections.containsKey(name) && registry != null) {
                try {
                    connections.put(name, registry.load(name));
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Could not load connection: " + name, ex);
                }
            }
            if (!connections.containsKey(name)) {
                throw new IllegalArgumentException("Unknown connection: " + name);
            }
            activeConnectionName = name;
        }

        public String useConnectionCommand(WorkspaceCommand command) {
            if (command.arguments().isEmpty()) {
                return fail("use requires <name>");
            }
            try {
                useConnection(command.arguments().get(0));
                return ok("Active connection: " + activeConnectionName);
            } catch (RuntimeException ex) {
                return fail(ex.getMessage());
            }
        }

        public String activeConnectionName() {
            return activeConnectionName;
        }

        public void replaceBuffer(String value) {
            buffer = value == null ? "" : value;
            try {
                saveBuffer();
            } catch (IOException ignored) {
                // Loading support workflows must still update the active TUI buffer when persistence is unavailable.
            }
        }

        public ConnectionConfig activeConnection() {
            return activeConnectionName == null ? null : connections.get(activeConnectionName);
        }

        public ConnectionConfig createConnection(
            String name,
            DatabaseType databaseType,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) {
            return createConnection(
                name,
                databaseType,
                ConnectionEnvironment.DEV,
                jdbcUrl,
                username,
                password,
                selectedSchemas,
                availableSchemas
            );
        }

        public ConnectionConfig createConnection(
            String name,
            DatabaseType databaseType,
            ConnectionEnvironment environment,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) {
            List<String> schemas = selectedSchemas == null ? Collections.<String>emptyList() : selectedSchemas;
            if (
                databaseType == DatabaseType.POSTGRESQL &&
                schemas.isEmpty() &&
                availableSchemas != null &&
                availableSchemas.contains("public")
            ) {
                schemas = Arrays.asList("public");
            }
            ConnectionConfig config = new ConnectionConfig(databaseType, environment, jdbcUrl, username, password, schemas);
            addConnection(name, config);
            activeConnectionName = name;
            return config;
        }

        public List<ConnectionListComponent.ConnectionItem> connectionItems() {
            List<ConnectionListComponent.ConnectionItem> items = new ArrayList<>();
            try {
                if (registry == null) {
                    for (Map.Entry<String, ConnectionConfig> entry : connections.entrySet()) {
                        items.add(new ConnectionListComponent.ConnectionItem(entry.getKey(), entry.getValue()));
                    }
                    return items;
                }
                for (ConnectionRegistry.ConnectionSummary summary : registry.list()) {
                    ConnectionConfig config = connections.containsKey(summary.name())
                        ? connections.get(summary.name())
                        : registry.load(summary.name());
                    connections.put(summary.name(), config);
                    items.add(new ConnectionListComponent.ConnectionItem(summary.name(), config));
                }
                return items;
            } catch (IOException ex) {
                return items;
            }
        }

        private void selectFirstSavedConnection() throws IOException {
            if (registry == null || activeConnectionName != null) {
                return;
            }
            List<ConnectionRegistry.ConnectionSummary> summaries = registry.list();
            if (summaries.isEmpty()) {
                return;
            }
            String name = summaries.get(0).name();
            connections.put(name, registry.load(name));
            activeConnectionName = name;
        }

        public void selectConnectionAt(int index) {
            List<ConnectionListComponent.ConnectionItem> items = connectionItems();
            if (items.isEmpty()) {
                fail("No saved connections");
                return;
            }
            int clamped = Math.max(0, Math.min(index, items.size() - 1));
            useConnection(items.get(clamped).name());
            ok("Active connection: " + activeConnectionName);
        }

        public ConnectionConfig createConnectionFromAction(
            String name,
            DatabaseType databaseType,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) throws IOException {
            return createConnectionFromAction(
                name,
                databaseType,
                ConnectionEnvironment.DEV,
                jdbcUrl,
                username,
                password,
                selectedSchemas,
                availableSchemas
            );
        }

        public ConnectionConfig createConnectionFromAction(
            String name,
            DatabaseType databaseType,
            ConnectionEnvironment environment,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) throws IOException {
            ConnectionConfig config = createConnection(
                name,
                databaseType,
                environment,
                jdbcUrl,
                username,
                password,
                selectedSchemas,
                availableSchemas
            );
            if (registry != null) {
                registry.save(name, config);
            }
            ok("Connection saved: " + name);
            return config;
        }

        public String runCurrentBuffer() {
            return runCurrentBuffer(Collections.<String>emptyList());
        }

        public String runCurrentBuffer(List<String> arguments) {
            return runCurrentBuffer(buffer, buffer == null ? 0 : buffer.length(), arguments);
        }

        public String runCurrentBuffer(String editorText, int cursorOffset) {
            return runCurrentBuffer(editorText, cursorOffset, Collections.<String>emptyList());
        }

        private String runCurrentBuffer(String editorText, int cursorOffset, List<String> arguments) {
            buffer = editorText == null ? "" : editorText;
            try {
                saveBuffer();
            } catch (IOException ignored) {
                // Buffer persistence must not prevent raw TUI execution from using the current editor text.
            }
            if (buffer == null || buffer.trim().isEmpty()) {
                return fail("Nothing is ready to execute");
            }
            if (activeConnection() == null) {
                return fail("No active connection selected");
            }
            String statement = SqlStatementSelector.currentStatement(buffer, cursorOffset);
            WorkspaceCommand editorCommand = WorkspaceCommand.parse(statement);
            if (isEditorMetadata(editorCommand.type())) {
                return metadataCommand(editorCommand);
            }
            try {
                SafetyGuard.requireSafe(
                    statement,
                    activeConnection().environment(),
                    activeConnectionName,
                    hasFlag(arguments, "--force"),
                    hasFlag(arguments, "--unsafe"),
                    valueAfter(arguments, "--confirm-risk")
                );
                SqlExecutionResult result = executor.executeSingle(activeConnection(), statement);
                if (editorStore != null) {
                    editorStore.recordHistory(statement, false);
                    history = editorStore.load().history();
                }
                setLastResult(result);
                lastError = null;
                statusMessage = "Query completed";
                return lastResult;
            } catch (Exception ex) {
                if (editorStore != null) {
                    try {
                        editorStore.recordHistory(statement, true);
                        history = editorStore.load().history();
                    } catch (IOException ignored) {
                        // History persistence must not hide the execution error.
                    }
                }
                return fail(ex.getMessage());
            }
        }

        public String renderConnections() {
            try {
                List<ConnectionRegistry.ConnectionSummary> summaries = registry == null ? inMemorySummaries() : registry.list();
                if (summaries.isEmpty()) {
                    return "No saved connections" + System.lineSeparator();
                }
                StringBuilder rendered = new StringBuilder();
                for (ConnectionRegistry.ConnectionSummary summary : summaries) {
                    rendered.append(summary).append(System.lineSeparator());
                }
                return rendered.toString();
            } catch (IOException ex) {
                return fail("Could not list connections: " + ex.getMessage()) + System.lineSeparator();
            }
        }

        public String createConnectionCommand(WorkspaceCommand command) {
            List<String> values = command.arguments();
            if (values.size() < 5) {
                return fail("new connection requires <oracle|postgresql> <name> <jdbcUrl> <username> <password> [schemas]");
            }
            try {
                DatabaseType databaseType = DatabaseType.fromStoredValue(values.get(0));
                int nextIndex = 5;
                ConnectionEnvironment environment = ConnectionEnvironment.DEV;
                if (values.size() > nextIndex && isEnvironment(values.get(nextIndex))) {
                    environment = ConnectionEnvironment.fromInputValue(values.get(nextIndex));
                    nextIndex++;
                }
                List<String> selectedSchemas = values.size() > nextIndex
                    ? ConfigStore.parseSchemas(values.get(nextIndex))
                    : Collections.<String>emptyList();
                List<String> availableSchemas = databaseType == DatabaseType.POSTGRESQL
                    ? Arrays.asList("public")
                    : Collections.<String>emptyList();
                ConnectionConfig config = createConnection(
                    values.get(1),
                    databaseType,
                    environment,
                    values.get(2),
                    values.get(3),
                    values.get(4),
                    selectedSchemas,
                    availableSchemas
                );
                if (registry != null) {
                    registry.save(values.get(1), config);
                }
                return ok("Connection saved: " + values.get(1));
            } catch (Exception ex) {
                return fail(ex.getMessage());
            }
        }

        public String bufferCommand(WorkspaceCommand command) {
            if (command.arguments().isEmpty()) {
                return "Buffer: " + bufferPreview();
            }
            String action = command.arguments().get(0);
            try {
                if ("set".equals(action)) {
                    buffer = command.argumentTextAfter(1);
                    saveBuffer();
                    return ok("Buffer updated");
                }
                if ("append".equals(action)) {
                    String addition = command.argumentTextAfter(1);
                    buffer = buffer == null || buffer.isEmpty() ? addition : buffer + System.lineSeparator() + addition;
                    saveBuffer();
                    return ok("Buffer updated");
                }
                if ("show".equals(action)) {
                    return buffer == null || buffer.isEmpty() ? "Buffer is empty" : buffer;
                }
                if ("clear".equals(action)) {
                    buffer = "";
                    saveBuffer();
                    return ok("Buffer cleared");
                }
                return fail("buffer supports set, append, show, clear");
            } catch (IOException ex) {
                return fail(ex.getMessage());
            }
        }

        public String libraryCommand(WorkspaceCommand command) {
            try {
                if (command.type() == WorkspaceCommand.Type.LIB_SAVE) {
                    return saveLibraryQuery(command);
                }
                if (command.type() == WorkspaceCommand.Type.LIB_LIST) {
                    return renderLibraryEntries(queryLibraryStore.list());
                }
                if (command.type() == WorkspaceCommand.Type.LIB_SEARCH) {
                    return renderLibraryEntries(queryLibraryStore.search(command.argumentText()));
                }
                if (command.type() == WorkspaceCommand.Type.LIB_LOAD) {
                    return loadLibraryQuery(command);
                }
                if (command.type() == WorkspaceCommand.Type.LIB_DELETE) {
                    return deleteLibraryQuery(command);
                }
                if (command.type() == WorkspaceCommand.Type.LIB_FAVORITE || command.type() == WorkspaceCommand.Type.LIB_UNFAVORITE) {
                    return favoriteLibraryQuery(command);
                }
                return fail("library supports save, list, search, load, delete, favorite, unfavorite");
            } catch (Exception ex) {
                return fail(ex.getMessage());
            }
        }

        public QueryLibraryEntry saveQueryLibraryEntry(
            String name,
            String sql,
            String description,
            List<String> tags,
            boolean favorite,
            boolean overwrite
        ) throws IOException {
            return queryLibraryStore.save(
                name,
                sql,
                description,
                tags,
                favorite,
                activeConnection() == null ? "" : activeConnection().environment().name(),
                activeConnectionName == null ? "" : activeConnectionName,
                overwrite
            );
        }

        public List<QueryLibraryEntry> listQueryLibraryEntries() throws IOException {
            return queryLibraryStore.list();
        }

        public List<QueryLibraryEntry> searchQueryLibraryEntries(String text) throws IOException {
            return queryLibraryStore.search(text);
        }

        public QueryLibraryEntry loadQueryLibraryEntry(String id) throws IOException {
            return queryLibraryStore.load(id);
        }

        public boolean deleteQueryLibraryEntry(String id) throws IOException {
            return queryLibraryStore.delete(id);
        }

        public QueryLibraryEntry setQueryLibraryFavorite(String id, boolean favorite) throws IOException {
            return queryLibraryStore.setFavorite(id, favorite);
        }

        public String renderHistory() {
            if (history == null || history.isEmpty()) {
                return "History is empty" + System.lineSeparator();
            }
            StringBuilder rendered = new StringBuilder();
            for (String entry : history) {
                rendered.append(entry).append(System.lineSeparator());
            }
            return rendered.toString();
        }

        public String metadataCommand(WorkspaceCommand command) {
            if (activeConnection() == null) {
                return fail("No active connection selected");
            }
            try {
                String statement = metadataStatement(command);
                SqlExecutionResult result = executor.executeSingle(activeConnection(), statement);
                setLastResult(result);
                lastError = null;
                statusMessage = "Metadata loaded";
                return lastResult;
            } catch (Exception ex) {
                return fail(ex.getMessage());
            }
        }

        public String runMetadataAction(String action) {
            return metadataCommand(WorkspaceCommand.parse(action));
        }

        public String lastError() {
            return lastError;
        }

        public SqlExecutionResult lastExecutionResult() {
            return lastExecutionResult;
        }

        public WorkspaceDashboardRenderer.DashboardState dashboardState() {
            ConnectionConfig active = activeConnection();
            List<String> schemas = active == null ? Collections.<String>emptyList() : active.schemas();
            return new WorkspaceDashboardRenderer.DashboardState(
                activeConnectionName,
                schemas,
                buffer,
                history,
                lastResult,
                lastError,
                dashboardConnections(),
                statusMessage
            );
        }

        public boolean nextResultPage() {
            if (lastExecutionResult == null || !lastExecutionResult.hasNextPage()) {
                return false;
            }
            setLastResult(lastExecutionResult.nextPage());
            statusMessage = "Result page " + (lastExecutionResult.pageIndex() + 1) + " of " + lastExecutionResult.pageCount();
            return true;
        }

        public boolean previousResultPage() {
            if (lastExecutionResult == null || !lastExecutionResult.hasPreviousPage()) {
                return false;
            }
            setLastResult(lastExecutionResult.previousPage());
            statusMessage = "Result page " + (lastExecutionResult.pageIndex() + 1) + " of " + lastExecutionResult.pageCount();
            return true;
        }

        private void setLastResult(SqlExecutionResult result) {
            lastExecutionResult = result == null ? new SqlExecutionResult("") : result;
            lastResult = lastExecutionResult.consoleTable();
        }

        private List<WorkspaceDashboardRenderer.ConnectionSummary> dashboardConnections() {
            List<ConnectionRegistry.ConnectionSummary> summaries;
            try {
                summaries = registry == null ? inMemorySummaries() : registry.list();
            } catch (IOException ex) {
                return Collections.singletonList(
                    new WorkspaceDashboardRenderer.ConnectionSummary("connections unavailable", "unknown", false)
                );
            }
            List<WorkspaceDashboardRenderer.ConnectionSummary> dashboardConnections = new ArrayList<>();
            for (ConnectionRegistry.ConnectionSummary summary : summaries) {
                dashboardConnections.add(
                    new WorkspaceDashboardRenderer.ConnectionSummary(
                        summary.name(),
                        summary.databaseType().name(),
                        summary.environment().name(),
                        summary.name().equals(activeConnectionName)
                    )
                );
            }
            return dashboardConnections;
        }

        private void saveBuffer() throws IOException {
            if (editorStore != null) {
                editorStore.saveBuffer(buffer);
                history = editorStore.load().history();
            }
        }

        private String saveLibraryQuery(WorkspaceCommand command) throws IOException {
            if (buffer == null || buffer.trim().isEmpty()) {
                return fail("Cannot save an empty buffer");
            }
            ParsedLibrarySave parsed = ParsedLibrarySave.from(command.arguments());
            QueryLibraryEntry entry = queryLibraryStore.save(
                parsed.name,
                buffer,
                parsed.description,
                parsed.tags,
                parsed.favorite,
                activeConnection() == null ? "" : activeConnection().environment().name(),
                activeConnectionName == null ? "" : activeConnectionName,
                parsed.overwrite
            );
            return ok("Saved query: " + entry.id() + System.lineSeparator() + QueryLibraryStore.PRIVACY_WARNING);
        }

        private String loadLibraryQuery(WorkspaceCommand command) throws IOException {
            String id = requiredFirstArgument(command, "lib load requires <id> [--replace]");
            QueryLibraryEntry entry = queryLibraryStore.load(id);
            if (buffer != null && !buffer.trim().isEmpty() && !buffer.equals(entry.sql()) && !hasFlag(command.arguments(), "--replace")) {
                return fail("Buffer has content. Re-run with --replace to load query: " + entry.id());
            }
            replaceBuffer(entry.sql());
            return ok("Loaded query into buffer: " + entry.id());
        }

        private String deleteLibraryQuery(WorkspaceCommand command) throws IOException {
            String id = requiredFirstArgument(command, "lib delete requires <id> --yes");
            String slug = QueryLibraryStore.slug(id);
            if (!hasFlag(command.arguments(), "--yes")) {
                return fail("Use --yes to delete query: " + slug);
            }
            return queryLibraryStore.delete(id) ? ok("Deleted query: " + slug) : fail("Query not found: " + slug);
        }

        private String favoriteLibraryQuery(WorkspaceCommand command) throws IOException {
            String id = requiredFirstArgument(command, "lib favorite requires <id>");
            boolean favorite = command.type() == WorkspaceCommand.Type.LIB_FAVORITE;
            QueryLibraryEntry entry = queryLibraryStore.setFavorite(id, favorite);
            return ok("Updated favorite: " + entry.id() + " = " + favorite);
        }

        private String renderLibraryEntries(List<QueryLibraryEntry> entries) {
            if (entries.isEmpty()) {
                return "No saved queries";
            }
            StringBuilder rendered = new StringBuilder();
            for (QueryLibraryEntry entry : entries) {
                if (rendered.length() > 0) {
                    rendered.append(System.lineSeparator());
                }
                rendered.append(entry.favorite() ? "* " : "  ");
                rendered.append(entry.id()).append(" | ").append(entry.name()).append(" | ");
                rendered.append(joinDisplay(entry.tags())).append(" | ").append(entry.description());
            }
            return rendered.toString();
        }

        private String requiredFirstArgument(WorkspaceCommand command, String message) {
            if (command.arguments().isEmpty() || command.arguments().get(0).startsWith("--")) {
                throw new IllegalArgumentException(message);
            }
            return command.arguments().get(0);
        }

        private static String joinDisplay(List<String> values) {
            StringBuilder joined = new StringBuilder();
            for (String value : values) {
                if (joined.length() > 0) {
                    joined.append(", ");
                }
                joined.append(value);
            }
            return joined.toString();
        }

        private static List<String> parseTags(String value) {
            if (value == null || value.trim().isEmpty()) {
                return Collections.emptyList();
            }
            List<String> tags = new ArrayList<>();
            for (String tag : value.split(",")) {
                if (!tag.trim().isEmpty()) {
                    tags.add(tag.trim());
                }
            }
            return tags;
        }

        private static final class ParsedLibrarySave {

            private final String name;
            private final String description;
            private final List<String> tags;
            private final boolean favorite;
            private final boolean overwrite;

            private ParsedLibrarySave(String name, String description, List<String> tags, boolean favorite, boolean overwrite) {
                this.name = name;
                this.description = description;
                this.tags = tags;
                this.favorite = favorite;
                this.overwrite = overwrite;
            }

            private static ParsedLibrarySave from(List<String> arguments) {
                List<String> nameParts = new ArrayList<>();
                String description = "";
                List<String> tags = Collections.emptyList();
                boolean favorite = false;
                boolean overwrite = false;
                for (int i = 0; i < arguments.size(); i++) {
                    String argument = arguments.get(i);
                    if ("--desc".equals(argument) || "--description".equals(argument)) {
                        if (i + 1 >= arguments.size()) {
                            throw new IllegalArgumentException("--desc requires text");
                        }
                        List<String> descriptionParts = new ArrayList<>();
                        while (i + 1 < arguments.size() && !arguments.get(i + 1).startsWith("--")) {
                            descriptionParts.add(arguments.get(++i));
                        }
                        description = joinDisplayWithSpace(descriptionParts);
                    } else if ("--tags".equals(argument)) {
                        if (i + 1 >= arguments.size()) {
                            throw new IllegalArgumentException("--tags requires comma-separated tags");
                        }
                        tags = parseTags(arguments.get(++i));
                    } else if ("--favorite".equals(argument)) {
                        favorite = true;
                    } else if ("--overwrite".equals(argument)) {
                        overwrite = true;
                    } else if (argument.startsWith("--")) {
                        throw new IllegalArgumentException("Unsupported lib save option: " + argument);
                    } else {
                        nameParts.add(argument);
                    }
                }
                String name = joinDisplayWithSpace(nameParts);
                if (name.trim().isEmpty()) {
                    throw new IllegalArgumentException("lib save requires <name>");
                }
                return new ParsedLibrarySave(name, description, tags, favorite, overwrite);
            }

            private static String joinDisplayWithSpace(List<String> values) {
                StringBuilder joined = new StringBuilder();
                for (String value : values) {
                    if (joined.length() > 0) {
                        joined.append(' ');
                    }
                    joined.append(value);
                }
                return joined.toString();
            }
        }

        private List<ConnectionRegistry.ConnectionSummary> inMemorySummaries() {
            List<ConnectionRegistry.ConnectionSummary> summaries = new ArrayList<>();
            for (Map.Entry<String, ConnectionConfig> entry : connections.entrySet()) {
                ConnectionConfig config = entry.getValue();
                summaries.add(
                    new ConnectionRegistry.ConnectionSummary(
                        entry.getKey(),
                        config.databaseType(),
                        config.environment(),
                        config.jdbcUrl(),
                        config.username(),
                        config.schemas()
                    )
                );
            }
            Collections.sort(summaries);
            return summaries;
        }

        private String metadataStatement(WorkspaceCommand command) {
            if (command.type() == WorkspaceCommand.Type.EXPLAIN) {
                return MetadataProviderFactory.forConfig(activeConnection()).explain(command.argumentText());
            }
            return OracleScriptCli.metadataSql(activeConnection(), metadataCommandName(command.type()), command.arguments());
        }

        private String bufferPreview() {
            return buffer == null || buffer.isEmpty() ? "empty" : buffer;
        }

        private String ok(String message) {
            lastError = null;
            statusMessage = message;
            return message;
        }

        private String fail(String message) {
            lastError = message;
            statusMessage = message;
            return message;
        }

        private static boolean hasFlag(List<String> values, String flag) {
            return values.contains(flag);
        }

        private static boolean isEnvironment(String value) {
            try {
                ConnectionEnvironment.fromInputValue(value);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        private static boolean isEditorMetadata(WorkspaceCommand.Type type) {
            return type == WorkspaceCommand.Type.TABLES || type == WorkspaceCommand.Type.DESC || type == WorkspaceCommand.Type.INDEXES;
        }

        private static String valueAfter(List<String> values, String flag) {
            int index = values.indexOf(flag);
            return index >= 0 && index + 1 < values.size() ? values.get(index + 1) : null;
        }

        private static String metadataCommandName(WorkspaceCommand.Type type) {
            if (type == WorkspaceCommand.Type.TABLES) return "tables";
            if (type == WorkspaceCommand.Type.SEARCH) return "search";
            if (type == WorkspaceCommand.Type.DESC) return "desc";
            if (type == WorkspaceCommand.Type.DETAILS) return "details";
            if (type == WorkspaceCommand.Type.INDEXES) return "indexes";
            if (type == WorkspaceCommand.Type.CONSTRAINTS) return "constraints";
            if (type == WorkspaceCommand.Type.FK_IN) return "fk-in";
            if (type == WorkspaceCommand.Type.FK_OUT) return "fk-out";
            if (type == WorkspaceCommand.Type.COUNT) return "count";
            if (type == WorkspaceCommand.Type.SAMPLE) return "sample";
            throw new IllegalArgumentException("Unsupported metadata command: " + type);
        }
    }
}
