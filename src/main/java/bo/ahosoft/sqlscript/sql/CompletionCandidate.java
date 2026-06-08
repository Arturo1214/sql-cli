package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class CompletionCandidate {

    private final String value;
    private final String label;
    private final String kind;
    private final int replacementStart;
    private final int replacementEnd;

    public CompletionCandidate(String value, String label, String kind, int replacementStart, int replacementEnd) {
        this.value = value;
        this.label = label;
        this.kind = kind;
        this.replacementStart = replacementStart;
        this.replacementEnd = replacementEnd;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String kind() {
        return kind;
    }

    public int replacementStart() {
        return replacementStart;
    }

    public int replacementEnd() {
        return replacementEnd;
    }
}
