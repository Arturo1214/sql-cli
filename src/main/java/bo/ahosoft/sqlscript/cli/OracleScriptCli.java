package bo.ahosoft.sqlscript.cli;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class OracleScriptCli {

    private static final int DEFAULT_SAMPLE_LIMIT = 20;

    private OracleScriptCli() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (SQLException ex) {
            System.err.print(SqlConsoleRenderer.formatFailure(ex));
            System.exit(2);
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(2);
        }
    }

    public static void run(String[] args) throws Exception {
        run(args, defaultWorkspaceRunner());
    }

    public static void run(String[] args, WorkspaceLauncher.WorkspaceRunner interactiveWorkspace) throws Exception {
        if (args.length == 0) {
            runInteractiveWorkspace(interactiveWorkspace);
            return;
        }

        if (isHelp(args[0])) {
            printUsage();
            return;
        }

        if (args.length >= 4 && !isCommand(args[0])) {
            executeLegacy(args);
            return;
        }

        ParsedArgs parsed = ParsedArgs.parse(args);
        String command = parsed.command();

        if ("profiles".equals(command)) {
            listProfiles();
            return;
        }
        if ("history".equals(command)) {
            printHistory();
            return;
        }
        if ("connections".equals(command)) {
            connections(parsed);
            return;
        }
        if ("init".equals(command)) {
            init(parsed);
            return;
        }
        if ("validate".equals(command)) {
            validate(parsed);
            return;
        }
        if ("workspace".equals(command) && args.length == 1) {
            runInteractiveWorkspace(interactiveWorkspace);
            return;
        }
        if ("workspace".equals(command)) {
            throw new IllegalArgumentException(
                "Use run-current for one-shot workspace execution, for example: run-current --buffer '<sql>' --cursor <offset> --dry-run"
            );
        }
        if ("run-current".equals(command)) {
            workspace(parsed);
            return;
        }

        ConnectionConfig config = ConfigStore.load(parsed.configFile());
        SqlScriptRunner runner = new SqlScriptRunner();
        appendHistory(parsed);

        if ("exec".equals(command)) {
            requireSize(parsed.values(), 1, "exec requiere <script|@archivo.sql>");
            String script = readScript(parsed.values().get(0));
            ensureSafe(script, parsed.force(), parsed.confirmRisk());
            if (parsed.csvFile() != null) {
                runner.exportCsv(config, script, parsed.csvFile());
                System.out.println("CSV generado: " + parsed.csvFile().getAbsolutePath());
            } else {
                print(runner.execute(config, script));
            }
        } else if ("export".equals(command)) {
            requireSize(parsed.values(), 2, "export requiere <script|@archivo.sql> <salida.csv>");
            String script = readScript(parsed.values().get(0));
            runner.exportCsv(config, script, new File(parsed.values().get(1)));
            System.out.println("CSV generado: " + new File(parsed.values().get(1)).getAbsolutePath());
        } else if ("tables".equals(command)) {
            String filter = parsed.values().isEmpty() ? null : parsed.values().get(0);
            print(
                runner.execute(
                    config,
                    metadataSql(config, command, filter == null ? Collections.<String>emptyList() : Collections.singletonList(filter))
                )
            );
        } else if ("search".equals(command)) {
            requireSize(parsed.values(), 1, "search requiere <texto>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("sample".equals(command)) {
            requireSize(parsed.values(), 1, "sample requiere <tabla> [limite]");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("desc".equals(command) || "describe".equals(command)) {
            requireSize(parsed.values(), 1, command + " requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("detail".equals(command) || "details".equals(command)) {
            requireSize(parsed.values(), 1, command + " requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("indexes".equals(command)) {
            requireSize(parsed.values(), 1, "indexes requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("constraints".equals(command)) {
            requireSize(parsed.values(), 1, "constraints requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("fk-in".equals(command)) {
            requireSize(parsed.values(), 1, "fk-in requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("fk-out".equals(command)) {
            requireSize(parsed.values(), 1, "fk-out requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else if ("explain".equals(command)) {
            requireSize(parsed.values(), 1, "explain requiere <select|@archivo.sql>");
            print(runner.execute(config, metadataProvider(config).explain(readScript(parsed.values().get(0)))));
        } else if ("count".equals(command)) {
            requireSize(parsed.values(), 1, "count requiere <tabla>");
            print(runner.execute(config, metadataSql(config, command, parsed.values())));
        } else {
            throw new IllegalArgumentException("Comando no reconocido: " + command);
        }
    }

    private static WorkspaceLauncher.WorkspaceRunner defaultWorkspaceRunner() {
        final InteractiveWorkspace.Session session = InteractiveWorkspace.defaultSession();
        final WorkspaceLauncher launcher = WorkspaceLauncher.forGui2(
            new WorkspaceLauncher.TerminalSupport() {
                public boolean isSupported() {
                    return isGui2TerminalSupported();
                }
            },
            new WorkspaceLauncher.WorkspaceRunner() {
                public int run() throws IOException {
                    return new Gui2TerminalWorkspace(session).run();
                }
            },
            new WorkspaceLauncher.WorkspaceRunner() {
                public int run() throws IOException {
                    return new InteractiveWorkspace(System.in, System.out, session).run();
                }
            }
        );
        return new WorkspaceLauncher.WorkspaceRunner() {
            public int run() throws IOException {
                return launcher.run();
            }
        };
    }

    private static boolean isGui2TerminalSupported() {
        String term = System.getenv("TERM");
        return System.console() != null && term != null && !"dumb".equalsIgnoreCase(term) && System.getenv("NO_COLOR") == null;
    }

    private static void runInteractiveWorkspace(WorkspaceLauncher.WorkspaceRunner interactiveWorkspace) throws IOException {
        int exitCode = interactiveWorkspace.run();
        if (exitCode != 0) {
            throw new IOException("Interactive workspace exited with code " + exitCode);
        }
    }

    private static void executeLegacy(String[] args) throws Exception {
        ConnectionConfig config = new ConnectionConfig(args[0], args[1], args[2]);
        String script = readScript(args[3]);
        ensureSafe(script, false, null);
        print(new SqlScriptRunner().execute(config, script));
    }

    private static void workspace(ParsedArgs parsed) throws Exception {
        String buffer = parsed.buffer();
        if (buffer == null) {
            requireSize(parsed.values(), 1, "workspace requiere --buffer <sql> o <script|@archivo.sql>");
            buffer = readScript(parsed.values().get(0));
        }
        String statement = SqlScriptRunner.currentStatement(buffer, parsed.cursorOffset(buffer));
        ensureSafe(statement, parsed.force(), parsed.confirmRisk());
        if (parsed.dryRun()) {
            System.out.println("Selected statement:");
            System.out.println(statement);
            return;
        }
        ConnectionConfig config = ConfigStore.load(parsed.configFile());
        print(new SqlScriptRunner().executeSingle(config, statement));
    }

    private static void connections(ParsedArgs parsed) throws Exception {
        if (parsed.values().isEmpty() || "list".equals(parsed.values().get(0))) {
            List<ConnectionRegistry.ConnectionSummary> summaries = defaultRegistry().list();
            if (summaries.isEmpty()) {
                System.out.println("No hay conexiones registradas.");
                return;
            }
            for (ConnectionRegistry.ConnectionSummary summary : summaries) {
                System.out.println(summary);
            }
            return;
        }
        throw new IllegalArgumentException("connections soporta: list");
    }

    private static void validate(ParsedArgs parsed) throws Exception {
        ConnectionConfig config = ConfigStore.load(parsed.configFile());
        defaultRegistry().validate(config);
        System.out.println("Configuración válida para usuario: " + config.username());
    }

    private static void init(ParsedArgs parsed) throws Exception {
        requireSize(parsed.values(), 3, "init requiere <jdbcUrl> <usuario> <password>");
        ConnectionConfig config = new ConnectionConfig(
            parsed.databaseType(),
            parsed.values().get(0),
            parsed.values().get(1),
            parsed.values().get(2),
            parsed.schemas()
        );
        defaultRegistry().validate(config);
        if (config.databaseType() == DatabaseType.POSTGRESQL && config.schemas().isEmpty()) {
            config = withDiscoveredSchemas(config);
        }
        ConfigStore.save(parsed.configFile(), config);
        System.out.println("Configuración guardada en: " + parsed.configFile().getAbsolutePath());
        System.out.println("Tipo de base de datos: " + config.databaseType());
        if (!config.schemas().isEmpty()) {
            System.out.println("Schemas: " + ConfigStore.joinSchemas(config.schemas()));
        }
        System.out.println("OJO: el password queda guardado en texto plano. Usá esto solo en un entorno controlado.");
    }

    public static String metadataSql(ConnectionConfig config, String command, List<String> values) {
        MetadataProvider provider = metadataProvider(config);
        if ("tables".equals(command)) {
            return provider.tables(values.isEmpty() ? null : values.get(0));
        }
        if ("search".equals(command)) {
            requireSize(values, 1, "search requiere <texto>");
            return provider.search(values.get(0));
        }
        if ("sample".equals(command)) {
            requireSize(values, 1, "sample requiere <tabla> [limite]");
            int limit = values.size() > 1 ? parsePositiveInt(values.get(1), "limite") : DEFAULT_SAMPLE_LIMIT;
            return provider.sample(values.get(0), limit);
        }
        if ("desc".equals(command) || "describe".equals(command)) {
            requireSize(values, 1, command + " requiere <tabla>");
            return provider.describe(values.get(0));
        }
        if ("detail".equals(command) || "details".equals(command)) {
            requireSize(values, 1, command + " requiere <tabla>");
            return provider.details(values.get(0));
        }
        if ("indexes".equals(command)) {
            requireSize(values, 1, "indexes requiere <tabla>");
            return provider.indexes(values.get(0));
        }
        if ("constraints".equals(command)) {
            requireSize(values, 1, "constraints requiere <tabla>");
            return provider.constraints(values.get(0));
        }
        if ("fk-in".equals(command)) {
            requireSize(values, 1, "fk-in requiere <tabla>");
            return provider.fkIn(values.get(0));
        }
        if ("fk-out".equals(command)) {
            requireSize(values, 1, "fk-out requiere <tabla>");
            return provider.fkOut(values.get(0));
        }
        if ("count".equals(command)) {
            requireSize(values, 1, "count requiere <tabla>");
            return provider.count(values.get(0));
        }
        throw new IllegalArgumentException("Comando de metadata no reconocido: " + command);
    }

    private static MetadataProvider metadataProvider(ConnectionConfig config) {
        return MetadataProviderFactory.forConfig(config);
    }

    private static ConnectionConfig withDiscoveredSchemas(ConnectionConfig config) throws SQLException {
        SchemaDiscoveryService discoveryService = new SchemaDiscoveryService();
        JdbcConnectionFactory connectionFactory = new JdbcConnectionFactory();
        Connection connection = connectionFactory.open(config);
        try {
            List<String> selected = discoveryService.resolveSelection(discoveryService.discover(connection), config.schemas());
            return new ConnectionConfig(config.databaseType(), config.jdbcUrl(), config.username(), config.password(), selected);
        } finally {
            connection.close();
        }
    }

    private static void listProfiles() {
        List<String> profiles = ConfigStore.profiles();
        if (profiles.isEmpty()) {
            System.out.println("No hay perfiles configurados.");
            return;
        }
        for (String profile : profiles) {
            System.out.println(profile);
        }
    }

    private static String readScript(String scriptArg) throws Exception {
        if (scriptArg.startsWith("@")) {
            byte[] bytes = Files.readAllBytes(Paths.get(scriptArg.substring(1)));
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return scriptArg;
    }

    private static void ensureSafe(String script, boolean force, String confirmRisk) {
        SafetyGuard.requireSafe(script, force, confirmRisk);
    }

    private static int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(name + " debe ser mayor a cero");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " inválido: " + value);
        }
    }

    private static int parseNonNegativeInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " debe ser cero o mayor");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " inválido: " + value);
        }
    }

    private static void print(SqlExecutionResult result) {
        System.out.print(result.consoleTable());
    }

    private static void requireSize(List<String> values, int size, String message) {
        if (values.size() < size) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isHelp(String value) {
        return CommandDispatcher.isHelp(value);
    }

    private static boolean isCommand(String value) {
        return CommandDispatcher.isCommand(value);
    }

    private static File historyFile() {
        return new File(System.getProperty("user.home"), ".oracle-script-cli.history");
    }

    private static ConnectionRegistry defaultRegistry() {
        File baseDirectory = new File(System.getProperty("user.home"), ".oracle-script-cli/connections");
        return new ConnectionRegistry(baseDirectory, new ProtectedSecretStore(new File(baseDirectory, "secrets")));
    }

    private static void appendHistory(ParsedArgs parsed) {
        try {
            FileWriter writer = new FileWriter(historyFile(), true);
            try {
                writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                writer.write(" | ");
                writer.write(parsed.command());
                for (String value : parsed.values()) {
                    writer.write(' ');
                    writer.write(value.startsWith("@") ? value : abbreviate(value));
                }
                if (parsed.profile() != null) {
                    writer.write(" --profile ");
                    writer.write(parsed.profile());
                }
                writer.write(System.lineSeparator());
            } finally {
                writer.close();
            }
        } catch (Exception ignored) {
            // History must never break the actual database command.
        }
    }

    private static void printHistory() throws Exception {
        File file = historyFile();
        if (!file.isFile()) {
            System.out.println("No hay historial todavía.");
            return;
        }
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        int from = Math.max(0, lines.size() - 30);
        for (int i = from; i < lines.size(); i++) {
            System.out.println(lines.get(i));
        }
    }

    private static String abbreviate(String value) {
        String oneLine = value.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 117) + "...";
    }

    private static void printUsage() {
        System.err.println("Uso con configuración:");
        System.err.println("  java -jar oracle-script-cli.jar init <jdbcUrl> <usuario> <password> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar profiles");
        System.err.println("  java -jar oracle-script-cli.jar connections list");
        System.err.println("  java -jar oracle-script-cli.jar validate [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar workspace");
        System.err.println(
            "  java -jar oracle-script-cli.jar run-current <script|@archivo.sql> [--cursor offset] [--dry-run] [--force --confirm-risk YES] [--profile nombre|--config archivo]"
        );
        System.err.println(
            "  java -jar oracle-script-cli.jar exec <script|@archivo.sql> [--force] [--csv salida.csv] [--profile nombre|--config archivo]"
        );
        System.err.println(
            "  java -jar oracle-script-cli.jar export <script|@archivo.sql> <salida.csv> [--profile nombre|--config archivo]"
        );
        System.err.println("  java -jar oracle-script-cli.jar tables [filtro] [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar search <texto> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar sample <tabla> [limite] [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar desc <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar detail <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar indexes <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar constraints <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar fk-in <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar fk-out <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar explain <select|@archivo.sql> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar count <tabla> [--profile nombre|--config archivo]");
        System.err.println("  java -jar oracle-script-cli.jar history");
        System.err.println();
        System.err.println("Workspace shortcuts:");
        System.err.println("  GUI2 split-pane workspace with explorer, SQL editor, results/logs, and status/help");
        System.err.println("  Ctrl+R runs the visible SQL buffer/current SQL");
        System.err.println("  Enter activates the selected connection or explorer action");
        System.err.println("  New Oracle/New PostgreSQL opens the connection form");
        System.err.println();
        System.err.println("Uso directo, sin configuración:");
        System.err.println("  java -jar oracle-script-cli.jar <jdbcUrl> <usuario> <password> <script|@archivo.sql>");
        System.err.println();
        System.err.println("Config default: " + ConfigStore.defaultConfigFile().getAbsolutePath());
    }

    private static final class ParsedArgs {

        private final String command;
        private final List<String> values;
        private final File configFile;
        private final boolean force;
        private final File csvFile;
        private final String profile;
        private final String confirmRisk;
        private final String buffer;
        private final Integer cursor;
        private final boolean dryRun;
        private final DatabaseType databaseType;
        private final List<String> schemas;

        private ParsedArgs(
            String command,
            List<String> values,
            File configFile,
            boolean force,
            File csvFile,
            String profile,
            String confirmRisk,
            String buffer,
            Integer cursor,
            boolean dryRun,
            DatabaseType databaseType,
            List<String> schemas
        ) {
            this.command = command;
            this.values = values;
            this.configFile = configFile;
            this.force = force;
            this.csvFile = csvFile;
            this.profile = profile;
            this.confirmRisk = confirmRisk;
            this.buffer = buffer;
            this.cursor = cursor;
            this.dryRun = dryRun;
            this.databaseType = databaseType;
            this.schemas = schemas;
        }

        public static ParsedArgs parse(String[] args) {
            String command = args[0];
            List<String> values = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
            File configFile = ConfigStore.defaultConfigFile();
            boolean force = false;
            File csvFile = null;
            String profile = null;
            String confirmRisk = null;
            String buffer = null;
            Integer cursor = null;
            boolean dryRun = false;
            boolean explicitConfig = false;
            DatabaseType databaseType = DatabaseType.ORACLE;
            List<String> schemas = Collections.emptyList();

            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                if ("--force".equals(value)) {
                    force = true;
                    values.remove(i--);
                } else if ("--dry-run".equals(value)) {
                    dryRun = true;
                    values.remove(i--);
                } else if ("--confirm-risk".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--confirm-risk requiere un valor");
                    }
                    confirmRisk = values.get(i + 1);
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--buffer".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--buffer requiere SQL");
                    }
                    buffer = values.get(i + 1);
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--cursor".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--cursor requiere un offset");
                    }
                    cursor = Integer.valueOf(parseNonNegativeInt(values.get(i + 1), "cursor"));
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--csv".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--csv requiere una ruta de archivo");
                    }
                    csvFile = new File(values.get(i + 1));
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--profile".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--profile requiere un nombre");
                    }
                    profile = values.get(i + 1);
                    if (!explicitConfig) {
                        configFile = ConfigStore.profileConfigFile(profile);
                    }
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--config".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--config requiere una ruta de archivo");
                    }
                    configFile = new File(values.get(i + 1));
                    explicitConfig = true;
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--type".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--type requiere oracle o postgresql");
                    }
                    databaseType = DatabaseType.fromStoredValue(values.get(i + 1));
                    values.remove(i + 1);
                    values.remove(i--);
                } else if ("--schemas".equals(value)) {
                    if (i + 1 >= values.size()) {
                        throw new IllegalArgumentException("--schemas requiere una lista separada por comas");
                    }
                    schemas = ConfigStore.parseSchemas(values.get(i + 1));
                    values.remove(i + 1);
                    values.remove(i--);
                }
            }
            return new ParsedArgs(
                command,
                values,
                configFile,
                force,
                csvFile,
                profile,
                confirmRisk,
                buffer,
                cursor,
                dryRun,
                databaseType,
                schemas
            );
        }

        public String command() {
            return command;
        }

        public List<String> values() {
            return values;
        }

        public File configFile() {
            return configFile;
        }

        public boolean force() {
            return force;
        }

        public File csvFile() {
            return csvFile;
        }

        public String profile() {
            return profile;
        }

        public String confirmRisk() {
            return confirmRisk;
        }

        public String buffer() {
            return buffer;
        }

        public int cursorOffset(String value) {
            if (cursor == null) {
                return value.length();
            }
            if (cursor < 0) {
                return 0;
            }
            return Math.min(cursor, value.length());
        }

        public boolean dryRun() {
            return dryRun;
        }

        public DatabaseType databaseType() {
            return databaseType;
        }

        public List<String> schemas() {
            return schemas;
        }
    }
}
