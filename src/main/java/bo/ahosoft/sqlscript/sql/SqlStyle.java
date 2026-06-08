package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public enum SqlStyle {
    KEYWORD,
    IDENTIFIER,
    LITERAL,
    STRING,
    NUMBER,
    COMMENT,
    PUNCTUATION,
    OPERATOR,
}
