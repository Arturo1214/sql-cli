package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SupportWorkflowFileServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsUtf8SqlFileFromTypedServerPath() throws Exception {
        File file = temporaryFolder.newFile("query.sql");
        write(file, "select 'á';\n");

        SupportWorkflowFileService service = new SupportWorkflowFileService();

        assertEquals("select 'á';\n", service.readSqlFile(file.getPath()).content());
    }

    @Test
    public void rejectsMissingDirectoryAndUnsupportedExtensionWithoutReading() throws Exception {
        SupportWorkflowFileService service = new SupportWorkflowFileService();

        assertError("File does not exist", readError(service, new File(temporaryFolder.getRoot(), "missing.sql").getPath()));
        assertError("Path is a directory", readError(service, temporaryFolder.getRoot().getPath()));

        File txt = temporaryFolder.newFile("query.txt");
        write(txt, "select 1");
        assertError("Only .sql files are supported", readError(service, txt.getPath()));
    }

    @Test
    public void validatesExportTargetsForOverwriteAndParentPath() throws Exception {
        SupportWorkflowFileService service = new SupportWorkflowFileService();
        File existing = temporaryFolder.newFile("result.csv");

        assertError("Target file already exists", exportError(service, existing.getPath(), false));
        assertEquals(existing.getCanonicalFile(), service.validateExportTarget(existing.getPath(), true));
        assertError(
            "Parent directory does not exist",
            exportError(service, new File(temporaryFolder.getRoot(), "missing/result.csv").getPath(), true)
        );
    }

    private static String readError(SupportWorkflowFileService service, String path) {
        try {
            service.readSqlFile(path);
            return "";
        } catch (SupportWorkflowFileService.WorkflowFileException ex) {
            return ex.getMessage();
        }
    }

    private static String exportError(SupportWorkflowFileService service, String path, boolean overwrite) {
        try {
            service.validateExportTarget(path, overwrite);
            return "";
        } catch (SupportWorkflowFileService.WorkflowFileException ex) {
            return ex.getMessage();
        }
    }

    private static void assertError(String expected, String actual) {
        assertTrue(actual, actual.contains(expected));
    }

    private static void write(File file, String content) throws Exception {
        FileOutputStream output = new FileOutputStream(file);
        try {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        } finally {
            output.close();
        }
    }
}
