package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class MutationWhereValidatorTest {

    @Test
    public void splitsStatementsWithoutUsingSemicolonsInsideTextOrComments() {
        List<String> statements = MutationWhereValidator.splitStatements(
            "update users set note = 'one;two' where id = 1; -- ignored; semicolon\n" +
            "delete from audit_log where message = 'x; y'; /* ignored; semicolon */ select 1 from dual"
        );

        assertEquals(3, statements.size());
        assertEquals("update users set note = 'one;two' where id = 1", statements.get(0));
        assertEquals("delete from audit_log where message = 'x; y'", statements.get(1));
        assertEquals("select 1 from dual", statements.get(2));
    }

    @Test
    public void detectsOnlyTopLevelWhereForUpdateAndDelete() {
        assertTrue(MutationWhereValidator.hasTopLevelWhere("update users set enabled = 0 where id = 10"));
        assertTrue(
            MutationWhereValidator.hasTopLevelWhere("delete from users where id in (select user_id from sessions where active = 0)")
        );
        assertFalse(MutationWhereValidator.hasTopLevelWhere("update users set note = 'where hidden'"));
        assertFalse(MutationWhereValidator.hasTopLevelWhere("delete from users /* where hidden */"));
        assertFalse(MutationWhereValidator.hasTopLevelWhere("delete from users where_backup = 1"));
    }

    @Test
    public void nestedWhereDoesNotSatisfyMutationGate() {
        assertMissingWhere("delete from users where_exists in (select id from audit where active = 1)");
        assertMissingWhere("update users set id = (select id from other where active = 1)");
        assertMissingWhere("delete from users -- where hidden\n");
    }

    @Test
    public void failFastReportsFirstUnsafeStatementInBatch() {
        try {
            MutationWhereValidator.requireTopLevelWhereForMutations("update users set enabled = 0; delete from audit_log where id = 4");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(SafetyGuard.MISSING_WHERE_MESSAGE));
            assertTrue(ex.getMessage().contains("statement 1"));
            return;
        }
        throw new AssertionError("Expected missing WHERE to be blocked");
    }

    @Test
    public void ignoresNonMutationStatementsAndAllowsMutationsWithTopLevelWhere() {
        MutationWhereValidator.requireTopLevelWhereForMutations(
            "truncate table users; drop table audit_log; alter table users add active number"
        );
        MutationWhereValidator.requireTopLevelWhereForMutations(
            "update users set enabled = 0 where id = 1; delete from audit_log where created_at < sysdate - 30"
        );
    }

    private static void assertMissingWhere(String sql) {
        try {
            MutationWhereValidator.requireTopLevelWhereForMutations(sql);
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(SafetyGuard.MISSING_WHERE_MESSAGE));
            return;
        }
        throw new AssertionError("Expected missing WHERE to be blocked");
    }
}
