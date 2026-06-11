package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.SafetyGuard;
import org.junit.Test;

public class TuiMessagesTest {

    @Test
    public void englishSupportWorkflowCopyIncludesShortcutsErrorsConfirmationsAndHelp() {
        TuiMessages messages = new TuiMessages(TuiLanguage.ENGLISH);

        assertTrue(messages.helpHint().contains("F6 open"));
        assertTrue(messages.helpHint().contains("F9 save query"));
        assertTrue(messages.helpHint().contains("F10 library"));
        assertTrue(messages.helpHint().contains("edit/test/delete connections"));
        assertTrue(messages.helpBody().contains("F7: export current result page as CSV"));
        assertTrue(messages.helpBody().contains("F9: save current SQL to the query library"));
        assertTrue(messages.helpBody().contains("lib save <name>"));
        assertTrue(messages.helpBody().contains("lib save <name> --template"));
        assertTrue(messages.helpBody().contains("lib preview <id> --param name=value"));
        assertTrue(messages.helpBody().contains("Raw substitution warning"));
        assertTrue(messages.templateFillTitle().contains("Fill Template"));
        assertTrue(messages.saveTemplate().contains("Save Template"));
        assertTrue(messages.fillTemplate().contains("Fill Template"));
        assertTrue(messages.helpBody().contains("Saved SQL may contain sensitive data"));
        assertTrue(messages.replaceDirtyEditorConfirmation().contains("Replace current editor content"));
        assertTrue(messages.exportAllPagesConfirmation().contains("Export all result pages"));
        assertTrue(messages.overwriteConfirmation("results.csv").contains("Overwrite existing file"));
        assertTrue(messages.exportOverwriteCancelled().contains("Export overwrite cancelled"));
        assertTrue(messages.loadSqlFileAction().contains("F6"));
        assertTrue(messages.saveQueryToLibraryAction().contains("F9"));
        assertTrue(messages.openQueryLibraryAction().contains("F10"));
        assertTrue(messages.dangerousSqlConfirmationTitle(false).contains("Dangerous SQL confirmation"));
        assertTrue(messages.dangerousSqlConfirmationMessage("UPDATE", "RUN", false).contains("Type RUN"));
        assertTrue(messages.helpBody().contains("UPDATE/DELETE require a top-level WHERE"));
        assertTrue(messages.localizeStatus(SafetyGuard.MISSING_WHERE_MESSAGE).contains("top-level WHERE"));
        assertTrue(messages.runAnyway().contains("Run anyway"));
        assertTrue(messages.dangerousSqlCanceled().contains("Dangerous SQL execution canceled"));
        assertTrue(messages.editSelectedConnectionAction().contains("Edit selected connection"));
        assertTrue(messages.testSelectedConnectionAction().contains("Test selected connection"));
        assertTrue(messages.deleteSelectedConnectionAction().contains("Delete selected connection"));
        assertTrue(
            messages.editConnectionWindowTitle(bo.ahosoft.sqlscript.domain.DatabaseType.POSTGRESQL).contains("Edit PostgreSQL connection")
        );
        assertTrue(messages.deleteConnectionConfirmation("reporting").contains("Delete saved connection: reporting?"));
        assertTrue(messages.keepExistingPasswordHint().contains("Leave password blank"));
        assertTrue(messages.testConnection().contains("Test"));
    }

    @Test
    public void spanishSupportWorkflowCopyIncludesShortcutsErrorsConfirmationsAndHelp() {
        TuiMessages messages = new TuiMessages(TuiLanguage.SPANISH);

        assertTrue(messages.helpHint().contains("F6 abrir"));
        assertTrue(messages.helpHint().contains("F9 guardar consulta"));
        assertTrue(messages.helpHint().contains("F10 biblioteca"));
        assertTrue(messages.helpHint().contains("editar/probar/eliminar conexiones"));
        assertTrue(messages.helpBody().contains("F7: exportar pagina actual"));
        assertTrue(messages.helpBody().contains("F9: guardar el SQL actual en la biblioteca"));
        assertTrue(messages.helpBody().contains("lib save <name>"));
        assertTrue(messages.helpBody().contains("lib save <name> --template"));
        assertTrue(messages.helpBody().contains("lib preview <id> --param name=value"));
        assertTrue(messages.helpBody().contains("Advertencia de sustitucion textual"));
        assertTrue(messages.templateFillTitle().contains("Completar plantilla"));
        assertTrue(messages.saveTemplate().contains("Guardar plantilla"));
        assertTrue(messages.fillTemplate().contains("Completar plantilla"));
        assertTrue(messages.helpBody().contains("El SQL guardado puede contener datos sensibles"));
        assertTrue(messages.replaceDirtyEditorConfirmation().contains("Reemplazar el contenido actual"));
        assertTrue(messages.exportAllPagesConfirmation().contains("Exportar todas las paginas"));
        assertTrue(messages.overwriteConfirmation("results.csv").contains("Sobrescribir archivo existente"));
        assertTrue(messages.exportOverwriteCancelled().contains("Sobrescritura de exportacion cancelada"));
        assertTrue(messages.loadSqlFileAction().contains("F6"));
        assertTrue(messages.saveQueryToLibraryAction().contains("F9"));
        assertTrue(messages.openQueryLibraryAction().contains("F10"));
        assertTrue(messages.dangerousSqlConfirmationTitle(true).contains("Confirmacion SQL peligrosa en PROD"));
        assertTrue(messages.dangerousSqlConfirmationMessage("DROP", "prod-main", true).contains("Escribe prod-main"));
        assertTrue(messages.helpBody().contains("UPDATE/DELETE requieren WHERE de nivel superior"));
        assertTrue(messages.localizeStatus(SafetyGuard.MISSING_WHERE_MESSAGE).contains("WHERE de nivel superior"));
        assertTrue(messages.runAnyway().contains("Ejecutar igual"));
        assertTrue(messages.dangerousSqlCanceled().contains("Ejecucion SQL peligrosa cancelada"));
        assertTrue(messages.editSelectedConnectionAction().contains("Editar conexion seleccionada"));
        assertTrue(messages.testSelectedConnectionAction().contains("Probar conexion seleccionada"));
        assertTrue(messages.deleteSelectedConnectionAction().contains("Eliminar conexion seleccionada"));
        assertTrue(messages.editConnectionWindowTitle(bo.ahosoft.sqlscript.domain.DatabaseType.ORACLE).contains("Editar conexion Oracle"));
        assertTrue(messages.deleteConnectionConfirmation("reporting").contains("Eliminar conexion guardada: reporting?"));
        assertTrue(messages.keepExistingPasswordHint().contains("Deja la contrasena vacia"));
        assertTrue(messages.testConnection().contains("Probar"));
    }
}
