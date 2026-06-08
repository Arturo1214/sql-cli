package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlRenderLine {

    private final String text;
    private final List<SqlRenderSpan> spans;

    public SqlRenderLine(String text, List<SqlRenderSpan> spans) {
        this.text = text == null ? "" : text;
        this.spans = Collections.unmodifiableList(new ArrayList<SqlRenderSpan>(spans));
    }

    public String text() {
        return text;
    }

    public List<SqlRenderSpan> spans() {
        return spans;
    }
}
