package bo.ahosoft.sqlscript.sql;

import static org.junit.Assert.assertEquals;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class SqlStatementSelectorTest {

    @Test
    public void selectsStatementAtCursor() {
        String buffer = "select * from users;\nselect * from orders;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf("orders"));

        assertEquals("select * from orders", statement);
    }

    @Test
    public void ignoresLaterStatementsWhenCursorIsInFirstStatement() {
        String buffer = "select * from users;\nselect * from orders;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf("users"));

        assertEquals("select * from users", statement);
    }

    @Test
    public void treatsFinalSemicolonAsCurrentStatementEnd() {
        String buffer = "select * from users;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.length());

        assertEquals("select * from users", statement);
    }

    @Test
    public void doesNotSplitInsideSingleQuotedSemicolon() {
        String buffer = "select 'a;b' as value from dual;\nselect * from orders;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf("value"));

        assertEquals("select 'a;b' as value from dual", statement);
    }

    @Test
    public void ignoresUnfinishedTrailingTextAfterCurrentStatement() {
        String buffer = "select * from users;\nselect * from";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf(';'));

        assertEquals("select * from users", statement);
    }

    @Test
    public void selectsStatementContainingCursorAcrossMultipleLines() {
        String buffer = "select *\nfrom users;\nselect *\nfrom orders;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf("orders"));

        assertEquals("select *\nfrom orders", statement);
    }

    @Test
    public void selectsPreviousStatementWhenCursorIsWhitespaceAfterSemicolon() {
        String buffer = "select * from users;\n\nselect * from orders;";

        String statement = SqlStatementSelector.currentStatement(buffer, buffer.indexOf("select * from orders") - 1);

        assertEquals("select * from users", statement);
    }

    @Test
    public void selectsNextStatementWhenCursorIsWhitespaceBeforeStatementWithoutPreviousSemicolonContext() {
        String buffer = "\n\nselect * from users;";

        String statement = SqlStatementSelector.currentStatement(buffer, 1);

        assertEquals("select * from users", statement);
    }
}
