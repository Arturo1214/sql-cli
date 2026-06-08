package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SqlConsoleRendererTest {

    @Test
    public void rendersSuccessfulQueryRowsAsReadableTable() {
        String output = SqlConsoleRenderer.formatRows(
            Arrays.asList("ID", "NAME"),
            Arrays.asList(Arrays.asList("100", "ALICE"), Arrays.asList("200", "BOB"))
        );

        assertTrue(output.contains("+-----+-------+"));
        assertTrue(output.contains("| ID  | NAME  |"));
        assertTrue(output.contains("| 100 | ALICE |"));
        assertTrue(output.contains("| 200 | BOB   |"));
        assertTrue(output.contains("Total rows: 2"));
    }

    @Test
    public void rendersEmptySuccessfulQueryWithHeadersAndRowCount() {
        String output = SqlConsoleRenderer.formatRows(Arrays.asList("STATUS"), Collections.<java.util.List<String>>emptyList());

        assertTrue(output.contains("| STATUS |"));
        assertTrue(output.contains("Total rows: 0"));
    }

    @Test
    public void rendersFailureFeedbackWithSqlStateAndVendorCode() {
        String output = SqlConsoleRenderer.formatFailure(new SQLException("invalid table", "42000", 942));

        assertTrue(output.contains("Error executing SQL: invalid table"));
        assertTrue(output.contains("SQLState: 42000"));
        assertTrue(output.contains("Vendor code: 942"));
    }
}
