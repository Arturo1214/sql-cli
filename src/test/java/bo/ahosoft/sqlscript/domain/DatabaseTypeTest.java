package bo.ahosoft.sqlscript.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class DatabaseTypeTest {

    @Test
    public void defaultsMissingValuesToOracleForLegacyProfiles() {
        assertEquals(DatabaseType.ORACLE, DatabaseType.fromStoredValue(null));
        assertEquals(DatabaseType.ORACLE, DatabaseType.fromStoredValue(" "));
    }

    @Test
    public void parsesStoredDatabaseTypeCaseInsensitively() {
        assertEquals(DatabaseType.ORACLE, DatabaseType.fromStoredValue("oracle"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromStoredValue("postgresql"));
    }

    @Test
    public void validatesDatabaseSpecificJdbcUrlPrefixes() {
        assertTrue(DatabaseType.ORACLE.acceptsJdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1"));
        assertFalse(DatabaseType.ORACLE.acceptsJdbcUrl("jdbc:postgresql://localhost:5432/app"));

        assertTrue(DatabaseType.POSTGRESQL.acceptsJdbcUrl("jdbc:postgresql://localhost:5432/app"));
        assertFalse(DatabaseType.POSTGRESQL.acceptsJdbcUrl("jdbc:oracle:thin:@localhost:1521/XEPDB1"));
    }
}
