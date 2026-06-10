package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InteractiveWorkspaceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void listsSwitchesAndCreatesConnectionsInSessionState() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        session.addConnection(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );

        assertEquals(Arrays.asList("local", "reporting"), session.connectionNames());
        session.useConnection("reporting");

        assertEquals("reporting", session.activeConnectionName());
        assertEquals(DatabaseType.POSTGRESQL, session.activeConnection().databaseType());
    }

    @Test
    public void postgresCreationDefaultsToPublicWhenNoSchemaIsChosen() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();

        ConnectionConfig config = session.createConnection(
            "reporting",
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg",
            "secret",
            Arrays.<String>asList(),
            Arrays.asList("public", "audit")
        );

        assertEquals(Arrays.asList("public"), config.schemas());
        assertEquals("reporting", session.activeConnectionName());
    }

    @Test
    public void oracleCreationIgnoresSchemaSelection() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();

        ConnectionConfig config = session.createConnection(
            "local",
            DatabaseType.ORACLE,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "ora",
            "secret",
            Arrays.asList("ignored"),
            Arrays.asList("ignored")
        );

        assertTrue(config.schemas().isEmpty());
    }

    @Test
    public void emptyBufferRunReportsNothingReady() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();

        String message = session.runCurrentBuffer();

        assertEquals("Nothing is ready to execute", message);
        assertEquals("Nothing is ready to execute", session.lastError());
    }

    @Test
    public void helpAndExitAreHandledCleanly() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InteractiveWorkspace workspace = new InteractiveWorkspace(new ByteArrayInputStream("help\nexit\n".getBytes("UTF-8")), output);

        int exitCode = workspace.run();
        String rendered = new String(output.toByteArray(), "UTF-8");

        assertEquals(0, exitCode);
        assertTrue(rendered.contains("Interactive Workspace"));
        assertTrue(rendered.contains("Commands:"));
        assertTrue(rendered.contains("Goodbye"));
        assertFalse(rendered.contains("Exception"));
    }

    @Test
    public void replListsAndSwitchesPersistedConnections() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        registry.save(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );
        InteractiveWorkspace.Session session = session(registry, new CapturingExecutor());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(input("connections\nuse reporting\nexit\n"), output, session).run();

        String rendered = text(output);
        assertTrue(rendered.contains("local -> jdbc:oracle:thin:@localhost:1521/XEPDB1"));
        assertTrue(rendered.contains("reporting -> jdbc:postgresql://localhost:5432/app"));
        assertTrue(rendered.contains("Active connection: reporting"));
        assertEquals("reporting", session.activeConnectionName());
        assertEquals(DatabaseType.POSTGRESQL, session.activeConnection().databaseType());
    }

    @Test
    public void replCreatesConnectionsThroughRegistryWithDatabaseSpecificSchemaRules() throws Exception {
        ConnectionRegistry registry = registry();
        InteractiveWorkspace.Session session = session(registry, new CapturingExecutor());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(
            input(
                "new connection postgresql reporting jdbc:postgresql://localhost:5432/app pg secret\nnew connection oracle local jdbc:oracle:thin:@localhost:1521/XEPDB1 ora secret ignored\nexit\n"
            ),
            output,
            session
        ).run();

        ConnectionConfig postgres = registry.load("reporting");
        ConnectionConfig oracle = registry.load("local");
        String rendered = text(output);
        assertEquals(Arrays.asList("public"), postgres.schemas());
        assertEquals(DatabaseType.ORACLE, oracle.databaseType());
        assertTrue(oracle.schemas().isEmpty());
        assertTrue(rendered.contains("Connection saved: reporting"));
        assertTrue(rendered.contains("Connection saved: local"));
    }

    @Test
    public void replEditsPersistsAndRunsCurrentBufferStatement() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        EditorStateStore editorStore = editorStore();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(registry, editorStore, executor);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(
            input("use local\nbuffer set select * from users; select * from orders\nrun\nexit\n"),
            output,
            session
        ).run();

        String rendered = text(output);
        assertEquals("select * from orders", executor.statement);
        assertEquals("select * from users; select * from orders", editorStore.load().buffer());
        assertEquals(Arrays.asList("select * from orders"), editorStore.load().history());
        assertTrue(rendered.contains("RESULT: select * from orders"));
        assertTrue(rendered.contains("Last result: RESULT: select * from orders"));
    }

    @Test
    public void dashboardStateIncludesConnectionSummariesBufferResultsAndStatus() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        registry.save(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );
        EditorStateStore editorStore = editorStore();
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(registry, editorStore, executor);

        session.useConnectionCommand(WorkspaceCommand.parse("use reporting"));
        session.bufferCommand(WorkspaceCommand.parse("buffer set select * from users; select * from orders"));
        session.runCurrentBuffer();
        WorkspaceDashboardRenderer.DashboardState state = session.dashboardState();

        assertEquals("reporting", state.activeConnectionName());
        assertEquals("select * from users; select * from orders", state.buffer());
        assertEquals("RESULT: select * from orders", state.lastResult());
        assertEquals("Query completed", state.statusMessage());
        assertEquals(2, state.connections().size());
        assertEquals("local", state.connections().get(0).name());
        assertEquals("ORACLE", state.connections().get(0).databaseType());
        assertFalse(state.connections().get(0).active());
        assertEquals("reporting", state.connections().get(1).name());
        assertEquals("POSTGRESQL", state.connections().get(1).databaseType());
        assertTrue(state.connections().get(1).active());
    }

    @Test
    public void sessionWithSavedConnectionsDefaultsToFirstConnectionForRawTuiStatus() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save(
            "dev-docs",
            new ConnectionConfig(
                DatabaseType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/docs",
                "docs",
                "secret",
                Arrays.asList("public")
            )
        );
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));

        InteractiveWorkspace.Session session = session(registry, new CapturingExecutor());
        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            WorkspaceScreenState.initial(),
            new ConnectionListComponent(session.connectionItems()),
            new BasicSqlEditorComponent("", 0),
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            120,
            35,
            session.activeConnectionName()
        );

        assertEquals("dev-docs", session.activeConnectionName());
        assertEquals("dev-docs", session.dashboardState().activeConnectionName());
        assertTrue(rendered.connectionLines().contains("* [DEV] dev-docs [POSTGRESQL] schema=public"));
        assertTrue(rendered.statusLine().contains("Active: dev-docs [DEV] [POSTGRESQL]"));
    }

    @Test
    public void replRedrawShowsActiveConnectionPanelBufferResultPaneAndStatus() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(registry, executor);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(
            input("use local\nbuffer set select * from users; select * from orders\nrun\nexit\n"),
            output,
            session
        ).run();

        String rendered = text(output);
        assertTrue(rendered.contains("* [DEV] local [ORACLE]"));
        assertTrue(rendered.contains("SQL Buffer:"));
        assertTrue(rendered.contains("select * from users; select * from orders"));
        assertTrue(rendered.contains("Results:"));
        assertTrue(rendered.contains("RESULT: select * from orders"));
        assertTrue(rendered.contains("Status: Query completed"));
    }

    @Test
    public void replAppliesSafetyGuardBeforeExecutingDestructiveBuffer() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(registry, executor);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(
            input("use local\nbuffer set delete from users where id = 1\nrun\nrun --force --confirm-risk YES\nexit\n"),
            output,
            session
        ).run();

        String rendered = text(output);
        assertEquals("delete from users where id = 1", executor.statement);
        assertTrue(rendered.contains("Safety mode blocked a dangerous SQL statement"));
        assertTrue(rendered.contains("RESULT: delete from users where id = 1"));
    }

    @Test
    public void rawTuiBlocksMutationWithoutTopLevelWhereBeforeConfirmation() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(registry, executor);
        session.useConnection("local");

        String blocked = session.runCurrentBufferWithUnsafeConfirmation("update users set active = 0", 0, "YES");

        assertEquals(null, executor.statement);
        assertTrue(blocked.contains(SafetyGuard.MISSING_WHERE_MESSAGE));
        assertEquals(SafetyGuard.MISSING_WHERE_MESSAGE + " (statement 1)", session.dashboardState().statusMessage());
    }

    @Test
    public void tuiBlocksDangerousSqlWithLocalizedMessageButAllowsMetadataCommands() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(registry, executor);
        session.useConnection("local");

        String blocked = session.runCurrentBuffer("delete from users", 0);
        String metadata = session.runCurrentBuffer("tables", 0);

        assertTrue(blocked.contains(SafetyGuard.MISSING_WHERE_MESSAGE));
        assertTrue(new TuiMessages(TuiLanguage.SPANISH).localizeResultText(blocked).contains("WHERE de nivel superior"));
        assertTrue(metadata.contains("RESULT:"));
        assertTrue(executor.statement.toLowerCase().contains("select"));
    }

    @Test
    public void rawTuiRunsCurrentStatementAtCursorAndPreservesHistoryResultAndStatus() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        EditorStateStore editorStore = editorStore();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(registry, editorStore, executor);
        session.useConnection("local");

        String output = session.runCurrentBuffer("select * from users; select * from orders", 8);

        assertEquals("select * from users", executor.statement);
        assertEquals("RESULT: select * from users", output);
        assertEquals("select * from users; select * from orders", editorStore.load().buffer());
        assertEquals(Arrays.asList("select * from users"), editorStore.load().history());
        WorkspaceDashboardRenderer.DashboardState state = session.dashboardState();
        assertEquals("RESULT: select * from users", state.lastResult());
        assertEquals("Query completed", state.statusMessage());
        assertEquals("select * from users; select * from orders", state.buffer());
    }

    @Test
    public void rawTuiRunsEditorMetadataCommandsThroughCurrentBufferExecution() throws Exception {
        ConnectionRegistry registry = registry();
        registry.save("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        CapturingExecutor executor = new CapturingExecutor();
        EditorStateStore editorStore = editorStore();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(registry, editorStore, executor);
        session.useConnection("local");

        String output = session.runCurrentBuffer("tables user", 0);

        assertTrue(executor.statement.contains("user_tables"));
        assertTrue(executor.statement.contains("upper('%user%')"));
        assertEquals("RESULT: " + executor.statement, output);
        assertEquals("tables user", editorStore.load().buffer());
        assertEquals("Metadata loaded", session.dashboardState().statusMessage());
    }

    @Test
    public void rawTuiRejectsEmptyEditorTextWithoutLosingPreviousResult() throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));

        String message = session.runCurrentBuffer("   ", 0);

        assertEquals("Nothing is ready to execute", message);
        assertEquals("Nothing is ready to execute", session.lastError());
        assertEquals("   ", session.dashboardState().buffer());
        assertEquals("Nothing is ready to execute", session.dashboardState().statusMessage());
    }

    @Test
    public void rawTuiSelectsConnectionsByPanelIndex() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        session.addConnection(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );

        List<ConnectionListComponent.ConnectionItem> items = session.connectionItems();
        session.selectConnectionAt(1);

        assertEquals(2, items.size());
        assertEquals("reporting", items.get(1).name());
        assertEquals("reporting", session.activeConnectionName());
        assertEquals("Active connection: reporting", session.dashboardState().statusMessage());
    }

    @Test
    public void rawTuiCreateConnectionActionUsesSessionRegistryRules() throws Exception {
        ConnectionRegistry registry = registry();
        InteractiveWorkspace.Session session = session(registry, new CapturingExecutor());

        ConnectionConfig config = session.createConnectionFromAction(
            "reporting",
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "pg",
            "secret",
            Collections.<String>emptyList(),
            Arrays.asList("public", "audit")
        );

        assertEquals(Arrays.asList("public"), config.schemas());
        assertEquals("reporting", session.activeConnectionName());
        assertEquals(Arrays.asList("public"), registry.load("reporting").schemas());
        assertEquals("Connection saved: reporting", session.dashboardState().statusMessage());
    }

    @Test
    public void queryLibraryCommandsSaveListSearchFavoriteAndDeleteCurrentBuffer() throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            editorStore(),
            new CapturingExecutor(),
            queryLibraryStore()
        );
        session.bufferCommand(WorkspaceCommand.parse("buffer set select * from invoices"));

        String saved = session.libraryCommand(
            WorkspaceCommand.parse("lib save Invoice Report --desc monthly totals --tags finance,month-end --favorite")
        );
        String listed = session.libraryCommand(WorkspaceCommand.parse("lib list"));
        String searched = session.libraryCommand(WorkspaceCommand.parse("lib search month-end"));
        String unfavorited = session.libraryCommand(WorkspaceCommand.parse("lib unfavorite invoice-report"));
        String deletedWithoutConfirmation = session.libraryCommand(WorkspaceCommand.parse("lib delete invoice-report"));
        String deleted = session.libraryCommand(WorkspaceCommand.parse("lib delete invoice-report --yes"));

        assertTrue(saved.contains("Saved query: invoice-report"));
        assertTrue(saved.contains(QueryLibraryStore.PRIVACY_WARNING));
        assertTrue(listed.contains("* invoice-report | Invoice Report | finance, month-end | monthly totals"));
        assertTrue(searched.contains("invoice-report | Invoice Report"));
        assertTrue(unfavorited.contains("Updated favorite: invoice-report = false"));
        assertEquals("Use --yes to delete query: invoice-report", deletedWithoutConfirmation);
        assertEquals("Deleted query: invoice-report", deleted);
        assertEquals("No saved queries", session.libraryCommand(WorkspaceCommand.parse("lib list")));
    }

    @Test
    public void queryLibraryLoadRequiresReplaceForDirtyBufferAndDoesNotExecuteSql() throws Exception {
        QueryLibraryStore store = queryLibraryStore();
        store.save("Drop Users", "delete from users", "dangerous", Arrays.asList("ops"), false);
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(null, editorStore(), executor, store);
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        session.bufferCommand(WorkspaceCommand.parse("buffer set select * from users"));

        String blocked = session.libraryCommand(WorkspaceCommand.parse("lib load drop-users"));
        String loaded = session.libraryCommand(WorkspaceCommand.parse("lib load drop-users --replace"));

        assertEquals("Buffer has content. Re-run with --replace to load query: drop-users", blocked);
        assertTrue(loaded.contains("Loaded query into buffer: drop-users"));
        assertEquals("delete from users", session.dashboardState().buffer());
        assertEquals(null, executor.statement);

        String runBlocked = session.runCurrentBuffer();

        assertTrue(runBlocked.contains(SafetyGuard.MISSING_WHERE_MESSAGE));
        assertEquals(null, executor.statement);
    }

    @Test
    public void queryLibraryTemplateCommandsSavePreviewAndFillWithoutAutoExecution() throws Exception {
        QueryLibraryStore store = queryLibraryStore();
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(null, editorStore(), executor, store);
        session.bufferCommand(
            WorkspaceCommand.parse("buffer set select * from customers where id = {{customer_id}} and status = {{status}}")
        );

        String saved = session.libraryCommand(WorkspaceCommand.parse("lib save Customer Template --template --tags support"));
        QueryLibraryEntry entry = store.load("customer-template");
        String preview = session.libraryCommand(
            WorkspaceCommand.parse("lib preview customer-template --param customer_id=42 --param status='ACTIVE'")
        );
        String blocked = session.libraryCommand(
            WorkspaceCommand.parse("lib fill customer-template --param customer_id=42 --param status='ACTIVE'")
        );
        String filled = session.libraryCommand(
            WorkspaceCommand.parse("lib fill customer-template --replace --param customer_id=42 --param status='ACTIVE'")
        );

        assertTrue(saved.contains("Saved template: customer-template"));
        assertTrue(saved.contains("Raw substitution warning"));
        assertTrue(entry.template());
        assertEquals(Arrays.asList("customer_id", "status"), entry.templateParameters());
        assertTrue(preview.contains("select * from customers where id = 42 and status = 'ACTIVE'"));
        assertTrue(preview.contains("Raw substitution warning"));
        assertEquals("Buffer has content. Re-run with --replace to load rendered template: customer-template", blocked);
        assertTrue(filled.contains("Rendered template loaded into buffer: customer-template"));
        assertEquals("select * from customers where id = 42 and status = 'ACTIVE'", session.dashboardState().buffer());
        assertEquals(null, executor.statement);
    }

    @Test
    public void queryLibraryTemplateFillReportsMissingValuesAndPreservesSafetyGuard() throws Exception {
        QueryLibraryStore store = queryLibraryStore();
        store.saveTemplate(
            "Delete Template",
            "delete from users where id = {{user_id}}",
            "dangerous template",
            Arrays.asList("ops"),
            false,
            "",
            "",
            Arrays.asList("user_id"),
            false
        );
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(null, editorStore(), executor, store);
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));

        String missing = session.libraryCommand(WorkspaceCommand.parse("lib fill delete-template --replace"));
        String filled = session.libraryCommand(WorkspaceCommand.parse("lib fill delete-template --replace --param user_id=7"));
        String runBlocked = session.runCurrentBuffer();

        assertEquals("Missing value for template parameter: user_id", missing);
        assertTrue(filled.contains("Rendered template loaded into buffer: delete-template"));
        assertEquals("delete from users where id = 7", session.dashboardState().buffer());
        assertTrue(runBlocked.contains("Safety mode blocked a dangerous SQL statement"));
        assertEquals(null, executor.statement);
    }

    @Test
    public void replDispatchesQueryLibraryCommandsAndPreservesMetadataWorkflow() throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            editorStore(),
            new CapturingExecutor(),
            queryLibraryStore()
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        new InteractiveWorkspace(
            input("buffer set select * from orders\nlib save Orders --tags reporting\nlib search reporting\ntables user\nexit\n"),
            output,
            session
        ).run();

        String rendered = text(output);
        assertTrue(rendered.contains("Saved query: orders"));
        assertTrue(rendered.contains("orders | Orders | reporting"));
        assertTrue(rendered.contains("No active connection selected"));
    }

    private InteractiveWorkspace.Session session(ConnectionRegistry registry, InteractiveWorkspace.WorkspaceSqlExecutor executor)
        throws Exception {
        return new InteractiveWorkspace.Session(registry, editorStore(), executor);
    }

    private EditorStateStore editorStore() throws Exception {
        return new EditorStateStore(temporaryFolder.newFile("editor.properties"), 5);
    }

    private QueryLibraryStore queryLibraryStore() throws Exception {
        return new QueryLibraryStore(temporaryFolder.newFile("query-library.properties"), Clock.systemUTC());
    }

    private ConnectionRegistry registry() throws Exception {
        File baseDirectory = temporaryFolder.newFolder("connections" + System.nanoTime());
        return new ConnectionRegistry(baseDirectory, new ProtectedSecretStore(new File(baseDirectory, "secrets")));
    }

    private static ByteArrayInputStream input(String value) throws Exception {
        return new ByteArrayInputStream(value.getBytes("UTF-8"));
    }

    private static String text(ByteArrayOutputStream output) throws Exception {
        return new String(output.toByteArray(), "UTF-8");
    }

    private static final class CapturingExecutor implements InteractiveWorkspace.WorkspaceSqlExecutor {

        private String statement;

        @Override
        public SqlExecutionResult executeSingle(ConnectionConfig config, String statement) throws SQLException {
            this.statement = statement;
            return new SqlExecutionResult("RESULT: " + statement);
        }
    }
}
