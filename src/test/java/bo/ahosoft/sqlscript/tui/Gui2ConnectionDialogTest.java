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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Gui2ConnectionDialogTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void validationPreventsSessionCreationForMissingNameOrWrongJdbcUrl() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog dialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result missingName = dialog.submit(
            new Gui2ConnectionDialog.Request(
                DatabaseType.ORACLE,
                " ",
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "ora",
                "secret",
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
            )
        );
        Gui2ConnectionDialog.Result wrongUrl = dialog.submit(
            new Gui2ConnectionDialog.Request(
                DatabaseType.POSTGRESQL,
                "reporting",
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "pg",
                "secret",
                Collections.<String>emptyList(),
                Arrays.asList("public")
            )
        );

        assertFalse(missingName.created());
        assertEquals("Connection name is required", missingName.message());
        assertFalse(wrongUrl.created());
        assertEquals("JDBC URL does not match PostgreSQL", wrongUrl.message());
        assertTrue(session.connectionNames().isEmpty());
    }

    @Test
    public void cancelDoesNotCreateConnection() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog dialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result result = dialog.cancel();

        assertFalse(result.created());
        assertEquals("Connection creation cancelled", result.message());
        assertTrue(session.connectionNames().isEmpty());
    }

    @Test
    public void oracleSubmitIgnoresSchemaSelection() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog dialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result result = dialog.submit(
            new Gui2ConnectionDialog.Request(
                DatabaseType.ORACLE,
                "local",
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "ora",
                "secret",
                Arrays.asList("ignored"),
                Arrays.asList("ignored")
            )
        );

        assertTrue(result.created());
        assertEquals(DatabaseType.ORACLE, result.config().databaseType());
        assertTrue(result.config().schemas().isEmpty());
        assertEquals("local", session.activeConnectionName());
    }

    @Test
    public void postgresqlSubmitDefaultsToPublicSchemaWhenNoSchemaIsSelected() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog dialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result result = dialog.submit(
            new Gui2ConnectionDialog.Request(
                DatabaseType.POSTGRESQL,
                "reporting",
                "jdbc:postgresql://localhost:5432/app",
                "pg",
                "secret",
                Collections.<String>emptyList(),
                Arrays.asList("public", "audit")
            )
        );

        assertTrue(result.created());
        assertEquals(Arrays.asList("public"), result.config().schemas());
        assertEquals("reporting", session.activeConnectionName());
        assertEquals("Connection saved: reporting", session.dashboardState().statusMessage());
    }

    @Test
    public void postgresqlWizardFormSavesValidValuesAndShowsVisibleFeedback() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).open(DatabaseType.POSTGRESQL);

        assertEquals(DatabaseType.POSTGRESQL, form.databaseType());
        assertEquals(Arrays.asList("Name", "Environment", "JDBC URL", "Username", "Password", "Schemas"), form.fieldLabels());
        assertEquals(ConnectionEnvironment.DEV, form.environment());

        Gui2ConnectionDialog.Result result = form
            .name(" reporting ")
            .environment(ConnectionEnvironment.STAGING)
            .jdbcUrl(" jdbc:postgresql://localhost:5432/app ")
            .username(" pg ")
            .password("secret")
            .availableSchemas(Arrays.asList("public", "audit"))
            .save();

        assertTrue(result.created());
        assertEquals("Connection saved: reporting", form.feedback());
        assertEquals("reporting", session.activeConnectionName());
        assertEquals(DatabaseType.POSTGRESQL, result.config().databaseType());
        assertEquals(ConnectionEnvironment.STAGING, result.config().environment());
        assertEquals("jdbc:postgresql://localhost:5432/app", result.config().jdbcUrl());
        assertEquals("pg", result.config().username());
    }

    @Test
    public void wizardFormSavesSelectedProdForOracleAndPostgresql() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog dialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result oracle = dialog
            .open(DatabaseType.ORACLE)
            .name("oracle-prod")
            .environment(ConnectionEnvironment.PROD)
            .jdbcUrl("jdbc:oracle:thin:@prod:1521/PROD")
            .username("support")
            .password("secret")
            .save();
        Gui2ConnectionDialog.Result postgresql = dialog
            .open(DatabaseType.POSTGRESQL)
            .name("pg-prod")
            .environment(ConnectionEnvironment.PROD)
            .jdbcUrl("jdbc:postgresql://prod:5432/app")
            .username("pg")
            .password("secret")
            .selectedSchemas(Arrays.asList("public"))
            .availableSchemas(Arrays.asList("public"))
            .save();

        assertTrue(oracle.created());
        assertEquals(ConnectionEnvironment.PROD, oracle.config().environment());
        assertTrue(postgresql.created());
        assertEquals(ConnectionEnvironment.PROD, postgresql.config().environment());
    }

    @Test
    public void requestDefaultsEnvironmentToDevWhenOmitted() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog.Result result = new Gui2ConnectionDialog(session).submit(
            new Gui2ConnectionDialog.Request(
                DatabaseType.ORACLE,
                null,
                "local",
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "ora",
                "secret",
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
            )
        );

        assertTrue(result.created());
        assertEquals(ConnectionEnvironment.DEV, result.config().environment());
    }

    @Test
    public void wizardCancelAndBackReturnWithoutSavingConnection() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).open(DatabaseType.ORACLE);

        form.name("local").jdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1").username("ora").password("secret");
        Gui2ConnectionDialog.Result back = form.back();
        Gui2ConnectionDialog.Result cancel = form.cancel();

        assertFalse(back.created());
        assertEquals("Connection creation cancelled", back.message());
        assertFalse(cancel.created());
        assertEquals("Connection creation cancelled", form.feedback());
        assertTrue(session.connectionNames().isEmpty());
    }

    @Test
    public void wizardRejectsMissingRequiredFieldsWithVisibleFeedback() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).open(DatabaseType.POSTGRESQL);

        Gui2ConnectionDialog.Result missingUrl = form.name("reporting").username("pg").password("secret").save();
        assertFalse(missingUrl.created());
        assertEquals("JDBC URL is required", missingUrl.message());
        assertEquals("JDBC URL is required", form.feedback());

        form.jdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1");
        Gui2ConnectionDialog.Result wrongUrl = form.save();

        assertFalse(wrongUrl.created());
        assertEquals("JDBC URL does not match PostgreSQL", form.feedback());
        assertTrue(session.connectionNames().isEmpty());
    }

    @Test
    public void wizardAppliesPostgresqlPublicDefaultAndOracleSchemaIgnore() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        Gui2ConnectionDialog postgresqlDialog = new Gui2ConnectionDialog(session);

        Gui2ConnectionDialog.Result postgresql = postgresqlDialog
            .open(DatabaseType.POSTGRESQL)
            .name("reporting")
            .jdbcUrl("jdbc:postgresql://localhost:5432/app")
            .username("pg")
            .password("secret")
            .availableSchemas(Arrays.asList("public", "audit"))
            .save();
        Gui2ConnectionDialog.Result oracle = postgresqlDialog
            .open(DatabaseType.ORACLE)
            .name("oracle-local")
            .jdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1")
            .username("ora")
            .password("secret")
            .selectedSchemas(Arrays.asList("ignored"))
            .availableSchemas(Arrays.asList("ignored"))
            .save();

        assertTrue(postgresql.created());
        assertEquals(Arrays.asList("public"), postgresql.config().schemas());
        assertTrue(oracle.created());
        assertTrue(oracle.config().schemas().isEmpty());
    }

    @Test
    public void createModeAllowsNewPasswordRevealWithoutChangingSavedValue() {
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(new InteractiveWorkspace.Session()).open(DatabaseType.ORACLE);

        form.password("secret");
        assertEquals(Gui2ConnectionDialog.Mode.CREATE, form.mode());
        assertEquals("******", form.passwordDisplayValue());

        form.togglePasswordReveal();
        assertEquals("secret", form.passwordDisplayValue());

        form.togglePasswordReveal();
        assertEquals("******", form.passwordDisplayValue());
        assertEquals("secret", form.passwordDraft().resolve(null));
    }

    @Test
    public void editModePrefillsConnectionWithoutRevealingExistingPassword() {
        ConnectionConfig existing = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            ConnectionEnvironment.STAGING,
            "jdbc:postgresql://localhost:5432/app",
            "pg",
            "stored-secret",
            Arrays.asList("audit")
        );

        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(new InteractiveWorkspace.Session()).openEdit("reporting", existing);

        assertEquals(Gui2ConnectionDialog.Mode.EDIT, form.mode());
        assertEquals("reporting", form.name());
        assertEquals(ConnectionEnvironment.STAGING, form.environment());
        assertEquals("jdbc:postgresql://localhost:5432/app", form.jdbcUrl());
        assertEquals("pg", form.username());
        assertEquals(Arrays.asList("audit"), form.selectedSchemas());
        assertEquals("", form.passwordDisplayValue());

        form.togglePasswordReveal();
        assertEquals("", form.passwordDisplayValue());
        assertTrue(form.passwordDraft().preservesExisting());
        assertEquals("stored-secret", form.passwordDraft().resolve("stored-secret"));
    }

    @Test
    public void editBlankPasswordPreservesSecretAndReplacementCanBeRevealedBeforeSave() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        ConnectionConfig existing = new ConnectionConfig(
            DatabaseType.ORACLE,
            ConnectionEnvironment.DEV,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "ora",
            "stored-secret",
            Collections.<String>emptyList()
        );
        session.addConnection("local", existing);
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).openEdit("local", existing);

        Gui2ConnectionDialog.Result preserved = form.save();

        assertTrue(preserved.created());
        assertEquals("stored-secret", preserved.config().password());
        assertEquals("stored-secret", session.activeConnection().password());

        form.password("replacement");
        assertEquals("***********", form.passwordDisplayValue());
        form.togglePasswordReveal();
        assertEquals("replacement", form.passwordDisplayValue());

        Gui2ConnectionDialog.Result replaced = form.save();

        assertTrue(replaced.created());
        assertEquals("replacement", replaced.config().password());
    }

    @Test
    public void invalidEditDraftIsRejectedBeforeSessionMutation() {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session();
        ConnectionConfig existing = new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "stored-secret");
        session.addConnection("local", existing);
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).openEdit("local", existing);

        Gui2ConnectionDialog.Result result = form.jdbcUrl("jdbc:postgresql://localhost:5432/app").save();

        assertFalse(result.created());
        assertEquals("JDBC URL does not match Oracle", result.message());
        assertEquals("jdbc:oracle:thin:@localhost:1521/XEPDB1", session.activeConnection().jdbcUrl());
        assertEquals("stored-secret", session.activeConnection().password());
    }

    @Test
    public void testConnectionFeedbackUsesDraftWithoutSavingIt() throws Exception {
        InteractiveWorkspace.Session session = sessionWithTestResult(ConnectionTestResult.success());
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).open(DatabaseType.POSTGRESQL);

        Gui2ConnectionDialog.Result result = form
            .name("reporting")
            .jdbcUrl("jdbc:postgresql://localhost:5432/app")
            .username("pg")
            .password("secret")
            .testConnection(250L);

        assertFalse(result.created());
        assertEquals("Connection test succeeded", result.message());
        assertEquals("Connection test succeeded", form.feedback());
        assertTrue(session.connectionNames().isEmpty());
    }

    @Test
    public void testConnectionFailureAndTimeoutFeedbackNeverPersistDraft() throws Exception {
        InteractiveWorkspace.Session failedSession = sessionWithTestResult(ConnectionTestResult.failure("bad credentials"));
        Gui2ConnectionDialog.Form failedForm = new Gui2ConnectionDialog(failedSession).open(DatabaseType.ORACLE);
        Gui2ConnectionDialog.Result failed = failedForm
            .name("local")
            .jdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1")
            .username("ora")
            .password("bad")
            .testConnection(100L);

        assertFalse(failed.created());
        assertEquals("bad credentials", failed.message());
        assertTrue(failedSession.connectionNames().isEmpty());

        InteractiveWorkspace.Session timeoutSession = sessionWithTestResult(ConnectionTestResult.timeout(100L));
        Gui2ConnectionDialog.Form timeoutForm = new Gui2ConnectionDialog(timeoutSession).open(DatabaseType.ORACLE);
        Gui2ConnectionDialog.Result timeout = timeoutForm
            .name("local")
            .jdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1")
            .username("ora")
            .password("bad")
            .testConnection(100L);

        assertFalse(timeout.created());
        assertEquals("Connection test timed out after 100ms", timeout.message());
        assertTrue(timeoutSession.connectionNames().isEmpty());
    }

    @Test
    public void invalidTestDraftDoesNotCallConnectionTestService() throws Exception {
        CountingOpener opener = new CountingOpener(ConnectionTestResult.success());
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            editorStore(),
            null,
            null,
            new ConnectionTestService(opener, new ImmediateExecutor())
        );
        Gui2ConnectionDialog.Form form = new Gui2ConnectionDialog(session).open(DatabaseType.POSTGRESQL);

        Gui2ConnectionDialog.Result result = form.name("reporting").username("pg").password("secret").testConnection(250L);

        assertFalse(result.created());
        assertEquals("JDBC URL is required", result.message());
        assertEquals(0, opener.openCount);
    }

    private InteractiveWorkspace.Session sessionWithTestResult(ConnectionTestResult result) throws Exception {
        return new InteractiveWorkspace.Session(
            null,
            editorStore(),
            null,
            null,
            new ConnectionTestService(new CountingOpener(result), new ImmediateExecutor())
        );
    }

    private EditorStateStore editorStore() throws Exception {
        return new EditorStateStore(new java.io.File(temporaryFolder.newFolder(), "editor.properties"), 30);
    }

    private static final class CountingOpener implements ConnectionTestService.ConnectionOpener {

        private final ConnectionTestResult result;
        private int openCount;

        private CountingOpener(ConnectionTestResult result) {
            this.result = result;
        }

        @Override
        public Connection open(ConnectionConfig config) throws SQLException {
            openCount++;
            if (result.status() == ConnectionTestResult.Status.FAILURE) {
                throw new SQLException(result.message());
            }
            if (result.status() == ConnectionTestResult.Status.TIMEOUT) {
                throw new SQLException(result.message());
            }
            return null;
        }
    }

    private static final class ImmediateExecutor implements ConnectionTestService.TaskExecutor {

        @Override
        public ConnectionTestService.PendingTask submit(final ConnectionTestService.ConnectionTask task) {
            return new ConnectionTestService.PendingTask() {
                @Override
                public Connection get(long timeoutMillis) throws Exception {
                    return task.open();
                }

                @Override
                public void cancel() {}
            };
        }
    }
}
