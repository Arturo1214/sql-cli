package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.domain.SqlExecutionResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class CsvResultExporter {

    private final SupportWorkflowFileService fileService;

    public CsvResultExporter() {
        this(new SupportWorkflowFileService());
    }

    CsvResultExporter(SupportWorkflowFileService fileService) {
        this.fileService = fileService;
    }

    public void export(SqlExecutionResult result, File target, ExportScope scope, boolean overwrite) {
        if (result == null || !result.hasTabularRows()) {
            throw new SupportWorkflowFileService.WorkflowFileException("No tabular query result is available to export");
        }
        File safeTarget = fileService.validateExportTarget(target == null ? null : target.getPath(), overwrite);
        File temp = new File(safeTarget.getParentFile(), safeTarget.getName() + ".tmp");
        try {
            writeCsv(result, temp, scope == null ? ExportScope.CURRENT_PAGE : scope);
            try {
                Files.move(temp.toPath(), safeTarget.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp.toPath(), safeTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            temp.delete();
            throw new SupportWorkflowFileService.WorkflowFileException("Could not export CSV: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            temp.delete();
            throw ex;
        }
    }

    private static void writeCsv(SqlExecutionResult result, File temp, ExportScope scope) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8);
        try {
            SqlExecutionResult.TabularPage first = result.tabularPage(scope == ExportScope.CURRENT_PAGE ? result.pageIndex() : 0);
            writeLine(writer, first.headers());
            if (scope == ExportScope.CURRENT_PAGE) {
                writeRows(writer, first.rows());
                return;
            }
            for (int page = 0; page < first.pageCount(); page++) {
                writeRows(writer, result.tabularPage(page).rows());
            }
        } finally {
            writer.close();
        }
    }

    private static void writeRows(BufferedWriter writer, List<List<String>> rows) throws IOException {
        for (List<String> row : rows) {
            writeLine(writer, row);
        }
    }

    private static void writeLine(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(i)));
        }
        writer.write('\n');
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        boolean quoted = safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0;
        safe = safe.replace("\"", "\"\"");
        return quoted ? "\"" + safe + "\"" : safe;
    }
}
