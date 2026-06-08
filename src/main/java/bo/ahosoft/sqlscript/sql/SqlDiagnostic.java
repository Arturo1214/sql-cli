package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class SqlDiagnostic {

    public enum Severity {
        INFO,
        WARNING,
    }

    private final Severity severity;
    private final String message;
    private final int offset;

    public SqlDiagnostic(Severity severity, String message, int offset) {
        this.severity = severity == null ? Severity.INFO : severity;
        this.message = message == null ? "" : message;
        this.offset = Math.max(0, offset);
    }

    public Severity severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public int offset() {
        return offset;
    }
}
