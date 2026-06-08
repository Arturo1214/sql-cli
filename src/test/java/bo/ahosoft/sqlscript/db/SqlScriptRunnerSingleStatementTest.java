package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class SqlScriptRunnerSingleStatementTest {

    @Test
    public void selectsCurrentStatementForSingleExecutionPath() {
        String buffer = "select * from users;\nselect * from orders;";

        String statement = SqlScriptRunner.currentStatement(buffer, buffer.indexOf("orders"));

        assertEquals("select * from orders", statement);
    }

    @Test
    public void rejectsDirectSingleExecutionWhenMultipleStatementsRemain() {
        try {
            SqlScriptRunner.requireSingleStatement("select * from users; select * from orders");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("single statement"));
            return;
        }
        throw new AssertionError("Expected multi-statement input to be rejected");
    }

    @Test
    public void acceptsFinalSemicolonForSingleExecution() {
        String statement = SqlScriptRunner.requireSingleStatement("select * from users;");

        assertEquals("select * from users", statement);
    }
}
