package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class MetadataCatalogSnapshotTest {

    @Test
    public void buildsImmutableTableSnapshotFromMetadata() {
        List<String> discoveredTables = new ArrayList<String>(Arrays.asList("orders", "users"));

        MetadataCatalogSnapshot snapshot = MetadataCatalogSnapshot.ofTables(discoveredTables);
        discoveredTables.add("audit_log");

        assertEquals(Arrays.asList("orders", "users"), snapshot.tableNames());
        try {
            snapshot.tableNames().add("mutable");
        } catch (UnsupportedOperationException ex) {
            assertEquals(Arrays.asList("orders", "users"), snapshot.tableNames());
            return;
        }
        throw new AssertionError("Expected table names to be read-only");
    }

    @Test
    public void filtersTablesByCaseInsensitivePrefixWithoutChangingSnapshot() {
        MetadataCatalogSnapshot snapshot = MetadataCatalogSnapshot.ofTables(Arrays.asList("orders", "ORDER_ITEMS", "users"));

        assertEquals(Arrays.asList("ORDER_ITEMS", "orders"), snapshot.matchingTables("ord"));
        assertEquals(Arrays.asList("orders", "ORDER_ITEMS", "users"), snapshot.tableNames());
    }

    @Test
    public void returnsEmptySnapshotWhenMetadataIsUnavailable() throws Exception {
        MetadataCatalogLoader loader = new MetadataCatalogLoader() {
            @Override
            public MetadataCatalogSnapshot load(ConnectionConfig connectionConfig) throws SQLException {
                throw new SQLException("metadata unavailable");
            }
        };

        MetadataCatalogSnapshot snapshot = MetadataCatalogSnapshot.safeLoad(
            loader,
            new ConnectionConfig("jdbc:oracle:thin:@//localhost:1521/XEPDB1", "app", "secret")
        );

        assertTrue(snapshot.tableNames().isEmpty());
    }

    @Test
    public void keepsPartialMetadataAndIgnoresMissingEntries() {
        MetadataCatalogSnapshot snapshot = MetadataCatalogSnapshot.ofTables(Arrays.asList("orders", null, " ", "users"));

        assertEquals(Arrays.asList("orders", "users"), snapshot.tableNames());
        assertEquals(Collections.singletonList("users"), snapshot.matchingTables("use"));
    }

    @Test
    public void loadsSnapshotUsingProviderTableQuery() throws Exception {
        ConnectionConfig config = new ConnectionConfig(
            DatabaseType.POSTGRESQL,
            "jdbc:postgresql://localhost/app",
            "app",
            "secret",
            Collections.singletonList("public")
        );
        MetadataCatalogLoader loader = new JdbcMetadataCatalogLoader(
            new FakeConnectionFactory(),
            MetadataProviderFactory.forConfig(config)
        );

        MetadataCatalogSnapshot snapshot = loader.load(config);

        assertEquals(Arrays.asList("orders", "users"), snapshot.tableNames());
    }

    private static final class FakeConnectionFactory extends JdbcConnectionFactory {

        @Override
        public Connection open(ConnectionConfig config) {
            return ProxyJdbc.connection(
                new ProxyJdbc.StatementHandler() {
                    @Override
                    public ResultSet executeQuery(String sql) throws SQLException {
                        if (!sql.contains("information_schema.tables")) {
                            throw new SQLException("Unexpected SQL: " + sql);
                        }
                        return ProxyJdbc.resultSet("table_name", "orders", "users");
                    }

                    @Override
                    public boolean execute(String sql) {
                        return false;
                    }

                    @Override
                    public ResultSet resultSet() {
                        return null;
                    }
                }
            );
        }
    }
}
