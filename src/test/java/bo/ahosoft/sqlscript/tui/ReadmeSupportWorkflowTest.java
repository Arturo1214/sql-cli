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
}
