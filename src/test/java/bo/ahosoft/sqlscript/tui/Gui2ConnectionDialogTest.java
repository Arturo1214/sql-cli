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
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class Gui2ConnectionDialogTest {

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
        assertEquals(Arrays.asList("Name", "JDBC URL", "Username", "Password", "Schemas"), form.fieldLabels());

        Gui2ConnectionDialog.Result result = form
            .name(" reporting ")
            .jdbcUrl(" jdbc:postgresql://localhost:5432/app ")
            .username(" pg ")
            .password("secret")
            .availableSchemas(Arrays.asList("public", "audit"))
            .save();

        assertTrue(result.created());
        assertEquals("Connection saved: reporting", form.feedback());
        assertEquals("reporting", session.activeConnectionName());
        assertEquals(DatabaseType.POSTGRESQL, result.config().databaseType());
        assertEquals("jdbc:postgresql://localhost:5432/app", result.config().jdbcUrl());
        assertEquals("pg", result.config().username());
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
}
