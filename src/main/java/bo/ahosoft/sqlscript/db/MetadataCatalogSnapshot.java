package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MetadataCatalogSnapshot {

    private static final MetadataCatalogSnapshot EMPTY = new MetadataCatalogSnapshot(Collections.<String>emptyList());

    private final List<String> tableNames;

    private MetadataCatalogSnapshot(List<String> tableNames) {
        this.tableNames = Collections.unmodifiableList(new ArrayList<String>(tableNames));
    }

    public static MetadataCatalogSnapshot empty() {
        return EMPTY;
    }

    public static MetadataCatalogSnapshot ofTables(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return empty();
        }
        List<String> normalized = new ArrayList<String>();
        for (String tableName : tableNames) {
            if (tableName != null && !tableName.trim().isEmpty()) {
                normalized.add(tableName.trim());
            }
        }
        return normalized.isEmpty() ? empty() : new MetadataCatalogSnapshot(normalized);
    }

    public static MetadataCatalogSnapshot safeLoad(MetadataCatalogLoader loader, ConnectionConfig connectionConfig) {
        if (loader == null) {
            return empty();
        }
        try {
            MetadataCatalogSnapshot snapshot = loader.load(connectionConfig);
            return snapshot == null ? empty() : snapshot;
        } catch (SQLException ex) {
            return empty();
        }
    }

    public List<String> tableNames() {
        return tableNames;
    }

    public List<String> matchingTables(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase();
        List<String> matches = new ArrayList<String>();
        for (String tableName : tableNames) {
            if (tableName.toLowerCase().startsWith(normalizedPrefix)) {
                matches.add(tableName);
            }
        }
        Collections.sort(
            matches,
            new Comparator<String>() {
                @Override
                public int compare(String left, String right) {
                    return left.compareToIgnoreCase(right);
                }
            }
        );
        return Collections.unmodifiableList(matches);
    }
}
