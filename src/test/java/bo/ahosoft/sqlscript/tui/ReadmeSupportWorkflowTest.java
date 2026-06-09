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
}
