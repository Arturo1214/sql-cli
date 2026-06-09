package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.domain.SqlExecutionResult;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CsvResultExporterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportsOnlyCurrentPageWithHeadersAndCsvEscaping() throws Exception {
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1", Arrays.asList("ID", "NOTE"), rows(105)).nextPage();
        File target = temporaryFolder.newFile("current.csv");

        new CsvResultExporter().export(result, target, ExportScope.CURRENT_PAGE, true);

        String csv = read(target);
        assertTrue(csv.startsWith("ID,NOTE\n101,"));
        assertTrue(csv.contains("\"value,101\""));
        assertFalse(csv.contains("100,value"));
    }

    @Test
    public void exportsAllPagesInResultOrder() throws Exception {
        SqlExecutionResult result = SqlExecutionResult.paged("SQL #1", Arrays.asList("ID", "NOTE"), rows(105));
        File target = new File(temporaryFolder.getRoot(), "all.csv");

        new CsvResultExporter().export(result, target, ExportScope.ALL_PAGES, false);

        String csv = read(target);
        assertTrue(csv.startsWith("ID,NOTE\n1,"));
        assertTrue(csv.contains("\n105,"));
        assertEquals(106, csv.split("\n").length);
    }

    @Test
    public void deniesOverwriteAndDoesNotTreatPartialExportAsSuccess() throws Exception {
        File target = temporaryFolder.newFile("existing.csv");
        Files.write(target.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        try {
            new CsvResultExporter()
                .export(
                    SqlExecutionResult.paged("SQL #1", Arrays.asList("ID"), Arrays.asList(Arrays.asList("1"))),
                    target,
                    ExportScope.CURRENT_PAGE,
                    false
                );
        } catch (SupportWorkflowFileService.WorkflowFileException ex) {
            assertTrue(ex.getMessage().contains("Target file already exists"));
        }

        assertEquals("original", read(target));
    }

    @Test
    public void rejectsResultsWithoutTabularRows() throws Exception {
        File target = new File(temporaryFolder.getRoot(), "empty.csv");

        try {
            new CsvResultExporter().export(new SqlExecutionResult("plain log"), target, ExportScope.CURRENT_PAGE, false);
        } catch (SupportWorkflowFileService.WorkflowFileException ex) {
            assertTrue(ex.getMessage().contains("No tabular query result is available"));
        }

        assertFalse(target.exists());
    }

    private static List<List<String>> rows(int count) {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (int i = 1; i <= count; i++) {
            rows.add(Arrays.asList(String.valueOf(i), "value," + i));
        }
        return rows;
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
