package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class SqlStatementSelector {

    private SqlStatementSelector() {}

    public static String currentStatement(String buffer, int cursorOffset) {
        if (buffer == null || buffer.trim().isEmpty()) {
            throw new IllegalArgumentException("buffer is required");
        }

        int cursor = clamp(cursorOffset, 0, buffer.length());
        int statementStart = 0;
        int selectedStart = -1;
        int selectedEnd = -1;
        boolean inSingleQuote = false;

        for (int i = 0; i < buffer.length(); i++) {
            char ch = buffer.charAt(i);
            if (ch == '\'') {
                if (inSingleQuote && i + 1 < buffer.length() && buffer.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (ch == ';' && !inSingleQuote) {
                if (cursor >= statementStart && cursor <= i + 1) {
                    selectedStart = statementStart;
                    selectedEnd = i;
                    break;
                }
                statementStart = i + 1;
            }
        }

        if (selectedStart < 0 && cursor >= statementStart) {
            selectedStart = statementStart;
            selectedEnd = buffer.length();
        }

        String statement = trimStatement(buffer, selectedStart, selectedEnd);
        if (!statement.isEmpty()) {
            return statement;
        }

        return previousStatement(buffer, selectedStart);
    }

    private static String previousStatement(String buffer, int beforeOffset) {
        int end = Math.max(0, beforeOffset - 1);
        while (end > 0 && Character.isWhitespace(buffer.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && buffer.charAt(end - 1) == ';') {
            end--;
        }

        int start = 0;
        boolean inSingleQuote = false;
        for (int i = 0; i < end; i++) {
            char ch = buffer.charAt(i);
            if (ch == '\'') {
                if (inSingleQuote && i + 1 < end && buffer.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            } else if (ch == ';' && !inSingleQuote) {
                start = i + 1;
            }
        }
        return trimStatement(buffer, start, end);
    }

    private static String trimStatement(String buffer, int start, int end) {
        if (start < 0 || end < start) {
            return "";
        }
        String statement = buffer.substring(start, end).trim();
        while (statement.endsWith(";")) {
            statement = statement.substring(0, statement.length() - 1).trim();
        }
        return statement;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
