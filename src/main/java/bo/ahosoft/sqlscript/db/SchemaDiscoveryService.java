package bo.ahosoft.sqlscript.db;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SchemaDiscoveryService {

    public List<String> discover(Connection connection) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select schema_name from information_schema.schemata order by schema_name")
        ) {
            while (resultSet.next()) {
                schemas.add(resultSet.getString(1));
            }
        }
        Collections.sort(schemas);
        return Collections.unmodifiableList(schemas);
    }

    public List<String> resolveSelection(List<String> discoveredSchemas, List<String> explicitSchemas) {
        List<String> discovered = normalize(discoveredSchemas);
        List<String> explicit = normalize(explicitSchemas);
        if (!explicit.isEmpty()) {
            Set<String> requested = new HashSet<>(explicit);
            List<String> selected = new ArrayList<>();
            for (String schema : discovered) {
                if (requested.contains(schema)) {
                    selected.add(schema);
                }
            }
            return Collections.unmodifiableList(selected);
        }
        if (discovered.contains("public")) {
            return Collections.singletonList("public");
        }
        if (discovered.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(discovered.get(0));
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }
}
