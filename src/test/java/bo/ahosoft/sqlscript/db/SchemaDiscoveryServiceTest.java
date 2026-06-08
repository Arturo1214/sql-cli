package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SchemaDiscoveryServiceTest {

    @Test
    public void listsPostgresqlSchemasFromJdbc() throws Exception {
        Connection connection = FakeJdbc.connectionWithRows("public", "app", "reporting");

        List<String> schemas = new SchemaDiscoveryService().discover(connection);

        assertEquals(Arrays.asList("app", "public", "reporting"), schemas);
    }

    @Test
    public void keepsExplicitSchemaSelectionInDiscoveredOrder() {
        List<String> schemas = Arrays.asList("app", "public", "reporting");

        List<String> selected = new SchemaDiscoveryService().resolveSelection(schemas, Arrays.asList("reporting", "public"));

        assertEquals(Arrays.asList("public", "reporting"), selected);
    }

    @Test
    public void defaultsToPublicWhenNoExplicitSelectionExists() {
        List<String> schemas = Arrays.asList("app", "public", "reporting");

        List<String> selected = new SchemaDiscoveryService().resolveSelection(schemas, Collections.<String>emptyList());

        assertEquals(Collections.singletonList("public"), selected);
    }

    @Test
    public void defaultsToFirstDiscoveredSchemaWhenPublicIsUnavailable() {
        List<String> schemas = Arrays.asList("app", "reporting");

        List<String> selected = new SchemaDiscoveryService().resolveSelection(schemas, Collections.<String>emptyList());

        assertEquals(Collections.singletonList("app"), selected);
    }

    private static final class FakeJdbc {

        static Connection connectionWithRows(final String... schemas) {
            return ProxyJdbc.connection(
                new ProxyJdbc.StatementHandler() {
                    @Override
                    public ResultSet executeQuery(String sql) throws SQLException {
                        if (!sql.contains("information_schema.schemata")) {
                            throw new SQLException("Unexpected SQL: " + sql);
                        }
                        return ProxyJdbc.resultSet("schema_name", schemas);
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
