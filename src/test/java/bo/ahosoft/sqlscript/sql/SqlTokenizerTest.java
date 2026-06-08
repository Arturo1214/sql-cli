package bo.ahosoft.sqlscript.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.domain.DatabaseType;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.List;
import org.junit.Test;

public class SqlTokenizerTest {

    @Test
    public void tokenizesKeywordsIdentifiersWhitespaceAndPunctuation() {
        List<SqlToken> tokens = SqlTokenizer.tokenize("select * from users");

        assertEquals(7, tokens.size());
        assertToken(tokens.get(0), SqlTokenType.KEYWORD, "select", 0, 6);
        assertToken(tokens.get(1), SqlTokenType.WHITESPACE, " ", 6, 7);
        assertToken(tokens.get(2), SqlTokenType.PUNCTUATION, "*", 7, 8);
        assertToken(tokens.get(3), SqlTokenType.WHITESPACE, " ", 8, 9);
        assertToken(tokens.get(4), SqlTokenType.KEYWORD, "from", 9, 13);
        assertToken(tokens.get(5), SqlTokenType.WHITESPACE, " ", 13, 14);
        assertToken(tokens.get(6), SqlTokenType.IDENTIFIER, "users", 14, 19);
    }

    @Test
    public void tokenizesLiteralsCommentsNumbersAndUnknownCharacters() {
        List<SqlToken> tokens = SqlTokenizer.tokenize("where name = 'Ada' -- note\n#");

        assertEquals(11, tokens.size());
        assertToken(tokens.get(0), SqlTokenType.KEYWORD, "where", 0, 5);
        assertToken(tokens.get(1), SqlTokenType.WHITESPACE, " ", 5, 6);
        assertToken(tokens.get(2), SqlTokenType.IDENTIFIER, "name", 6, 10);
        assertToken(tokens.get(3), SqlTokenType.WHITESPACE, " ", 10, 11);
        assertToken(tokens.get(4), SqlTokenType.PUNCTUATION, "=", 11, 12);
        assertToken(tokens.get(5), SqlTokenType.WHITESPACE, " ", 12, 13);
        assertToken(tokens.get(6), SqlTokenType.LITERAL, "'Ada'", 13, 18);
        assertToken(tokens.get(7), SqlTokenType.WHITESPACE, " ", 18, 19);
        assertToken(tokens.get(8), SqlTokenType.COMMENT, "-- note", 19, 26);
        assertToken(tokens.get(9), SqlTokenType.WHITESPACE, "\n", 26, 27);
        assertToken(tokens.get(10), SqlTokenType.UNKNOWN, "#", 27, 28);
    }

    @Test
    public void tokenizesQuotedIdentifiersAndUnclosedStringAsVisibleLiterals() {
        List<SqlToken> tokens = SqlTokenizer.tokenize("select \"User Name\" from users where id = '42");

        assertEquals(15, tokens.size());
        assertToken(tokens.get(2), SqlTokenType.IDENTIFIER, "\"User Name\"", 7, 18);
        assertToken(tokens.get(14), SqlTokenType.LITERAL, "'42", 41, 44);
    }

    @Test
    public void tokenizesDialectKeywordsStringsNumbersCommentsAndOperators() {
        List<SqlToken> tokens = SqlTokenizer.tokenize("select jsonb 'x', limit 10 -- note", SqlDialect.POSTGRESQL);

        assertToken(tokens.get(0), SqlTokenType.KEYWORD, "select", 0, 6);
        assertToken(tokens.get(2), SqlTokenType.KEYWORD, "jsonb", 7, 12);
        assertToken(tokens.get(4), SqlTokenType.STRING, "'x'", 13, 16);
        assertToken(tokens.get(5), SqlTokenType.OPERATOR, ",", 16, 17);
        assertToken(tokens.get(7), SqlTokenType.KEYWORD, "limit", 18, 23);
        assertToken(tokens.get(9), SqlTokenType.NUMBER, "10", 24, 26);
        assertToken(tokens.get(11), SqlTokenType.COMMENT, "-- note", 27, 34);
    }

    @Test
    public void exposesRepresentativeOracleAndPostgresqlKeywordSets() {
        assertTrue(SqlDialect.ORACLE.isKeyword("dual"));
        assertTrue(SqlDialect.ORACLE.isKeyword("merge"));
        assertTrue(SqlDialect.ORACLE.isKeyword("connect"));
        assertTrue(SqlDialect.POSTGRESQL.isKeyword("returning"));
        assertTrue(SqlDialect.POSTGRESQL.isKeyword("jsonb"));
        assertTrue(SqlDialect.POSTGRESQL.isKeyword("limit"));
        assertEquals(SqlDialect.ORACLE, SqlDialect.fromDatabaseType(DatabaseType.ORACLE));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromDatabaseType(DatabaseType.POSTGRESQL));
        assertEquals(SqlDialect.GENERIC, SqlDialect.fromDatabaseType(null));
    }

    private static void assertToken(SqlToken token, SqlTokenType type, String text, int startOffset, int endOffset) {
        assertEquals(type, token.type());
        assertEquals(text, token.text());
        assertEquals(startOffset, token.startOffset());
        assertEquals(endOffset, token.endOffset());
    }
}
