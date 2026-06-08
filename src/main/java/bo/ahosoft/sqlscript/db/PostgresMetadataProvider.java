package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PostgresMetadataProvider implements MetadataProvider {

    private final List<String> schemas;

    public PostgresMetadataProvider(List<String> schemas) {
        this.schemas = schemas == null || schemas.isEmpty() ? Collections.singletonList("public") : new ArrayList<>(schemas);
    }

    @Override
    public String tables(String filter) {
        String sql =
            "select table_schema, table_name from information_schema.tables where table_type = 'BASE TABLE' and " +
            schemaPredicate("table_schema");
        if (filter != null && !filter.trim().isEmpty()) {
            sql += " and lower(table_name) like lower('%" + MetadataProviderSupport.escapeSqlLiteral(filter.trim()) + "%')";
        }
        return sql + " order by table_schema, table_name";
    }

    @Override
    public String search(String text) {
        String value = MetadataProviderSupport.escapeSqlLiteral(text.trim());
        return (
            "select 'TABLE' as object_type, table_schema || '.' || table_name as object_name, '-' as column_name from information_schema.tables " +
            "where table_type = 'BASE TABLE' and " +
            schemaPredicate("table_schema") +
            " and lower(table_name) like lower('%" +
            value +
            "%') " +
            "union all select 'COLUMN' as object_type, table_schema || '.' || table_name as object_name, column_name from information_schema.columns " +
            "where " +
            schemaPredicate("table_schema") +
            " and lower(column_name) like lower('%" +
            value +
            "%') order by object_type, object_name, column_name"
        );
    }

    @Override
    public String sample(String tableName, int limit) {
        return "select * from " + MetadataProviderSupport.safeIdentifier(tableName) + " limit " + limit;
    }

    @Override
    public String describe(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select ordinal_position as column_id, column_name, data_type, character_maximum_length as data_length, is_nullable as nullable " +
            "from information_schema.columns where " +
            schemaPredicate("table_schema") +
            " and table_name = '" +
            escapedTable +
            "' order by ordinal_position"
        );
    }

    @Override
    public String details(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select c.ordinal_position as column_id, c.column_name, c.data_type, c.is_nullable as nullable, " +
            "case when pk.column_name is null then 'NO' else 'YES' end as primary_key, coalesce(fk.references_table || '.' || fk.references_column, '-') as foreign_key_references " +
            "from information_schema.columns c left join (select kcu.table_schema, kcu.table_name, kcu.column_name from information_schema.table_constraints tc join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema where tc.constraint_type = 'PRIMARY KEY') pk " +
            "on pk.table_schema = c.table_schema and pk.table_name = c.table_name and pk.column_name = c.column_name left join (select kcu.table_schema, kcu.table_name, kcu.column_name, ccu.table_name as references_table, ccu.column_name as references_column from information_schema.table_constraints tc join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema where tc.constraint_type = 'FOREIGN KEY') fk " +
            "on fk.table_schema = c.table_schema and fk.table_name = c.table_name and fk.column_name = c.column_name where " +
            schemaPredicate("c.table_schema") +
            " and c.table_name = '" +
            escapedTable +
            "' order by c.ordinal_position"
        );
    }

    @Override
    public String indexes(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select schemaname, tablename, indexname, indexdef from pg_indexes where " +
            schemaPredicate("schemaname") +
            " and tablename = '" +
            escapedTable +
            "' order by indexname"
        );
    }

    @Override
    public String constraints(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select constraint_name, constraint_type, table_schema, table_name from information_schema.table_constraints where " +
            schemaPredicate("table_schema") +
            " and table_name = '" +
            escapedTable +
            "' order by constraint_type, constraint_name"
        );
    }

    @Override
    public String fkIn(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select kcu.table_name as foreign_table_name, kcu.column_name, ccu.column_name as references_column from information_schema.table_constraints tc join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema where tc.constraint_type = 'FOREIGN KEY' and " +
            schemaPredicate("ccu.table_schema") +
            " and ccu.table_name = '" +
            escapedTable +
            "' order by kcu.table_name, kcu.column_name"
        );
    }

    @Override
    public String fkOut(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(unqualified(tableName));
        return (
            "select tc.constraint_name, kcu.column_name, ccu.table_name as references_table, ccu.column_name as references_column from information_schema.table_constraints tc join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema where tc.constraint_type = 'FOREIGN KEY' and " +
            schemaPredicate("kcu.table_schema") +
            " and kcu.table_name = '" +
            escapedTable +
            "' order by tc.constraint_name, kcu.ordinal_position"
        );
    }

    @Override
    public String explain(String script) {
        return "explain " + MetadataProviderSupport.stripTrailingSemicolon(script);
    }

    @Override
    public String count(String tableName) {
        return "select count(*) as total from " + MetadataProviderSupport.safeIdentifier(tableName);
    }

    private String schemaPredicate(String column) {
        StringBuilder predicate = new StringBuilder(column).append(" in (");
        for (int i = 0; i < schemas.size(); i++) {
            if (i > 0) {
                predicate.append(',');
            }
            predicate.append('\'').append(MetadataProviderSupport.escapeSqlLiteral(schemas.get(i))).append('\'');
        }
        return predicate.append(')').toString();
    }

    private static String unqualified(String tableName) {
        MetadataProviderSupport.safeIdentifier(tableName);
        int dot = tableName.indexOf('.');
        return dot < 0 ? tableName : tableName.substring(dot + 1);
    }
}
