package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class OracleScriptCliDocumentationTest {

    @Test
    public void readmeDocumentsInteractiveWorkspaceAndRunCurrentAutomationPath() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("java -jar target/oracle-script-cli.jar"));
        assertTrue(readme.contains("open the TUI"));
        assertTrue(readme.contains("java -jar target/oracle-script-cli.jar workspace"));
        assertTrue(readme.contains("Database Script Workspace"));
        assertTrue(readme.contains("Left menu"));
        assertTrue(readme.contains("SQL editor"));
        assertTrue(readme.contains("Results/logs"));
        assertTrue(readme.contains("Ctrl+R"));
        assertTrue(readme.contains("Execute the current SQL buffer"));
        assertTrue(readme.contains("Open help"));
        assertFalse(readme.contains("Ctrl+H` shows help"));
        assertTrue(readme.contains("Select the active connection/action"));
        assertTrue(readme.contains("New Oracle"));
        assertTrue(readme.contains("New PostgreSQL"));
        assertTrue(readme.contains("uses a constrained environment selector"));
        assertTrue(readme.contains("plain text"));
        assertTrue(readme.contains("plain text"));
        assertTrue(readme.contains("compact fallback"));
        assertFalse(readme.contains("Lanterna-backed raw terminal workspace"));
        assertFalse(readme.contains("Raw workspace shortcuts"));
        assertTrue(readme.contains("run-current"));
        assertTrue(readme.contains("connections"));
        assertTrue(readme.contains("use <name>"));
        assertTrue(readme.contains("buffer set"));
        assertTrue(readme.contains("--unsafe"));
        assertTrue(readme.contains("help"));
        assertTrue(readme.contains("exit"));
    }
}
