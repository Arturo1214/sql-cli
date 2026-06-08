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

public class ConnectionListComponentTest {

    @Test
    public void rendersSavedConnectionsAndCreationActionsInLeftPanel() {
        ConnectionListComponent component = new ConnectionListComponent(
            Arrays.asList(
                new ConnectionListComponent.ConnectionItem(
                    "qa-oracle",
                    new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa", "secret")
                ),
                new ConnectionListComponent.ConnectionItem(
                    "reporting-postgres",
                    new ConnectionConfig(
                        DatabaseType.POSTGRESQL,
                        "jdbc:postgresql://localhost:5432/reporting",
                        "report",
                        "secret",
                        Arrays.asList("public")
                    )
                )
            )
        );

        ConnectionListComponent.RenderedPanel rendered = component.render(99, 1, WorkspaceFocus.CONNECTIONS);

        assertTrue(rendered.lines().contains("Connections *"));
        assertTrue(rendered.lines().contains("  [DEV] qa-oracle [ORACLE]"));
        assertTrue(rendered.lines().contains("* [DEV] reporting-postgres [POSTGRESQL] schema=public"));
        assertTrue(rendered.lines().contains("Actions"));
        assertFalse(rendered.lines().contains("  Tables"));
        assertFalse(rendered.lines().contains("> Tables"));
        assertFalse(rendered.lines().contains("  Describe"));
        assertFalse(rendered.lines().contains("  Indexes"));
        assertTrue(rendered.lines().contains("  New Oracle connection"));
        assertTrue(rendered.lines().contains("> New PostgreSQL connection"));
        assertFalse(rendered.selected().isPresent());
        assertEquals("reporting-postgres", rendered.active().name());
        assertEquals(4, rendered.rowCount());
        assertEquals(Arrays.asList("New Oracle connection", "New PostgreSQL connection"), rendered.actionLabels());
    }

    @Test
    public void rendersProdConnectionsWithVisibleWarningMarker() {
        ConnectionListComponent component = new ConnectionListComponent(
            Collections.singletonList(
                new ConnectionListComponent.ConnectionItem(
                    "billing-db",
                    new ConnectionConfig(
                        DatabaseType.ORACLE,
                        ConnectionEnvironment.PROD,
                        "jdbc:oracle:thin:@prod:1521/PROD",
                        "support",
                        "secret",
                        Collections.<String>emptyList()
                    )
                )
            )
        );

        ConnectionListComponent.RenderedPanel rendered = component.render(0, 0, WorkspaceFocus.CONNECTIONS);

        assertTrue(rendered.lines().contains("* !! PROD !! [PROD] billing-db [ORACLE]"));
    }

    @Test
    public void rendersCreationActionsAsSelectableRowsWhenNoConnectionsAreSaved() {
        ConnectionListComponent component = new ConnectionListComponent(Collections.<ConnectionListComponent.ConnectionItem>emptyList());

        ConnectionListComponent.RenderedPanel rendered = component.render(1, -1, WorkspaceFocus.CONNECTIONS);

        assertTrue(rendered.lines().contains("Connections *"));
        assertTrue(rendered.lines().contains("No saved connections"));
        assertTrue(rendered.lines().contains("  New Oracle connection"));
        assertTrue(rendered.lines().contains("> New PostgreSQL connection"));
        assertFalse(rendered.selected().isPresent());
        assertEquals(2, rendered.rowCount());
    }

    @Test
    public void clampsSelectionWhenNoConnectionsAreSavedAndKeepsCreationActionsAvailable() {
        ConnectionListComponent component = new ConnectionListComponent(Collections.<ConnectionListComponent.ConnectionItem>emptyList());

        ConnectionListComponent.RenderedPanel rendered = component.render(4, -1, WorkspaceFocus.EDITOR);

        assertTrue(rendered.lines().contains("Connections"));
        assertTrue(rendered.lines().contains("No saved connections"));
        assertFalse(rendered.selected().isPresent());
        assertEquals(Arrays.asList("New Oracle connection", "New PostgreSQL connection"), rendered.actionLabels());
    }

    @Test
    public void preparesPostgreSqlConnectionWithPublicDefaultSchemaWhenNoSchemaIsProvided() {
        ConnectionConfig config = ConnectionListComponent.prepareConnection(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost:5432/app",
            "app",
            "secret",
            Collections.<String>emptyList()
        );

        assertEquals(DatabaseType.POSTGRESQL, config.databaseType());
        assertEquals(Collections.singletonList("public"), config.schemas());
    }

    @Test
    public void rendersViewportWithManyLongConnectionsAndKeepsSelectionVisibleWithinWidth() {
        java.util.List<ConnectionListComponent.ConnectionItem> items = new java.util.ArrayList<ConnectionListComponent.ConnectionItem>();
        for (int i = 1; i <= 30; i++) {
            items.add(
                new ConnectionListComponent.ConnectionItem(
                    "very-long-reporting-connection-name-that-would-overflow-pane-" + i,
                    new ConnectionConfig(
                        DatabaseType.POSTGRESQL,
                        "jdbc:postgresql://localhost:5432/db" + i,
                        "user",
                        "secret",
                        Collections.singletonList("public")
                    )
                )
            );
        }
        ConnectionListComponent component = new ConnectionListComponent(items);

        ConnectionListComponent.RenderedPanel rendered = component.render(25, -1, WorkspaceFocus.CONNECTIONS, 8, 32);

        assertEquals(8, rendered.lines().size());
        assertTrue(rendered.lines().toString().contains("> [DEV] very-long-reporting"));
        assertTrue(rendered.selected().name().endsWith("26"));
        for (String line : rendered.lines()) {
            assertTrue("line should fit menu viewport: " + line, line.length() <= 32);
        }
    }

    @Test
    public void ignoresOracleSchemaValuesWhenPreparingConnection() {
        ConnectionConfig config = ConnectionListComponent.prepareConnection(
            DatabaseType.ORACLE,
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "system",
            "secret",
            Arrays.asList("SH", "HR")
        );

        assertEquals(DatabaseType.ORACLE, config.databaseType());
        assertTrue(config.schemas().isEmpty());
    }
}
