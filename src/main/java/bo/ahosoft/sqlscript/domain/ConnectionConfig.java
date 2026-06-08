package bo.ahosoft.sqlscript.domain;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConnectionConfig {

    private final DatabaseType databaseType;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final List<String> schemas;

    public ConnectionConfig(String jdbcUrl, String username, String password) {
        this(DatabaseType.ORACLE, jdbcUrl, username, password, Collections.<String>emptyList());
    }

    public ConnectionConfig(DatabaseType databaseType, String jdbcUrl, String username, String password, List<String> schemas) {
        this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.schemas = this.databaseType == DatabaseType.POSTGRESQL ? normalizeSchemas(schemas) : Collections.<String>emptyList();
    }

    public DatabaseType databaseType() {
        return databaseType;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public List<String> schemas() {
        return schemas;
    }

    private static List<String> normalizeSchemas(List<String> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String schema : schemas) {
            if (schema != null && !schema.trim().isEmpty()) {
                normalized.add(schema.trim());
            }
        }
        return Collections.unmodifiableList(normalized);
    }
}
