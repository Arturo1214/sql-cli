package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TuiMessagesTest {

    @Test
    public void englishSupportWorkflowCopyIncludesShortcutsErrorsConfirmationsAndHelp() {
        TuiMessages messages = new TuiMessages(TuiLanguage.ENGLISH);

        assertTrue(messages.helpHint().contains("F6 open"));
        assertTrue(messages.helpBody().contains("F7: export current result page as CSV"));
        assertTrue(messages.replaceDirtyEditorConfirmation().contains("Replace current editor content"));
        assertTrue(messages.exportAllPagesConfirmation().contains("Export all result pages"));
        assertTrue(messages.overwriteConfirmation("results.csv").contains("Overwrite existing file"));
        assertTrue(messages.exportOverwriteCancelled().contains("Export overwrite cancelled"));
        assertTrue(messages.loadSqlFileAction().contains("F6"));
    }

    @Test
    public void spanishSupportWorkflowCopyIncludesShortcutsErrorsConfirmationsAndHelp() {
        TuiMessages messages = new TuiMessages(TuiLanguage.SPANISH);

        assertTrue(messages.helpHint().contains("F6 abrir"));
        assertTrue(messages.helpBody().contains("F7: exportar pagina actual"));
        assertTrue(messages.replaceDirtyEditorConfirmation().contains("Reemplazar el contenido actual"));
        assertTrue(messages.exportAllPagesConfirmation().contains("Exportar todas las paginas"));
        assertTrue(messages.overwriteConfirmation("results.csv").contains("Sobrescribir archivo existente"));
        assertTrue(messages.exportOverwriteCancelled().contains("Sobrescritura de exportacion cancelada"));
        assertTrue(messages.loadSqlFileAction().contains("F6"));
    }
}
