package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public enum SqlDialect {
    GENERIC(commonKeywords()),
    ORACLE(
        merge(
            commonKeywords(),
            "connect",
            "dual",
            "minus",
            "merge",
            "model",
            "nvl",
            "prior",
            "rownum",
            "sequence",
            "start",
            "sysdate",
            "systimestamp"
        )
    ),
    POSTGRESQL(merge(commonKeywords(), "bigserial", "json", "jsonb", "limit", "offset", "returning", "serial", "true", "false", "with"));

    private final Set<String> keywords;

    SqlDialect(Set<String> keywords) {
        this.keywords = keywords;
    }

    public boolean isKeyword(String value) {
        return value != null && keywords.contains(value.toLowerCase(Locale.ROOT));
    }

    public static SqlDialect fromDatabaseType(DatabaseType databaseType) {
        if (databaseType == DatabaseType.ORACLE) {
            return ORACLE;
        }
        if (databaseType == DatabaseType.POSTGRESQL) {
            return POSTGRESQL;
        }
        return GENERIC;
    }

    public static SqlDialect fromDatabaseTypeName(String databaseType) {
        if (databaseType == null) {
            return GENERIC;
        }
        try {
            return fromDatabaseType(DatabaseType.valueOf(databaseType.trim().toUpperCase(Locale.ROOT)));
        } catch (RuntimeException ignored) {
            return GENERIC;
        }
    }

    private static Set<String> commonKeywords() {
        return new HashSet<String>(
            Arrays.asList(
                "and",
                "as",
                "between",
                "by",
                "case",
                "cast",
                "create",
                "delete",
                "distinct",
                "drop",
                "else",
                "end",
                "exists",
                "from",
                "full",
                "group",
                "having",
                "in",
                "inner",
                "insert",
                "into",
                "is",
                "join",
                "left",
                "like",
                "not",
                "null",
                "on",
                "or",
                "order",
                "outer",
                "right",
                "select",
                "set",
                "table",
                "then",
                "union",
                "update",
                "values",
                "view",
                "when",
                "where"
            )
        );
    }

    private static Set<String> merge(Set<String> base, String... extra) {
        Set<String> merged = new HashSet<String>(base);
        merged.addAll(Arrays.asList(extra));
        return merged;
    }
}
