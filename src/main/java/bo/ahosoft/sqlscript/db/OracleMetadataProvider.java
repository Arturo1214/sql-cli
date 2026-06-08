package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class OracleMetadataProvider implements MetadataProvider {

    @Override
    public String tables(String filter) {
        String sql = "select table_name from user_tables";
        if (filter != null && !filter.trim().isEmpty()) {
            sql += " where upper(table_name) like upper('%" + MetadataProviderSupport.escapeSqlLiteral(filter.trim()) + "%')";
        }
        return sql + " order by table_name";
    }

    @Override
    public String search(String text) {
        String value = MetadataProviderSupport.escapeSqlLiteral(text.trim());
        return (
            "select 'TABLE' as object_type, table_name as object_name, '-' as column_name " +
            "from user_tables where upper(table_name) like upper('%" +
            value +
            "%') " +
            "union all " +
            "select 'COLUMN' as object_type, table_name as object_name, column_name " +
            "from user_tab_columns where upper(column_name) like upper('%" +
            value +
            "%') " +
            "order by object_type, object_name, column_name"
        );
    }

    @Override
    public String sample(String tableName, int limit) {
        return "select * from " + MetadataProviderSupport.safeIdentifier(tableName) + " where rownum <= " + limit;
    }

    @Override
    public String describe(String tableName) {
        return (
            "select column_id, column_name, data_type, data_length, nullable " +
            "from user_tab_columns " +
            "where table_name = upper('" +
            MetadataProviderSupport.escapeSqlLiteral(tableName) +
            "') " +
            "order by column_id"
        );
    }

    @Override
    public String details(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(tableName);
        return (
            "select c.column_id, c.column_name, " +
            dataTypeExpression("c") +
            " as data_type, c.nullable, " +
            "case when exists (select 1 from user_constraints pk join user_cons_columns pkc on pk.constraint_name = pkc.constraint_name and pk.table_name = pkc.table_name " +
            "where pk.constraint_type = 'P' and pkc.table_name = c.table_name and pkc.column_name = c.column_name) then 'YES' else 'NO' end as primary_key, " +
            "nvl((select listagg(rcc.table_name || '.' || rcc.column_name, ', ') within group (order by fkc.position) from user_constraints fk " +
            "join user_cons_columns fkc on fk.constraint_name = fkc.constraint_name and fk.table_name = fkc.table_name join user_constraints refc on fk.r_constraint_name = refc.constraint_name " +
            "join user_cons_columns rcc on refc.constraint_name = rcc.constraint_name and fkc.position = rcc.position where fk.constraint_type = 'R' and fkc.table_name = c.table_name and fkc.column_name = c.column_name), '-') as foreign_key_references " +
            "from user_tab_columns c where c.table_name = upper('" +
            escapedTable +
            "') order by c.column_id"
        );
    }

    @Override
    public String indexes(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(tableName);
        return (
            "select i.index_name, i.uniqueness, i.status, ic.column_position, ic.column_name " +
            "from user_indexes i join user_ind_columns ic on i.index_name = ic.index_name and i.table_name = ic.table_name " +
            "where i.table_name = upper('" +
            escapedTable +
            "') order by i.index_name, ic.column_position"
        );
    }

    @Override
    public String constraints(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(tableName);
        return (
            "select c.constraint_name, case c.constraint_type when 'P' then 'PRIMARY KEY' when 'R' then 'FOREIGN KEY' when 'U' then 'UNIQUE' when 'C' then 'CHECK' else c.constraint_type end as constraint_type, " +
            "cc.column_name, c.status, c.search_condition from user_constraints c left join user_cons_columns cc on c.constraint_name = cc.constraint_name and c.table_name = cc.table_name " +
            "where c.table_name = upper('" +
            escapedTable +
            "') order by c.constraint_type, c.constraint_name, cc.position"
        );
    }

    @Override
    public String fkIn(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(tableName);
        return (
            "select fk.table_name, fk.constraint_name, fkc.column_name, rcc.column_name as references_column " +
            "from user_constraints refc join user_constraints fk on fk.r_constraint_name = refc.constraint_name " +
            "join user_cons_columns fkc on fk.constraint_name = fkc.constraint_name and fk.table_name = fkc.table_name " +
            "join user_cons_columns rcc on refc.constraint_name = rcc.constraint_name and fkc.position = rcc.position " +
            "where fk.constraint_type = 'R' and refc.table_name = upper('" +
            escapedTable +
            "') order by fk.table_name, fk.constraint_name, fkc.position"
        );
    }

    @Override
    public String fkOut(String tableName) {
        String escapedTable = MetadataProviderSupport.escapeSqlLiteral(tableName);
        return (
            "select fk.constraint_name, fkc.column_name, rcc.table_name as references_table, rcc.column_name as references_column " +
            "from user_constraints fk join user_cons_columns fkc on fk.constraint_name = fkc.constraint_name and fk.table_name = fkc.table_name " +
            "join user_constraints refc on fk.r_constraint_name = refc.constraint_name join user_cons_columns rcc on refc.constraint_name = rcc.constraint_name and fkc.position = rcc.position " +
            "where fk.constraint_type = 'R' and fk.table_name = upper('" +
            escapedTable +
            "') order by fk.constraint_name, fkc.position"
        );
    }

    @Override
    public String explain(String script) {
        return (
            "explain plan for " +
            MetadataProviderSupport.stripTrailingSemicolon(script) +
            "; select plan_table_output from table(dbms_xplan.display())"
        );
    }

    @Override
    public String count(String tableName) {
        return "select count(*) as TOTAL from " + MetadataProviderSupport.safeIdentifier(tableName);
    }

    private static String dataTypeExpression(String alias) {
        return (
            "case " +
            "when " +
            alias +
            ".data_type in ('CHAR','VARCHAR2','NCHAR','NVARCHAR2') then " +
            alias +
            ".data_type || '(' || " +
            alias +
            ".char_length || ')' " +
            "when " +
            alias +
            ".data_type = 'NUMBER' and " +
            alias +
            ".data_precision is not null and nvl(" +
            alias +
            ".data_scale, 0) = 0 then 'NUMBER(' || " +
            alias +
            ".data_precision || ')' " +
            "when " +
            alias +
            ".data_type = 'NUMBER' and " +
            alias +
            ".data_precision is not null then 'NUMBER(' || " +
            alias +
            ".data_precision || ',' || " +
            alias +
            ".data_scale || ')' " +
            "when " +
            alias +
            ".data_type = 'NUMBER' then 'NUMBER' else " +
            alias +
            ".data_type end"
        );
    }
}
