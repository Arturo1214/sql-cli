package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResultsPanelComponent {

    private final String content;
    private final SqlExecutionResult result;

    private ResultsPanelComponent(String content, SqlExecutionResult result) {
        this.content = content == null ? "" : content;
        this.result = result;
    }

    public static ResultsPanelComponent empty() {
        return new ResultsPanelComponent("", null);
    }

    public static ResultsPanelComponent success(SqlExecutionResult result) {
        return new ResultsPanelComponent(result == null ? "" : result.consoleTable(), result);
    }

    public static ResultsPanelComponent failure(SQLException exception) {
        return new ResultsPanelComponent(SqlConsoleRenderer.formatFailure(exception), null);
    }

    public RenderedPanel render(int maxLines, WorkspaceFocus focus) {
        return render(maxLines, focus, 0);
    }

    public RenderedPanel render(int maxLines, WorkspaceFocus focus, int scrollOffset) {
        return render(maxLines, focus, scrollOffset, 0, 0);
    }

    public RenderedPanel render(int maxLines, WorkspaceFocus focus, int scrollOffset, int horizontalOffset, int maxColumns) {
        return render(maxLines, focus, scrollOffset, horizontalOffset, maxColumns, new TuiMessages(TuiLanguage.ENGLISH));
    }

    public RenderedPanel render(
        int maxLines,
        WorkspaceFocus focus,
        int scrollOffset,
        int horizontalOffset,
        int maxColumns,
        TuiMessages messages
    ) {
        List<String> lines = new ArrayList<>();
        int safeHorizontalOffset = Math.max(0, horizontalOffset);
        int safeMaxColumns = Math.max(0, maxColumns);
        TuiMessages safeMessages = messages == null ? new TuiMessages(TuiLanguage.ENGLISH) : messages;
        lines.add(title(focus, safeHorizontalOffset, safeMaxColumns, safeMessages));
        String source = result != null && result.hasTabularRows() ? result.consoleTableWithRowNumbers(safeMessages) : content;
        if (source.trim().isEmpty()) {
            lines.add(safeMessages.noQueryExecutedYet());
            return new RenderedPanel(limit(lines, maxLines));
        }

        List<String> contentLines = new ArrayList<String>();
        for (String line : source.split("\\r?\\n")) {
            if (!line.isEmpty()) {
                contentLines.add(horizontalClip(line, safeHorizontalOffset, safeMaxColumns));
            }
        }
        int visibleContentLines = maxLines <= 0 ? contentLines.size() : Math.max(0, maxLines - 1);
        if (result != null && result.hasTabularRows() && scrollOffset == 0 && maxLines > 0) {
            List<String> tabularLines = compactTabularViewport(contentLines, visibleContentLines, safeMessages);
            if (tabularLines != null) {
                lines.addAll(tabularLines);
                return new RenderedPanel(limit(lines, maxLines));
            }
        }
        int offset = Math.max(0, Math.min(scrollOffset, Math.max(0, contentLines.size() - visibleContentLines)));
        boolean truncated = visibleContentLines > 0 && offset + visibleContentLines < contentLines.size();
        int contentLimit = truncated && offset == 0 ? Math.max(0, visibleContentLines - 1) : visibleContentLines;
        int to = contentLimit <= 0 ? offset : Math.min(contentLines.size(), offset + contentLimit);
        lines.addAll(contentLines.subList(offset, to));
        if (offset == 0 && visibleContentLines > 0 && to < contentLines.size()) {
            lines.add("...");
        }
        return new RenderedPanel(limit(lines, maxLines));
    }

    private static List<String> compactTabularViewport(List<String> contentLines, int visibleContentLines, TuiMessages messages) {
        if (contentLines.size() <= visibleContentLines) {
            return null;
        }
        int headerIndex = tableHeaderIndex(contentLines);
        int firstDataRowIndex = firstDataRowIndex(contentLines, headerIndex);
        if (headerIndex < 0 || firstDataRowIndex < 0 || firstDataRowIndex < visibleContentLines - 1) {
            return null;
        }
        if (visibleContentLines < 3) {
            return Collections.singletonList(messages.resultRowsNeedMoreHeight());
        }
        List<String> compact = new ArrayList<String>();
        int statusIndex = pageStatusIndex(contentLines);
        if (statusIndex >= 0) {
            compact.add(contentLines.get(statusIndex));
        }
        compact.add(contentLines.get(headerIndex));
        int rowIndex = firstDataRowIndex;
        while (rowIndex < contentLines.size() && compact.size() < visibleContentLines - 1) {
            String line = contentLines.get(rowIndex);
            if (!isTableDataRow(line)) {
                break;
            }
            compact.add(line);
            rowIndex++;
        }
        if (rowIndex < contentLines.size()) {
            compact.add("...");
        }
        return compact;
    }

    private static int pageStatusIndex(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Page ") || line.startsWith("Pagina ")) {
                return i;
            }
        }
        return -1;
    }

    private static int tableHeaderIndex(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if (isTableSeparator(lines.get(i - 1)) && isTableDataRow(lines.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int firstDataRowIndex(List<String> lines, int headerIndex) {
        if (headerIndex < 0) {
            return -1;
        }
        for (int i = headerIndex + 1; i < lines.size(); i++) {
            if (isTableSeparator(lines.get(i))) {
                for (int row = i + 1; row < lines.size(); row++) {
                    if (isTableDataRow(lines.get(row))) {
                        return row;
                    }
                    if (isTableSeparator(lines.get(row))) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean isTableSeparator(String line) {
        return line != null && line.startsWith("+");
    }

    private static boolean isTableDataRow(String line) {
        return line != null && line.startsWith("|");
    }

    private static String title(WorkspaceFocus focus, int horizontalOffset, int maxColumns, TuiMessages messages) {
        String title = maxColumns > 0 ? messages.resultsTitle(focus, horizontalOffset) : messages.resultsTitle(focus);
        return horizontalClip(title, 0, maxColumns);
    }

    private static String horizontalClip(String line, int horizontalOffset, int maxColumns) {
        if (maxColumns <= 0) {
            return line;
        }
        String safeLine = line == null ? "" : line;
        if (horizontalOffset >= safeLine.length()) {
            return horizontalOffset == 0 ? "" : "←";
        }
        int to = Math.min(safeLine.length(), horizontalOffset + maxColumns);
        String clipped = safeLine.substring(horizontalOffset, to);
        boolean hasLeft = horizontalOffset > 0;
        boolean hasRight = to < safeLine.length();
        if (!hasLeft && !hasRight) {
            return clipped;
        }
        StringBuilder marked = new StringBuilder(clipped);
        if (hasLeft && marked.length() > 0) {
            marked.setCharAt(0, '←');
        }
        if (hasRight && marked.length() > 0) {
            marked.setCharAt(marked.length() - 1, '→');
        }
        return marked.toString();
    }

    private static List<String> limit(List<String> lines, int maxLines) {
        if (maxLines <= 0 || lines.size() <= maxLines) {
            return lines;
        }
        List<String> clipped = new ArrayList<>(lines.subList(0, maxLines));
        clipped.set(maxLines - 1, "...");
        return clipped;
    }

    public static final class RenderedPanel {

        private final List<String> lines;

        public RenderedPanel(List<String> lines) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }

        List<String> lines() {
            return lines;
        }
    }
}
