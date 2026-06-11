package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public class ReadmeSupportWorkflowTest {

    @Test
    public void readmeDocumentsSshOnlyTypedPathsShortcutsOverwriteAndCsvScope() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("F6"));
        assertTrue(readme.contains("F7"));
        assertTrue(readme.contains("F8"));
        assertTrue(readme.contains("typed server filesystem path"));
        assertTrue(readme.contains("CSV"));
        assertTrue(readme.contains("overwrite"));
        assertTrue(readme.contains("No desktop file picker"));
    }

    @Test
    public void readmeDocumentsQueryLibraryCommandsTuiShortcutsPrivacyAndSafetyGuard() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("Query library"));
        assertTrue(readme.contains("lib save <name>"));
        assertTrue(readme.contains("lib list"));
        assertTrue(readme.contains("lib search <text>"));
        assertTrue(readme.contains("lib load <id> --replace"));
        assertTrue(readme.contains("lib delete <id> --yes"));
        assertTrue(readme.contains("F9"));
        assertTrue(readme.contains("F10"));
        assertTrue(readme.contains("~/.oracle-script-cli/query-library.properties"));
        assertTrue(readme.contains("Saved SQL may contain sensitive data"));
        assertTrue(readme.contains("Loading a query never executes it"));
        assertTrue(readme.contains("SafetyGuard"));
    }

    @Test
    public void readmeDocumentsParameterizedQueryTemplateSyntaxCommandsAndRawSubstitutionWarning() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("Parameterized query templates"));
        assertTrue(readme.contains("{{name}}"));
        assertTrue(readme.contains("lib save <name> --template"));
        assertTrue(readme.contains("lib preview <id> --param name=value"));
        assertTrue(readme.contains("lib fill <id> --replace --param name=value"));
        assertTrue(readme.contains("prompted once and reused"));
        assertTrue(readme.contains("Raw substitution warning"));
        assertTrue(readme.contains("Rendered templates load into the editor only"));
        assertTrue(readme.contains("never auto-execute"));
    }

    @Test
    public void readmeDocumentsTuiDangerousSqlConfirmationAndCliUnsafeScope() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("Dangerous SQL confirmation"));
        assertTrue(readme.contains("Run the packaged CLI with no arguments to open the TUI"));
        assertTrue(readme.contains("workspace"));
        assertTrue(readme.contains("--unsafe"));
        assertTrue(readme.contains("CLI mode only"));
        assertTrue(readme.contains("type the active connection name exactly"));
        assertTrue(readme.contains("UPDATE and DELETE require a top-level WHERE"));
        assertTrue(readme.contains("DROP, TRUNCATE, and ALTER keep the existing confirmation behavior"));
    }

    @Test
    public void readmeDocumentsConnectionEditDeleteTestAndPasswordRules() throws Exception {
        String readme = new String(Files.readAllBytes(new File("README.md").toPath()), StandardCharsets.UTF_8);

        assertTrue(readme.contains("Edit selected connection"));
        assertTrue(readme.contains("Test selected connection"));
        assertTrue(readme.contains("Delete selected connection"));
        assertTrue(readme.contains("explicit confirmation before deletion"));
        assertTrue(readme.contains("Deleting the active connection leaves the workspace disconnected"));
        assertTrue(readme.contains("Testing a connection never saves the draft"));
        assertTrue(readme.contains("Leave the edit password blank to keep the existing secret"));
        assertTrue(readme.contains("Newly typed replacement passwords can be revealed"));
        assertTrue(readme.contains("bounded timeout"));
    }
}
