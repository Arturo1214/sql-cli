package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SqlDiagnostics {

    private SqlDiagnostics() {}

    public static List<SqlDiagnostic> analyze(String sql, SqlDialect dialect) {
        String source = sql == null ? "" : sql;
        SqlDialect safeDialect = dialect == null ? SqlDialect.GENERIC : dialect;
        List<SqlDiagnostic> diagnostics = new ArrayList<SqlDiagnostic>();
        if (source.trim().isEmpty()) {
            return diagnostics;
        }
        detectMetadataCommandSuggestions(source, diagnostics);
        detectUnmatchedDelimiters(source, diagnostics);
        detectSelectMistakes(source, safeDialect, diagnostics);
        return diagnostics;
    }

    public static String normalizeForExecution(String sql) {
        return sql;
    }

    private static void detectMetadataCommandSuggestions(String source, List<SqlDiagnostic> diagnostics) {
        String[] parts = source.trim().split("\\s+", 2);
        String command = parts.length == 0 ? "" : parts[0].toLowerCase(Locale.ROOT);
        if ("table".equals(command) || "show".equals(command)) {
            diagnostics.add(info("Did you mean tables?", 0));
        } else if ("desc".equals(command) || "describee".equals(command)) {
            diagnostics.add(info("Did you mean describe <table>?", 0));
        } else if ("index".equals(command) || ("indexes".equals(command) && parts.length == 1)) {
            diagnostics.add(info("Did you mean indexes <table>?", 0));
        }
    }

    private static void detectUnmatchedDelimiters(String source, List<SqlDiagnostic> diagnostics) {
        boolean inString = false;
        int stringStart = 0;
        int parentheses = 0;
        int firstOpenParenthesis = -1;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (inString) {
                if (current == '\'' && i + 1 < source.length() && source.charAt(i + 1) == '\'') {
                    i++;
                } else if (current == '\'') {
                    inString = false;
                }
                continue;
            }
            if (current == '\'') {
                inString = true;
                stringStart = i;
            } else if (current == '(') {
                if (parentheses == 0) {
                    firstOpenParenthesis = i;
                }
                parentheses++;
            } else if (current == ')' && parentheses > 0) {
                parentheses--;
            } else if (current == ')' && parentheses == 0) {
                diagnostics.add(warning("Unmatched closing parenthesis", i));
            }
        }
        if (inString) {
            diagnostics.add(warning("Unmatched string quote", stringStart));
        }
        if (parentheses > 0) {
            diagnostics.add(warning("Unmatched opening parenthesis", firstOpenParenthesis));
        }
    }

    private static void detectSelectMistakes(String source, SqlDialect dialect, List<SqlDiagnostic> diagnostics) {
        List<SqlToken> tokens = significantTokens(source, dialect);
        if (tokens.isEmpty() || !"select".equalsIgnoreCase(tokens.get(0).text())) {
            return;
        }
        if (tokens.size() > 1 && "from".equalsIgnoreCase(tokens.get(1).text())) {
            diagnostics.add(warning("SELECT is missing a column list before FROM", tokens.get(1).startOffset()));
            return;
        }
        boolean hasFrom = false;
        for (SqlToken token : tokens) {
            if ("from".equalsIgnoreCase(token.text())) {
                hasFrom = true;
                break;
            }
        }
        if (!hasFrom && dialect == SqlDialect.ORACLE && looksLikeConstantSelect(tokens)) {
            diagnostics.add(info("Oracle constant SELECT may need FROM dual", tokens.get(0).startOffset()));
        }
    }

    private static boolean looksLikeConstantSelect(List<SqlToken> tokens) {
        for (int i = 1; i < tokens.size(); i++) {
            SqlTokenType type = tokens.get(i).type();
            if (type == SqlTokenType.NUMBER || type == SqlTokenType.STRING || type == SqlTokenType.LITERAL) {
                return true;
            }
        }
        return false;
    }

    private static List<SqlToken> significantTokens(String source, SqlDialect dialect) {
        List<SqlToken> tokens = new ArrayList<SqlToken>();
        for (SqlToken token : SqlTokenizer.tokenize(source, dialect)) {
            if (token.type() != SqlTokenType.WHITESPACE && token.type() != SqlTokenType.COMMENT) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static SqlDiagnostic info(String message, int offset) {
        return new SqlDiagnostic(SqlDiagnostic.Severity.INFO, message, offset);
    }

    private static SqlDiagnostic warning(String message, int offset) {
        return new SqlDiagnostic(SqlDiagnostic.Severity.WARNING, message, offset);
    }
}
