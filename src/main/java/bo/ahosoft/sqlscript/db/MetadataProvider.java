package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public interface MetadataProvider {
    String tables(String filter);

    String search(String text);

    String sample(String tableName, int limit);

    String describe(String tableName);

    String details(String tableName);

    String indexes(String tableName);

    String constraints(String tableName);

    String fkIn(String tableName);

    String fkOut(String tableName);

    String explain(String script);

    String count(String tableName);
}
