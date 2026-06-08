package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class SqlRenderSpan {

    private final int startColumn;
    private final int length;
    private final SqlStyle style;

    public SqlRenderSpan(int startColumn, int length, SqlStyle style) {
        this.startColumn = startColumn;
        this.length = length;
        this.style = style;
    }

    public int startColumn() {
        return startColumn;
    }

    public int length() {
        return length;
    }

    public SqlStyle style() {
        return style;
    }
}
