package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class SafetyGuardTest {

    @Test
    public void allowsReadOnlySqlWithoutConfirmation() {
        SafetyGuard.requireSafe("select * from users", false, null);
    }

    @Test
    public void blocksDestructiveSqlWithoutForce() {
        assertBlocked("delete from users where id = 10", false, null, "destructive");
    }

    @Test
    public void blocksForceWithoutTypedRiskConfirmation() {
        assertBlocked("truncate table temp_users", true, null, "--confirm-risk YES");
        assertBlocked("drop table temp_users", true, "yes", "--confirm-risk YES");
    }

    @Test
    public void allowsDestructiveSqlWithForceAndExactTypedConfirmation() {
        SafetyGuard.requireSafe("update users set enabled = 0", true, "YES");
    }

    private static void assertBlocked(String sql, boolean force, String confirmation, String messagePart) {
        try {
            SafetyGuard.requireSafe(sql, force, confirmation);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(messagePart));
            return;
        }
        throw new AssertionError("Expected SQL to be blocked");
    }
}
