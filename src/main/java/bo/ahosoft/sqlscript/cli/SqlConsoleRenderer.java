package bo.ahosoft.sqlscript.cli;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SqlConsoleRenderer {

    private static final int MAX_CELL_WIDTH = 60;

    private SqlConsoleRenderer() {}

    public static String formatRows(List<String> headers, List<List<String>> rows) {
        List<Integer> widths = new ArrayList<>();
        for (String header : headers) {
            String value = truncate(header);
            widths.add(value.length());
        }

        List<String> normalizedHeaders = normalize(headers);
        List<List<String>> normalizedRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> normalizedRow = normalize(row);
            normalizedRows.add(normalizedRow);
            for (int i = 0; i < normalizedRow.size(); i++) {
                widths.set(i, Math.max(widths.get(i), normalizedRow.get(i).length()));
            }
        }

        StringBuilder table = new StringBuilder();
        appendSeparator(table, widths);
        appendRow(table, normalizedHeaders, widths);
        appendSeparator(table, widths);
        for (List<String> row : normalizedRows) {
            appendRow(table, row, widths);
        }
        appendSeparator(table, widths);
        table.append("Total rows: ").append(normalizedRows.size()).append(System.lineSeparator());
        return table.toString();
    }

    public static String formatFailure(SQLException exception) {
        StringBuilder output = new StringBuilder();
        output.append("Error executing SQL: ").append(exception.getMessage()).append(System.lineSeparator());
        if (exception.getSQLState() != null) {
            output.append("SQLState: ").append(exception.getSQLState()).append(System.lineSeparator());
        }
        output.append("Vendor code: ").append(exception.getErrorCode()).append(System.lineSeparator());
        return output.toString();
    }

    private static List<String> normalize(List<String> values) {
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            normalized.add(truncate(value));
        }
        return normalized;
    }

    private static void appendSeparator(StringBuilder table, List<Integer> widths) {
        table.append('+');
        for (int width : widths) {
            table.append(repeat('-', width + 2)).append('+');
        }
        table.append(System.lineSeparator());
    }

    private static void appendRow(StringBuilder table, List<String> values, List<Integer> widths) {
        table.append('|');
        for (int i = 0; i < values.size(); i++) {
            table.append(' ').append(padRight(values.get(i), widths.get(i))).append(' ').append('|');
        }
        table.append(System.lineSeparator());
    }

    private static String padRight(String value, int width) {
        return value + repeat(' ', Math.max(0, width - value.length()));
    }

    private static String truncate(String value) {
        if (value == null) {
            return "null";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= MAX_CELL_WIDTH ? normalized : normalized.substring(0, MAX_CELL_WIDTH - 3) + "...";
    }

    private static String repeat(char ch, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            value.append(ch);
        }
        return value.toString();
    }
}
