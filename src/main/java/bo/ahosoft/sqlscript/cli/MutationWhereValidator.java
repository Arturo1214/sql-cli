package bo.ahosoft.sqlscript.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MutationWhereValidator {

    private MutationWhereValidator() {}

    public static void requireTopLevelWhereForMutations(String script) {
        List<String> statements = splitStatements(script);
        for (int i = 0; i < statements.size(); i++) {
            String statement = statements.get(i);
            if (isUpdateOrDelete(statement) && !hasTopLevelWhere(statement)) {
                throw new IllegalArgumentException(SafetyGuard.MISSING_WHERE_MESSAGE + " (statement " + (i + 1) + ")");
            }
        }
    }

    public static boolean hasTopLevelWhere(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            return false;
        }
        ScannerState state = new ScannerState();
        int length = statement.length();
        for (int i = 0; i < length; i++) {
            char current = statement.charAt(i);
            char next = i + 1 < length ? statement.charAt(i + 1) : '\0';

            if (state.lineComment) {
                if (current == '\n' || current == '\r') {
                    state.lineComment = false;
                }
                continue;
            }
            if (state.blockComment) {
                if (current == '*' && next == '/') {
                    state.blockComment = false;
                    i++;
                }
                continue;
            }
            if (state.singleQuote) {
                if (current == '\'' && next == '\'') {
                    i++;
                } else if (current == '\'') {
                    state.singleQuote = false;
                }
                continue;
            }
            if (state.doubleQuote) {
                if (current == '"' && next == '"') {
                    i++;
                } else if (current == '"') {
                    state.doubleQuote = false;
                }
                continue;
            }

            if (current == '-' && next == '-') {
                state.lineComment = true;
                i++;
                continue;
            }
            if (current == '/' && next == '*') {
                state.blockComment = true;
                i++;
                continue;
            }
            if (current == '\'') {
                state.singleQuote = true;
                continue;
            }
            if (current == '"') {
                state.doubleQuote = true;
                continue;
            }
            if (current == '(') {
                state.depth++;
                continue;
            }
            if (current == ')') {
                if (state.depth > 0) {
                    state.depth--;
                }
                continue;
            }
            if (state.depth == 0 && isIdentifierStart(current)) {
                int end = readIdentifierEnd(statement, i + 1);
                if ("where".equals(statement.substring(i, end).toLowerCase(Locale.ROOT))) {
                    return true;
                }
                i = end - 1;
            }
        }
        return false;
    }

    public static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<String>();
        if (script == null || script.trim().isEmpty()) {
            return statements;
        }
        ScannerState state = new ScannerState();
        StringBuilder currentStatement = new StringBuilder();
        int length = script.length();
        for (int i = 0; i < length; i++) {
            char current = script.charAt(i);
            char next = i + 1 < length ? script.charAt(i + 1) : '\0';

            if (state.lineComment) {
                if (current == '\n' || current == '\r') {
                    state.lineComment = false;
                    currentStatement.append(' ');
                }
                continue;
            }
            if (state.blockComment) {
                if (current == '*' && next == '/') {
                    state.blockComment = false;
                    i++;
                    currentStatement.append(' ');
                }
                continue;
            }
            currentStatement.append(current);
            if (state.singleQuote) {
                if (current == '\'' && next == '\'') {
                    currentStatement.append(next);
                    i++;
                } else if (current == '\'') {
                    state.singleQuote = false;
                }
                continue;
            }
            if (state.doubleQuote) {
                if (current == '"' && next == '"') {
                    currentStatement.append(next);
                    i++;
                } else if (current == '"') {
                    state.doubleQuote = false;
                }
                continue;
            }

            if (current == '-' && next == '-') {
                currentStatement.setLength(currentStatement.length() - 1);
                state.lineComment = true;
                i++;
                continue;
            }
            if (current == '/' && next == '*') {
                currentStatement.setLength(currentStatement.length() - 1);
                state.blockComment = true;
                i++;
                continue;
            }
            if (current == '\'') {
                state.singleQuote = true;
                continue;
            }
            if (current == '"') {
                state.doubleQuote = true;
                continue;
            }
            if (current == ';') {
                appendStatement(statements, currentStatement.substring(0, currentStatement.length() - 1));
                currentStatement.setLength(0);
            }
        }
        appendStatement(statements, currentStatement.toString());
        return statements;
    }

    private static void appendStatement(List<String> statements, String statement) {
        String trimmed = statement == null ? "" : statement.trim();
        if (!trimmed.isEmpty()) {
            statements.add(trimmed);
        }
    }

    private static boolean isUpdateOrDelete(String statement) {
        String firstToken = firstToken(statement);
        return "update".equals(firstToken) || "delete".equals(firstToken);
    }

    private static String firstToken(String statement) {
        if (statement == null) {
            return "";
        }
        int i = 0;
        while (i < statement.length()) {
            char current = statement.charAt(i);
            char next = i + 1 < statement.length() ? statement.charAt(i + 1) : '\0';
            if (Character.isWhitespace(current)) {
                i++;
                continue;
            }
            if (current == '-' && next == '-') {
                i += 2;
                while (i < statement.length() && statement.charAt(i) != '\n' && statement.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }
            if (current == '/' && next == '*') {
                i += 2;
                while (i + 1 < statement.length() && !(statement.charAt(i) == '*' && statement.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(statement.length(), i + 2);
                continue;
            }
            if (isIdentifierStart(current)) {
                int end = readIdentifierEnd(statement, i + 1);
                return statement.substring(i, end).toLowerCase(Locale.ROOT);
            }
            return "";
        }
        return "";
    }

    private static int readIdentifierEnd(String value, int index) {
        int end = index;
        while (end < value.length() && isIdentifierPart(value.charAt(end))) {
            end++;
        }
        return end;
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '$' || value == '#';
    }

    private static boolean isIdentifierPart(char value) {
        return isIdentifierStart(value) || Character.isDigit(value);
    }

    private static final class ScannerState {

        private boolean lineComment;
        private boolean blockComment;
        private boolean singleQuote;
        private boolean doubleQuote;
        private int depth;
    }
}
