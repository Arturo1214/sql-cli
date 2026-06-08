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
        int offset = Math.max(0, Math.min(scrollOffset, Math.max(0, contentLines.size() - visibleContentLines)));
        int to = visibleContentLines <= 0 ? offset : Math.min(contentLines.size(), offset + visibleContentLines);
        lines.addAll(contentLines.subList(offset, to));
        if (offset == 0 && visibleContentLines > 0 && to < contentLines.size() && lines.size() == maxLines) {
            lines.set(lines.size() - 1, "...");
        }
        return new RenderedPanel(limit(lines, maxLines));
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
