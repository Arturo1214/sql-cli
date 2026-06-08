package bo.ahosoft.sqlscript.domain;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public enum DatabaseType {
    ORACLE("jdbc:oracle:"),
    POSTGRESQL("jdbc:postgresql:");

    private final String jdbcUrlPrefix;

    DatabaseType(String jdbcUrlPrefix) {
        this.jdbcUrlPrefix = jdbcUrlPrefix;
    }

    public static DatabaseType fromStoredValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ORACLE;
        }
        for (DatabaseType databaseType : values()) {
            if (databaseType.name().equalsIgnoreCase(value.trim())) {
                return databaseType;
            }
        }
        throw new IllegalArgumentException("Unsupported database type: " + value);
    }

    public boolean acceptsJdbcUrl(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.trim().toLowerCase().startsWith(jdbcUrlPrefix);
    }
}
