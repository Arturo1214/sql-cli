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
        assertTrue(readme.contains("opens the interactive workspace by default"));
        assertTrue(readme.contains("java -jar target/oracle-script-cli.jar workspace"));
        assertTrue(readme.contains("Lanterna GUI2 split-pane workspace"));
        assertTrue(readme.contains("left explorer"));
        assertTrue(readme.contains("SQL editor"));
        assertTrue(readme.contains("bottom results/logs"));
        assertTrue(readme.contains("Ctrl+R"));
        assertTrue(readme.contains("Ctrl+R` executes the visible SQL buffer"));
        assertTrue(readme.contains("F1` or `?` shows help"));
        assertFalse(readme.contains("Ctrl+H` shows help"));
        assertTrue(readme.contains("Enter` activates the selected connection or action in the explorer"));
        assertTrue(readme.contains("New Oracle"));
        assertTrue(readme.contains("New PostgreSQL"));
        assertTrue(readme.contains("prompts for name, JDBC URL, username, password, and schemas"));
        assertTrue(readme.contains("plain text"));
        assertTrue(readme.contains("does not add syntax highlighting, autocomplete, or SQL correction"));
        assertTrue(readme.contains("compact fallback"));
        assertFalse(readme.contains("Lanterna-backed raw terminal workspace"));
        assertFalse(readme.contains("Raw workspace shortcuts"));
        assertTrue(readme.contains("run-current"));
        assertTrue(readme.contains("connections"));
        assertTrue(readme.contains("use <name>"));
        assertTrue(readme.contains("buffer set"));
        assertTrue(readme.contains("run --force --confirm-risk YES"));
        assertTrue(readme.contains("help"));
        assertTrue(readme.contains("exit"));
    }
}
