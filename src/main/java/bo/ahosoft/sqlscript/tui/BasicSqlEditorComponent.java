package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BasicSqlEditorComponent implements SqlEditorComponent {

    private final StringBuilder text;
    private final CompletionProvider completionProvider;
    private final MetadataCatalogSnapshot catalog;
    private final SqlDialect dialect;
    private int cursorOffset;

    public BasicSqlEditorComponent() {
        this("", 0);
    }

    public BasicSqlEditorComponent(String text, int cursorOffset) {
        this(text, cursorOffset, defaultCompletionProvider(), MetadataCatalogSnapshot.empty());
    }

    public BasicSqlEditorComponent(String text, int cursorOffset, CompletionProvider completionProvider, MetadataCatalogSnapshot catalog) {
        this(text, cursorOffset, completionProvider, catalog, SqlDialect.GENERIC);
    }

    public BasicSqlEditorComponent(
        String text,
        int cursorOffset,
        CompletionProvider completionProvider,
        MetadataCatalogSnapshot catalog,
        SqlDialect dialect
    ) {
        this.text = new StringBuilder(text == null ? "" : text);
        this.cursorOffset = clamp(cursorOffset, 0, this.text.length());
        this.completionProvider = completionProvider == null ? defaultCompletionProvider() : completionProvider;
        this.catalog = catalog == null ? MetadataCatalogSnapshot.empty() : catalog;
        this.dialect = dialect == null ? SqlDialect.GENERIC : dialect;
    }

    public String text() {
        return text.toString();
    }

    public int cursorOffset() {
        return cursorOffset;
    }

    public void insertText(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        text.insert(cursorOffset, value);
        cursorOffset += value.length();
    }

    public void moveCursor(int delta) {
        cursorOffset = clamp(cursorOffset + delta, 0, text.length());
    }

    public void deleteBeforeCursor() {
        if (cursorOffset == 0) {
            return;
        }
        text.deleteCharAt(cursorOffset - 1);
        cursorOffset--;
    }

    public String currentStatement() {
        return SqlStatementSelector.currentStatement(text(), cursorOffset);
    }

    public String executionReadinessMessage() {
        return text().trim().isEmpty() ? "Nothing is ready to execute" : "Ready";
    }

    public List<String> renderLines(int width, int height) {
        int safeWidth = Math.max(1, width);
        int maxLines = Math.max(1, height);
        String[] rawLines = text().split("\\n", -1);
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < rawLines.length && lines.size() < maxLines; i++) {
            String line = rawLines[i];
            lines.add(line.length() <= safeWidth ? line : line.substring(0, safeWidth));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    public List<String> renderLines(int width, int height, int verticalOffset, int horizontalOffset) {
        int safeWidth = Math.max(1, width);
        int maxLines = Math.max(1, height);
        String[] rawLines = text().split("\\n", -1);
        List<String> lines = new ArrayList<String>();
        int startLine = clamp(verticalOffset, 0, Math.max(0, rawLines.length - 1));
        for (int i = startLine; i < rawLines.length && lines.size() < maxLines; i++) {
            lines.add(horizontalClip(rawLines[i], Math.max(0, horizontalOffset), safeWidth));
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    public SqlRenderModel renderModel(int width, int height) {
        int safeWidth = Math.max(1, width);
        int maxLines = Math.max(1, height);
        String[] rawLines = text().split("\\n", -1);
        List<SqlRenderLine> lines = new ArrayList<SqlRenderLine>();
        for (int i = 0; i < rawLines.length && lines.size() < maxLines; i++) {
            String line = rawLines[i];
            String visibleText = line.length() <= safeWidth ? line : line.substring(0, safeWidth);
            lines.add(new SqlRenderLine(visibleText, spansForLine(visibleText)));
        }
        if (lines.isEmpty()) {
            lines.add(new SqlRenderLine("", Collections.<SqlRenderSpan>emptyList()));
        }
        return new SqlRenderModel(lines, cursorOffset(), completionCandidates(), diagnostics());
    }

    public SqlRenderModel renderModel(int width, int height, int verticalOffset, int horizontalOffset) {
        int safeWidth = Math.max(1, width);
        int maxLines = Math.max(1, height);
        String[] rawLines = text().split("\\n", -1);
        List<SqlRenderLine> lines = new ArrayList<SqlRenderLine>();
        int startLine = clamp(verticalOffset, 0, Math.max(0, rawLines.length - 1));
        int safeHorizontalOffset = Math.max(0, horizontalOffset);
        for (int i = startLine; i < rawLines.length && lines.size() < maxLines; i++) {
            String visibleText = horizontalClip(rawLines[i], safeHorizontalOffset, safeWidth);
            lines.add(new SqlRenderLine(visibleText, spansForLine(visibleText)));
        }
        if (lines.isEmpty()) {
            lines.add(new SqlRenderLine("", Collections.<SqlRenderSpan>emptyList()));
        }
        return new SqlRenderModel(lines, cursorOffset(), completionCandidates(), diagnostics());
    }

    private static String horizontalClip(String line, int horizontalOffset, int width) {
        String value = line == null ? "" : line;
        if (horizontalOffset >= value.length()) {
            return horizontalOffset == 0 ? "" : "←";
        }
        int to = Math.min(value.length(), horizontalOffset + width);
        String clipped = value.substring(horizontalOffset, to);
        boolean hasLeft = horizontalOffset > 0;
        boolean hasRight = to < value.length();
        if (!hasLeft && !hasRight) {
            return clipped;
        }
        StringBuilder marked = new StringBuilder(clipped);
        if (marked.length() > width) {
            marked.setLength(width);
        }
        if (hasLeft && marked.length() > 0) {
            marked.setCharAt(0, '←');
        }
        if (hasRight && marked.length() > 0) {
            marked.setCharAt(marked.length() - 1, '→');
        }
        return marked.toString();
    }

    public List<CompletionCandidate> completionCandidates() {
        return completionProvider.suggest(text(), cursorOffset(), catalog);
    }

    public List<String> decorations() {
        List<String> decorations = new ArrayList<String>();
        for (SqlDiagnostic diagnostic : diagnostics()) {
            decorations.add(diagnostic.message());
        }
        return decorations;
    }

    private List<SqlDiagnostic> diagnostics() {
        return SqlDiagnostics.analyze(text(), dialect);
    }

    private List<SqlRenderSpan> spansForLine(String line) {
        List<SqlRenderSpan> spans = new ArrayList<SqlRenderSpan>();
        List<SqlToken> tokens = SqlTokenizer.tokenize(line, dialect);
        for (SqlToken token : tokens) {
            SqlStyle style = styleFor(token.type());
            if (style != null) {
                spans.add(new SqlRenderSpan(token.startOffset(), token.endOffset() - token.startOffset(), style));
            }
        }
        return spans;
    }

    private static SqlStyle styleFor(SqlTokenType type) {
        if (type == SqlTokenType.KEYWORD) {
            return SqlStyle.KEYWORD;
        }
        if (type == SqlTokenType.IDENTIFIER) {
            return SqlStyle.IDENTIFIER;
        }
        if (type == SqlTokenType.LITERAL) {
            return SqlStyle.LITERAL;
        }
        if (type == SqlTokenType.STRING) {
            return SqlStyle.STRING;
        }
        if (type == SqlTokenType.NUMBER) {
            return SqlStyle.NUMBER;
        }
        if (type == SqlTokenType.COMMENT) {
            return SqlStyle.COMMENT;
        }
        if (type == SqlTokenType.PUNCTUATION) {
            return SqlStyle.PUNCTUATION;
        }
        if (type == SqlTokenType.OPERATOR) {
            return SqlStyle.OPERATOR;
        }
        return null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static CompletionProvider defaultCompletionProvider() {
        List<CompletionProvider> providers = new ArrayList<CompletionProvider>();
        providers.add(new SqlKeywordCompletionProvider());
        providers.add(new CatalogCompletionProvider());
        return new CompositeCompletionProvider(providers);
    }
}
