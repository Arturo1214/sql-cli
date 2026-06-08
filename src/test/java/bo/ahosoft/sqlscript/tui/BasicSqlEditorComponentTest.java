package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class BasicSqlEditorComponentTest {

    @Test
    public void preservesBufferAndCursorAcrossRendering() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent();

        editor.insertText("select * from users");
        editor.moveCursor(-11);

        assertEquals("select * from users", editor.text());
        assertEquals("select * from users", editor.renderLines(80, 5).get(0));
        assertEquals(8, editor.cursorOffset());
    }

    @Test
    public void editsTextWithCursorMovementAndDeletion() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select from users", 7);

        editor.insertText("* ");
        editor.moveCursor(editor.text().length());
        editor.deleteBeforeCursor();

        assertEquals("select * from user", editor.text());
        assertEquals(editor.text().length(), editor.cursorOffset());
    }

    @Test
    public void insertTextPreservesPastedMultilineScript() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent();
        String script = "select *\n  from users\n where status = 'ACTIVE';\n\nselect ? from dual;";

        editor.insertText(script);

        assertEquals(script, editor.text());
        assertEquals(script.length(), editor.cursorOffset());
        assertEquals(
            Arrays.asList("select *", "  from users", " where status = 'ACTIVE';", "", "select ? from dual;"),
            editor.renderLines(80, 10)
        );
    }

    @Test
    public void selectsCurrentStatementAtCursor() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from users;\nselect * from orders;", 28);

        assertEquals("select * from orders", editor.currentStatement());
    }

    @Test
    public void selectsCurrentStatementAfterMultilinePasteByCursorOffset() {
        String script = "select *\n  from users;\n\nselect *\n  from orders;";
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent();
        editor.insertText(script);
        editor.moveCursor(-8);

        assertEquals("select *\n  from orders", editor.currentStatement());
    }

    @Test
    public void rejectsEmptyExecutionAndExposesEmptyDecorations() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("   ", 0);

        assertEquals("Nothing is ready to execute", editor.executionReadinessMessage());
        assertTrue(editor.decorations().isEmpty());
    }

    @Test
    public void renderModelHighlightsSqlTokensWithLineRelativeSpans() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select 'Ada' from users", 6);

        SqlRenderModel model = editor.renderModel(80, 5);

        assertEquals(6, model.cursorOffset());
        assertEquals(1, model.lines().size());
        assertEquals("select 'Ada' from users", model.lines().get(0).text());
        List<SqlRenderSpan> spans = model.lines().get(0).spans();
        assertEquals(4, spans.size());
        assertSpan(spans.get(0), 0, 6, SqlStyle.KEYWORD);
        assertSpan(spans.get(1), 7, 5, SqlStyle.STRING);
        assertSpan(spans.get(2), 13, 4, SqlStyle.KEYWORD);
        assertSpan(spans.get(3), 18, 5, SqlStyle.IDENTIFIER);
    }

    @Test
    public void renderModelPreservesPlainFallbackWhenVisibleTextIsTruncated() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from users", 18);

        SqlRenderModel model = editor.renderModel(6, 1);

        assertEquals("select", model.lines().get(0).text());
        assertEquals("select", editor.renderLines(6, 1).get(0));
        assertEquals(1, model.lines().get(0).spans().size());
        assertSpan(model.lines().get(0).spans().get(0), 0, 6, SqlStyle.KEYWORD);
    }

    @Test
    public void exposesRequestedAutocompleteCandidatesWithReplacementRange() {
        CompletionProvider provider = new CompositeCompletionProvider(
            Arrays.<CompletionProvider>asList(new SqlKeywordCompletionProvider(), new CatalogCompletionProvider())
        );
        MetadataCatalogSnapshot catalog = MetadataCatalogSnapshot.ofTables(Arrays.asList("users", "user_audit"));
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from us", 16, provider, catalog);

        List<CompletionCandidate> candidates = editor.completionCandidates();

        assertEquals(2, candidates.size());
        assertCandidate(candidates.get(0), "user_audit", "table", 14, 16);
        assertCandidate(candidates.get(1), "users", "table", 14, 16);
        assertEquals("select * from us", editor.text());
    }

    @Test
    public void renderLinesSupportsVerticalAndHorizontalViewportWithoutWrapping() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent(
            "select short_line\nselect this_is_a_very_long_column_name from this_is_a_very_long_table_name where status = 'ACTIVE'\nselect after_scroll",
            0
        );

        List<String> topLeft = editor.renderLines(20, 2, 0, 0);
        List<String> scrolled = editor.renderLines(20, 2, 1, 35);

        assertEquals(Arrays.asList("select short_line", "select this_is_a_ve→"), topLeft);
        assertEquals(2, scrolled.size());
        assertTrue(scrolled.get(0).startsWith("←"));
        assertTrue(scrolled.get(0).contains("this_is"));
        assertEquals("←", scrolled.get(1));
        for (String line : scrolled) {
            assertTrue("line should fit editor viewport: " + line, line.length() <= 20);
        }
    }

    @Test
    public void renderModelUsesSameViewportClippingForHighlightedLines() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from very_long_table_name", 0);

        SqlRenderModel model = editor.renderModel(12, 1, 0, 10);

        assertEquals(1, model.lines().size());
        assertEquals("←om very_lo→", model.lines().get(0).text());
        for (SqlRenderSpan span : model.lines().get(0).spans()) {
            assertTrue(span.startColumn() >= 0);
            assertTrue(span.startColumn() + span.length() <= model.lines().get(0).text().length());
        }
    }

    @Test
    public void renderModelExposesDialectHighlightingAndDiagnosticsDecorations() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent(
            "select jsonb 'x' from events where id = 42",
            6,
            null,
            null,
            SqlDialect.POSTGRESQL
        );

        SqlRenderModel model = editor.renderModel(80, 5);

        assertSpan(model.lines().get(0).spans().get(0), 0, 6, SqlStyle.KEYWORD);
        assertSpan(model.lines().get(0).spans().get(1), 7, 5, SqlStyle.KEYWORD);
        assertSpan(model.lines().get(0).spans().get(2), 13, 3, SqlStyle.STRING);
        assertSpan(model.lines().get(0).spans().get(8), 40, 2, SqlStyle.NUMBER);
        assertTrue(model.diagnostics().isEmpty());
        assertTrue(editor.decorations().isEmpty());
    }

    @Test
    public void decorationsExposeNonIntrusiveEditorDiagnostics() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select from users", 0, null, null, SqlDialect.ORACLE);

        List<String> decorations = editor.decorations();

        assertEquals(1, decorations.size());
        assertTrue(decorations.get(0).contains("SELECT is missing a column list before FROM"));
    }

    @Test
    public void defaultsToKeywordAutocompleteWhenNoCatalogIsInjected() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("sel", 3);

        List<CompletionCandidate> candidates = editor.completionCandidates();

        assertEquals(1, candidates.size());
        assertCandidate(candidates.get(0), "SELECT", "keyword", 0, 3);
    }

    private static void assertSpan(SqlRenderSpan span, int startColumn, int length, SqlStyle style) {
        assertEquals(startColumn, span.startColumn());
        assertEquals(length, span.length());
        assertEquals(style, span.style());
    }

    private static void assertCandidate(
        CompletionCandidate candidate,
        String value,
        String kind,
        int replacementStart,
        int replacementEnd
    ) {
        assertEquals(value, candidate.value());
        assertEquals(value, candidate.label());
        assertEquals(kind, candidate.kind());
        assertEquals(replacementStart, candidate.replacementStart());
        assertEquals(replacementEnd, candidate.replacementEnd());
    }
}
