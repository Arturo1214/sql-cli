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
        SafetyGuard.requireSafe("select * from users", ConnectionEnvironment.DEV, "local", false, false, null);
    }

    @Test
    public void blocksDestructiveSqlWithoutForce() {
        assertBlocked("delete from users where id = 10", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
    }

    @Test
    public void blocksRequiredDangerousSqlTypesByDefault() {
        assertBlocked("drop table users", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
        assertBlocked("truncate table users", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
        assertBlocked("alter table users add active number", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
        assertBlocked("update users set active = 0", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
        assertBlocked("delete from users", ConnectionEnvironment.DEV, "local", false, false, null, "dangerous");
    }

    @Test
    public void blocksForceWithoutTypedRiskConfirmation() {
        assertBlocked("truncate table temp_users", ConnectionEnvironment.DEV, "local", true, false, null, "--confirm-risk YES");
        assertBlocked("drop table temp_users", ConnectionEnvironment.DEV, "local", true, false, "yes", "--confirm-risk YES");
    }

    @Test
    public void allowsDestructiveSqlWithForceAndExactTypedConfirmation() {
        SafetyGuard.requireSafe("update users set enabled = 0", ConnectionEnvironment.DEV, "local", true, false, "YES");
    }

    @Test
    public void unsafeBypassesNonProdDangerousSql() {
        SafetyGuard.requireSafe("delete from users", ConnectionEnvironment.QA, "qa-db", false, true, null);
    }

    @Test
    public void prodUnsafeRequiresActiveConnectionNameConfirmation() {
        assertBlocked("drop table users", ConnectionEnvironment.PROD, "billing-db", false, true, null, "--confirm-risk billing-db");
        SafetyGuard.requireSafe("drop table users", ConnectionEnvironment.PROD, "billing-db", false, true, "billing-db");
    }

    @Test
    public void selectAndMetadataLikeCommandsAreNotDangerous() {
        assertTrue(SafetyGuard.isAllowedByDefault("select * from users"));
        assertTrue(SafetyGuard.isAllowedByDefault("tables"));
        assertTrue(SafetyGuard.isAllowedByDefault("describe users"));
        assertTrue(SafetyGuard.isAllowedByDefault("indexes users"));
    }

    private static void assertBlocked(
        String sql,
        ConnectionEnvironment environment,
        String connectionName,
        boolean force,
        boolean unsafe,
        String confirmation,
        String messagePart
    ) {
        try {
            SafetyGuard.requireSafe(sql, environment, connectionName, force, unsafe, confirmation);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(messagePart));
            return;
        }
        throw new AssertionError("Expected SQL to be blocked");
    }
}
