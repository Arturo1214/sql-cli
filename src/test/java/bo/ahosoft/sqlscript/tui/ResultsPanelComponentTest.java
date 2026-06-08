package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.SQLException;
import java.util.Arrays;
import org.junit.Test;

public class ResultsPanelComponentTest {

    @Test
    public void rendersSuccessfulConsoleTableWithFocusMarkerAndClipsToAvailableLines() {
        String table = SqlConsoleRenderer.formatRows(
            Arrays.asList("ID", "NAME"),
            Arrays.asList(Arrays.asList("100", "ALICE"), Arrays.asList("200", "BOB"))
        );
        ResultsPanelComponent component = ResultsPanelComponent.success(new SqlExecutionResult(table));

        ResultsPanelComponent.RenderedPanel rendered = component.render(4, WorkspaceFocus.RESULTS);

        assertTrue(rendered.lines().contains("Results *"));
        assertEquals(4, rendered.lines().size());
        assertTrue(rendered.lines().get(1).contains("+-----+-------+"));
        assertTrue(rendered.lines().get(2).contains("| ID  | NAME  |"));
        assertEquals("...", rendered.lines().get(3));
    }

    @Test
    public void rendersSqlErrorUsingConsoleFailureTextWithoutFocusMarker() {
        ResultsPanelComponent component = ResultsPanelComponent.failure(new SQLException("invalid table", "42000", 942));

        ResultsPanelComponent.RenderedPanel rendered = component.render(5, WorkspaceFocus.EDITOR);

        assertTrue(rendered.lines().contains("Results"));
        assertFalse(rendered.lines().contains("Results *"));
        assertTrue(rendered.lines().contains("Error executing SQL: invalid table"));
        assertTrue(rendered.lines().contains("SQLState: 42000"));
        assertTrue(rendered.lines().contains("Vendor code: 942"));
    }

    @Test
    public void rendersEmptyStateWhenNoResultHasBeenProduced() {
        ResultsPanelComponent component = ResultsPanelComponent.empty();

        ResultsPanelComponent.RenderedPanel rendered = component.render(3, WorkspaceFocus.CONNECTIONS);

        assertEquals(Arrays.asList("Results", "No query has been executed yet"), rendered.lines());
    }

    @Test
    public void rendersScrolledResultViewportWithinCurrentPage() {
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            content.append("row-").append(i).append(System.lineSeparator());
        }
        ResultsPanelComponent component = ResultsPanelComponent.success(new SqlExecutionResult(content.toString()));

        ResultsPanelComponent.RenderedPanel rendered = component.render(5, WorkspaceFocus.RESULTS, 6);

        assertEquals("Results *", rendered.lines().get(0));
        assertEquals("row-7", rendered.lines().get(1));
        assertEquals("row-10", rendered.lines().get(4));
        assertFalse(rendered.lines().contains("row-1"));
    }

    @Test
    public void rendersAtMostOneHundredDataRowsFromPagedExecutionResult() {
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1: select id from users", Arrays.asList("ID"), oneHundredAndFiveRows());

        ResultsPanelComponent.RenderedPanel rendered = ResultsPanelComponent.success(result).render(130, WorkspaceFocus.RESULTS);

        String text = rendered.lines().toString();
        assertTrue(text.contains("Page 1/2 | Rows 1-100 | Next: PageDown | Previous: PageUp"));
        assertTrue(text.contains("100"));
        assertFalse(text.contains("101"));
    }

    @Test
    public void rendersLeadingAbsoluteRowNumberColumnOnFirstAndSecondPages() {
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1: select id from users", Arrays.asList("ID"), oneHundredAndFiveRows());

        String firstPage = ResultsPanelComponent.success(result).render(130, WorkspaceFocus.RESULTS, 0, 0, 120).lines().toString();
        String secondPage = ResultsPanelComponent.success(result.nextPage())
            .render(20, WorkspaceFocus.RESULTS, 0, 0, 120)
            .lines()
            .toString();

        assertTrue(firstPage.contains("| #   | ID  |"));
        assertTrue(firstPage.contains("| 1   | 1   |"));
        assertTrue(firstPage.contains("| 100 | 100 |"));
        assertTrue(secondPage.contains("| 101 | 101 |"));
        assertTrue(secondPage.contains("| 105 | 105 |"));
    }

    @Test
    public void clipsWideResultRowsToHorizontalViewportWithoutWrapping() {
        SqlExecutionResult result = wideResult();

        ResultsPanelComponent.RenderedPanel rendered = ResultsPanelComponent.success(result).render(10, WorkspaceFocus.RESULTS, 0, 0, 40);

        for (String line : rendered.lines()) {
            assertTrue("line should fit viewport: " + line, line.length() <= 40);
        }
        assertTrue(rendered.lines().toString().contains("→"));
        assertFalse(rendered.lines().toString().contains("COL_07"));
    }

    @Test
    public void horizontalOffsetChangesVisibleResultContent() {
        SqlExecutionResult result = wideResult();

        String left = ResultsPanelComponent.success(result).render(10, WorkspaceFocus.RESULTS, 0, 0, 50).lines().toString();
        String right = ResultsPanelComponent.success(result).render(10, WorkspaceFocus.RESULTS, 0, 70, 50).lines().toString();

        assertTrue(left.contains("COL_01"));
        assertFalse(left.contains("COL_08"));
        assertFalse(right.contains("COL_01"));
        assertTrue(right.contains("COL_08"));
        assertTrue(right.contains("←"));
    }

    private static java.util.List<java.util.List<String>> oneHundredAndFiveRows() {
        java.util.List<java.util.List<String>> rows = new java.util.ArrayList<java.util.List<String>>();
        for (int i = 1; i <= 105; i++) {
            rows.add(Arrays.asList(String.valueOf(i)));
        }
        return rows;
    }

    private static SqlExecutionResult wideResult() {
        java.util.List<String> headers = new java.util.ArrayList<String>();
        java.util.List<String> row = new java.util.ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            headers.add(String.format("COL_%02d", Integer.valueOf(i)));
            row.add(String.format("VALUE_%02d", Integer.valueOf(i)));
        }
        return SqlExecutionResult.paged("SQL #1: select * from wide_table", headers, Arrays.asList(row));
    }
}
