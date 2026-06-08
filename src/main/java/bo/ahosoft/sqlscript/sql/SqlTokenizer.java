package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.List;

public final class SqlTokenizer {

    private static final String PUNCTUATION = "*(),.;=+-/<>";

    private SqlTokenizer() {}

    public static List<SqlToken> tokenize(String sql) {
        return tokenize(sql, SqlDialect.GENERIC, false);
    }

    public static List<SqlToken> tokenize(String sql, SqlDialect dialect) {
        return tokenize(sql, dialect, true);
    }

    private static List<SqlToken> tokenize(String sql, SqlDialect dialect, boolean richTokens) {
        String source = sql == null ? "" : sql;
        SqlDialect safeDialect = dialect == null ? SqlDialect.GENERIC : dialect;
        List<SqlToken> tokens = new ArrayList<SqlToken>();
        int offset = 0;
        while (offset < source.length()) {
            char current = source.charAt(offset);
            if (Character.isWhitespace(current)) {
                offset = addWhile(
                    source,
                    offset,
                    tokens,
                    SqlTokenType.WHITESPACE,
                    new CharacterMatcher() {
                        public boolean matches(char value) {
                            return Character.isWhitespace(value);
                        }
                    }
                );
            } else if (startsLineComment(source, offset)) {
                offset = addUntilLineBreak(source, offset, tokens, SqlTokenType.COMMENT);
            } else if (current == '\'') {
                offset = addQuoted(source, offset, tokens, richTokens ? SqlTokenType.STRING : SqlTokenType.LITERAL, '\'');
            } else if (current == '"') {
                offset = addQuoted(source, offset, tokens, SqlTokenType.IDENTIFIER, '"');
            } else if (Character.isDigit(current)) {
                offset = addWhile(
                    source,
                    offset,
                    tokens,
                    richTokens ? SqlTokenType.NUMBER : SqlTokenType.LITERAL,
                    new CharacterMatcher() {
                        public boolean matches(char value) {
                            return Character.isDigit(value) || value == '.';
                        }
                    }
                );
            } else if (isIdentifierStart(current)) {
                offset = addIdentifierOrKeyword(source, offset, tokens, safeDialect);
            } else if (PUNCTUATION.indexOf(current) >= 0) {
                tokens.add(
                    new SqlToken(
                        richTokens ? SqlTokenType.OPERATOR : SqlTokenType.PUNCTUATION,
                        source.substring(offset, offset + 1),
                        offset,
                        offset + 1
                    )
                );
                offset++;
            } else {
                tokens.add(new SqlToken(SqlTokenType.UNKNOWN, source.substring(offset, offset + 1), offset, offset + 1));
                offset++;
            }
        }
        return tokens;
    }

    private static int addIdentifierOrKeyword(String source, int start, List<SqlToken> tokens, SqlDialect dialect) {
        int end = start + 1;
        while (end < source.length() && isIdentifierPart(source.charAt(end))) {
            end++;
        }
        String text = source.substring(start, end);
        SqlTokenType type = dialect.isKeyword(text) ? SqlTokenType.KEYWORD : SqlTokenType.IDENTIFIER;
        tokens.add(new SqlToken(type, text, start, end));
        return end;
    }

    private static int addQuoted(String source, int start, List<SqlToken> tokens, SqlTokenType type, char quote) {
        int end = start + 1;
        while (end < source.length()) {
            char current = source.charAt(end);
            end++;
            if (current == quote) {
                break;
            }
        }
        tokens.add(new SqlToken(type, source.substring(start, end), start, end));
        return end;
    }

    private static int addUntilLineBreak(String source, int start, List<SqlToken> tokens, SqlTokenType type) {
        int end = start;
        while (end < source.length() && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
            end++;
        }
        tokens.add(new SqlToken(type, source.substring(start, end), start, end));
        return end;
    }

    private static int addWhile(String source, int start, List<SqlToken> tokens, SqlTokenType type, CharacterMatcher matcher) {
        int end = start;
        while (end < source.length() && matcher.matches(source.charAt(end))) {
            end++;
        }
        tokens.add(new SqlToken(type, source.substring(start, end), start, end));
        return end;
    }

    private static boolean startsLineComment(String source, int offset) {
        return source.charAt(offset) == '-' && offset + 1 < source.length() && source.charAt(offset + 1) == '-';
    }

    private static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private interface CharacterMatcher {
        boolean matches(char value);
    }
}
