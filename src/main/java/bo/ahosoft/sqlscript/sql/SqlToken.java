package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class SqlToken {

    private final SqlTokenType type;
    private final String text;
    private final int startOffset;
    private final int endOffset;

    public SqlToken(SqlTokenType type, String text, int startOffset, int endOffset) {
        this.type = type;
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public SqlTokenType type() {
        return type;
    }

    public String text() {
        return text;
    }

    public int startOffset() {
        return startOffset;
    }

    public int endOffset() {
        return endOffset;
    }
}
