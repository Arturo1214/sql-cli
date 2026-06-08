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

public final class SqlRenderModel {

    private final List<SqlRenderLine> lines;
    private final List<CompletionCandidate> completionCandidates;
    private final List<SqlDiagnostic> diagnostics;
    private final int cursorOffset;

    public SqlRenderModel(List<SqlRenderLine> lines, int cursorOffset) {
        this(lines, cursorOffset, Collections.<CompletionCandidate>emptyList(), Collections.<SqlDiagnostic>emptyList());
    }

    public SqlRenderModel(List<SqlRenderLine> lines, int cursorOffset, List<CompletionCandidate> completionCandidates) {
        this(lines, cursorOffset, completionCandidates, Collections.<SqlDiagnostic>emptyList());
    }

    public SqlRenderModel(
        List<SqlRenderLine> lines,
        int cursorOffset,
        List<CompletionCandidate> completionCandidates,
        List<SqlDiagnostic> diagnostics
    ) {
        this.lines = Collections.unmodifiableList(new ArrayList<SqlRenderLine>(lines));
        this.cursorOffset = cursorOffset;
        this.completionCandidates = Collections.unmodifiableList(new ArrayList<CompletionCandidate>(completionCandidates));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<SqlDiagnostic>(diagnostics));
    }

    public List<SqlRenderLine> lines() {
        return lines;
    }

    public int cursorOffset() {
        return cursorOffset;
    }

    public List<CompletionCandidate> completionCandidates() {
        return completionCandidates;
    }

    public List<SqlDiagnostic> diagnostics() {
        return diagnostics;
    }
}
