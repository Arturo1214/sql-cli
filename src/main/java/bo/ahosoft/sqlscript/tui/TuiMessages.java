package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class TuiMessages {

    private final TuiLanguage language;

    public TuiMessages(TuiLanguage language) {
        this.language = language == null ? TuiLanguage.ENGLISH : language;
    }

    public TuiLanguage language() {
        return language;
    }

    String windowTitle() {
        return language == TuiLanguage.SPANISH ? "Espacio de Scripts DB" : "Database Script Workspace";
    }

    String ready() {
        return language == TuiLanguage.SPANISH ? "Listo" : "Ready";
    }

    String none() {
        return language == TuiLanguage.SPANISH ? "ninguna" : "none";
    }

    String noResultsYet() {
        return language == TuiLanguage.SPANISH ? "Sin resultados todavia" : "No results yet";
    }

    String sqlEditorTitle() {
        return language == TuiLanguage.SPANISH ? "Editor SQL" : "SQL Editor";
    }

    String resultsLogsTitle() {
        return language == TuiLanguage.SPANISH ? "Resultados / Logs" : "Results / Logs";
    }

    String newOracleConnection() {
        return language == TuiLanguage.SPANISH ? "Nueva conexion Oracle" : "New Oracle connection";
    }

    String newPostgresqlConnection() {
        return language == TuiLanguage.SPANISH ? "Nueva conexion PostgreSQL" : "New PostgreSQL connection";
    }

    String languageAction() {
        return language == TuiLanguage.SPANISH ? "Idioma: Espanol" : "Language: English";
    }

    public String helpHint() {
        return language == TuiLanguage.SPANISH
            ? "Ctrl+R ejecutar | F6 abrir SQL | F9 guardar consulta | F10 biblioteca | F7/F8 exportar CSV | F1/? ayuda | Tab foco | Esc cerrar | diagnosticos SQL"
            : "Ctrl+R run | F6 open SQL | F9 save query | F10 library | F7/F8 export CSV | F1/? help | Tab focus | Esc close | SQL diagnostics";
    }

    public String statusText(WorkspaceDashboardRenderer.DashboardState state) {
        String status = localizeStatus(valueOrDefault(state.statusMessage(), "Ready"));
        String active = activeConnectionLabel(state);
        if (language == TuiLanguage.SPANISH) {
            return "Estado: " + status + " | Activa: " + active;
        }
        return "Status: " + status + " | Active: " + active;
    }

    public String localizeStatus(String value) {
        if (language != TuiLanguage.SPANISH || value == null) {
            return value;
        }
        if ("Ready".equals(value)) return "Listo";
        if ("Query completed".equals(value)) return "Consulta completada";
        if ("Nothing is ready to execute".equals(value)) return "No hay nada listo para ejecutar";
        if ("No active connection selected".equals(value)) return "No hay una conexion activa seleccionada";
        if ("No saved connections".equals(value)) return "No hay conexiones guardadas";
        if (value.startsWith("Safety mode blocked UPDATE/DELETE without a top-level WHERE clause")) {
            return value.replace(
                "Safety mode blocked UPDATE/DELETE without a top-level WHERE clause",
                "Modo seguro bloqueo UPDATE/DELETE sin WHERE de nivel superior"
            );
        }
        if (value.startsWith("Safety mode blocked a dangerous SQL statement")) {
            return value.replace("Safety mode blocked a dangerous SQL statement", "Modo seguro bloqueo una sentencia SQL peligrosa");
        }
        if ("Dangerous SQL execution canceled".equals(value)) return dangerousSqlCanceled();
        if (value.startsWith("Confirmation did not match")) return "La confirmacion no coincide. El SQL peligroso no se ejecuto.";
        if ("Metadata loaded".equals(value)) return "Metadatos cargados";
        if (value.startsWith("Active connection: ")) return "Conexion activa: " + value.substring("Active connection: ".length());
        if (value.startsWith("Connection saved: ")) return "Conexion guardada: " + value.substring("Connection saved: ".length());
        if (value.startsWith("Result page ")) return value.replace("Result page", "Pagina de resultados").replace(" of ", " de ");
        return value;
    }

    public String localizeResultText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return noResultsYet();
        }
        return localizeStatus(value);
    }

    public String connectionWindowTitle(DatabaseType databaseType) {
        if (language == TuiLanguage.SPANISH) {
            return "Nueva conexion " + databaseTypeLabel(databaseType);
        }
        return "New " + databaseTypeLabel(databaseType) + " connection";
    }

    public String connectionWizard(DatabaseType databaseType) {
        if (language == TuiLanguage.SPANISH) {
            return "Asistente de conexion " + databaseTypeLabel(databaseType);
        }
        return databaseTypeLabel(databaseType) + " connection wizard";
    }

    String name() {
        return language == TuiLanguage.SPANISH ? "Nombre" : "Name";
    }

    String environment() {
        return language == TuiLanguage.SPANISH ? "Entorno" : "Environment";
    }

    String jdbcUrl() {
        return "JDBC URL";
    }

    String username() {
        return language == TuiLanguage.SPANISH ? "Usuario" : "Username";
    }

    String password() {
        return language == TuiLanguage.SPANISH ? "Contrasena" : "Password";
    }

    String schemas() {
        return language == TuiLanguage.SPANISH ? "Esquemas" : "Schemas";
    }

    String save() {
        return language == TuiLanguage.SPANISH ? "Guardar" : "Save";
    }

    String saveTemplate() {
        return language == TuiLanguage.SPANISH ? "Guardar plantilla" : "Save Template";
    }

    String fillTemplate() {
        return language == TuiLanguage.SPANISH ? "Completar plantilla" : "Fill Template";
    }

    String preview() {
        return language == TuiLanguage.SPANISH ? "Vista previa" : "Preview";
    }

    String templateFillTitle() {
        return language == TuiLanguage.SPANISH ? "Completar plantilla" : "Fill Template";
    }

    String rawSubstitutionWarning() {
        return language == TuiLanguage.SPANISH
            ? "Advertencia de sustitucion textual: los valores se insertan como texto. Cita y escapa valores antes de ejecutar."
            : QueryLibraryStore.RAW_SUBSTITUTION_WARNING;
    }

    String cancel() {
        return language == TuiLanguage.SPANISH ? "Cancelar" : "Cancel";
    }

    String runAnyway() {
        return language == TuiLanguage.SPANISH ? "Ejecutar igual" : "Run anyway";
    }

    String back() {
        return language == TuiLanguage.SPANISH ? "Volver" : "Back";
    }

    String defaults() {
        return language == TuiLanguage.SPANISH ? "Valores por defecto" : "Defaults";
    }

    String close() {
        return language == TuiLanguage.SPANISH ? "Cerrar" : "Close";
    }

    String helpTitle() {
        return language == TuiLanguage.SPANISH ? "Ayuda" : "Help";
    }

    public String helpBody() {
        if (language == TuiLanguage.SPANISH) {
            return (
                "Atajos de teclado\n" +
                "F1/?: abrir ayuda fuera del editor SQL\n" +
                "F2/Ctrl+B: enfocar Conexiones\n" +
                "F3/Ctrl+E: enfocar Editor SQL\n" +
                "Tab/Shift+Tab: mover foco\n" +
                "F5/Ctrl+R: ejecutar SQL o comando\n" +
                "F6: abrir archivo .sql desde una ruta del servidor\n" +
                "F9: guardar el SQL actual en la biblioteca de consultas\n" +
                "F10: abrir/buscar la biblioteca de consultas\n" +
                "F7: exportar pagina actual como CSV\n" +
                "F8: exportar todas las paginas como CSV con confirmacion\n" +
                "Editor: resaltado SQL y diagnosticos no destructivos\n" +
                "ArrowUp/ArrowDown: desplazar resultados verticalmente\n" +
                "ArrowLeft/ArrowRight: desplazar resultados horizontalmente\n" +
                "PageDown/PageUp: pagina siguiente/anterior\n" +
                "Conexiones: Nueva conexion Oracle, Nueva conexion PostgreSQL\n" +
                "Enter: seleccionar conexion guardada o abrir accion seleccionada\n" +
                "Comandos de metadatos: tables, describe <table>, indexes <table>\n" +
                "Biblioteca: lib save <name>, lib save <name> --template, lib list, lib search <text>, lib load <id> --replace, lib preview <id> --param name=value, lib fill <id> --replace --param name=value, lib delete <id> --yes\n" +
                "Plantillas: usa {{name}}; los duplicados se piden una vez y se reutilizan. Advertencia de sustitucion textual: cita y escapa valores antes de ejecutar.\n" +
                "Seguridad: UPDATE/DELETE requieren WHERE de nivel superior; DROP/TRUNCATE/ALTER conservan la confirmacion peligrosa.\n" +
                "El SQL guardado puede contener datos sensibles. Cargar una consulta no la ejecuta.\n" +
                "Idioma: usa la accion Idioma/Espanol en el panel izquierdo\n" +
                "Cerrar ayuda: Esc o Enter en Cerrar"
            );
        }
        return (
            "Keyboard shortcuts\n" +
            "F1/?: open help outside the SQL editor\n" +
            "F2/Ctrl+B: focus Connections\n" +
            "F3/Ctrl+E: focus SQL Editor\n" +
            "Tab/Shift+Tab: move focus\n" +
            "F5/Ctrl+R: execute SQL or command\n" +
            "F6: open a .sql file from a server path\n" +
            "F9: save current SQL to the query library\n" +
            "F10: open/search the query library\n" +
            "F7: export current result page as CSV\n" +
            "F8: export all result pages as CSV with confirmation\n" +
            "Editor: SQL highlighting and non-destructive diagnostics\n" +
            "ArrowUp/ArrowDown: scroll results vertically\n" +
            "ArrowLeft/ArrowRight: scroll results horizontally\n" +
            "PageDown/PageUp: next/previous result page\n" +
            "Connections: New Oracle connection, New PostgreSQL connection\n" +
            "Enter: select saved connection or open selected action\n" +
            "Metadata commands: tables, describe <table>, indexes <table>\n" +
            "Query library: lib save <name>, lib save <name> --template, lib list, lib search <text>, lib load <id> --replace, lib preview <id> --param name=value, lib fill <id> --replace --param name=value, lib delete <id> --yes\n" +
            "Templates: use {{name}}; duplicates are prompted once and reused. Raw substitution warning: quote and escape values before running.\n" +
            "Safety: UPDATE/DELETE require a top-level WHERE; DROP/TRUNCATE/ALTER keep dangerous confirmation.\n" +
            "Saved SQL may contain sensitive data. Loading a query does not execute it.\n" +
            "Language: use the Language action in the left pane\n" +
            "Close help: Esc or Enter on Close"
        );
    }

    String connectionSaved(String name) {
        return language == TuiLanguage.SPANISH ? "Conexion guardada: " + name : "Connection saved: " + name;
    }

    String loadSqlFileAction() {
        return language == TuiLanguage.SPANISH ? "Abrir archivo SQL (F6)" : "Load SQL File (F6)";
    }

    String saveQueryToLibraryAction() {
        return language == TuiLanguage.SPANISH ? "Guardar consulta en biblioteca (F9)" : "Save Query to Library (F9)";
    }

    String openQueryLibraryAction() {
        return language == TuiLanguage.SPANISH ? "Abrir biblioteca de consultas (F10)" : "Open Query Library (F10)";
    }

    String queryLibraryTitle() {
        return language == TuiLanguage.SPANISH ? "Biblioteca de consultas" : "Query Library";
    }

    String saveQueryTitle() {
        return language == TuiLanguage.SPANISH ? "Guardar consulta en biblioteca" : "Save Query to Library";
    }

    String description() {
        return language == TuiLanguage.SPANISH ? "Descripcion" : "Description";
    }

    String tags() {
        return language == TuiLanguage.SPANISH ? "Etiquetas" : "Tags";
    }

    String favorite() {
        return language == TuiLanguage.SPANISH ? "Favorita" : "Favorite";
    }

    String search() {
        return language == TuiLanguage.SPANISH ? "Buscar" : "Search";
    }

    String load() {
        return language == TuiLanguage.SPANISH ? "Cargar" : "Load";
    }

    String delete() {
        return language == TuiLanguage.SPANISH ? "Eliminar" : "Delete";
    }

    String unfavorite() {
        return language == TuiLanguage.SPANISH ? "Quitar favorita" : "Unfavorite";
    }

    String exportCurrentPageAction() {
        return language == TuiLanguage.SPANISH ? "Exportar pagina actual CSV (F7)" : "Export Current Page CSV (F7)";
    }

    String exportAllPagesAction() {
        return language == TuiLanguage.SPANISH ? "Exportar todas las paginas CSV (F8)" : "Export All Pages CSV (F8)";
    }

    String openSqlFileTitle() {
        return language == TuiLanguage.SPANISH ? "Abrir archivo SQL" : "Open SQL File";
    }

    String exportTitle(ExportScope scope) {
        if (scope == ExportScope.ALL_PAGES) {
            return language == TuiLanguage.SPANISH ? "Exportar todas las paginas CSV" : "Export All Pages CSV";
        }
        return language == TuiLanguage.SPANISH ? "Exportar pagina actual CSV" : "Export Current Page CSV";
    }

    String pathLabel() {
        return language == TuiLanguage.SPANISH ? "Ruta" : "Path";
    }

    String replaceDirtyEditorConfirmation() {
        return language == TuiLanguage.SPANISH ? "Reemplazar el contenido actual del editor?" : "Replace current editor content?";
    }

    String exportAllPagesConfirmation() {
        return language == TuiLanguage.SPANISH ? "Exportar todas las paginas del resultado?" : "Export all result pages?";
    }

    String exportAllPagesConfirmationTitle() {
        return language == TuiLanguage.SPANISH ? "Confirmar exportacion de todas las paginas" : "Export All Pages Confirmation";
    }

    String exportAllPagesCancelled() {
        return language == TuiLanguage.SPANISH ? "Exportacion de todas las paginas cancelada" : "Export all pages cancelled";
    }

    String overwriteConfirmationTitle() {
        return language == TuiLanguage.SPANISH ? "Sobrescribir archivo existente" : "Overwrite Existing File";
    }

    String overwriteConfirmation(String path) {
        return language == TuiLanguage.SPANISH ? "Sobrescribir archivo existente: " + path + "?" : "Overwrite existing file: " + path + "?";
    }

    String exportOverwriteCancelled() {
        return language == TuiLanguage.SPANISH ? "Sobrescritura de exportacion cancelada" : "Export overwrite cancelled";
    }

    String dangerousSqlConfirmationTitle(boolean production) {
        if (production) {
            return language == TuiLanguage.SPANISH ? "Confirmacion SQL peligrosa en PROD" : "Dangerous PROD SQL confirmation";
        }
        return language == TuiLanguage.SPANISH ? "Confirmacion SQL peligrosa" : "Dangerous SQL confirmation";
    }

    String dangerousSqlConfirmationMessage(String operation, String requiredConfirmation, boolean production) {
        if (language == TuiLanguage.SPANISH) {
            if (production) {
                return (
                    "Riesgo alto: " +
                    operation +
                    " en una conexion PROD. Escribe " +
                    requiredConfirmation +
                    " exactamente para ejecutar una sola vez."
                );
            }
            return ("Riesgo: " + operation + " puede modificar datos. Escribe " + requiredConfirmation + " para ejecutar una sola vez.");
        }
        if (production) {
            return ("High risk: " + operation + " on a PROD connection. Type " + requiredConfirmation + " exactly to execute once.");
        }
        return "Risk: " + operation + " can change data. Type " + requiredConfirmation + " to execute once.";
    }

    String dangerousSqlCanceled() {
        return language == TuiLanguage.SPANISH ? "Ejecucion SQL peligrosa cancelada" : "Dangerous SQL execution canceled";
    }

    String continueAction() {
        return language == TuiLanguage.SPANISH ? "Continuar" : "Continue";
    }

    String overwriteAction() {
        return language == TuiLanguage.SPANISH ? "Sobrescribir" : "Overwrite";
    }

    String sqlFileLoaded(String path) {
        return language == TuiLanguage.SPANISH ? "Archivo SQL cargado: " + path : "SQL file loaded: " + path;
    }

    String csvExported(String path) {
        return language == TuiLanguage.SPANISH ? "CSV exportado: " + path : "CSV exported: " + path;
    }

    String connectionCancelled() {
        return language == TuiLanguage.SPANISH ? "Creacion de conexion cancelada" : "Connection creation cancelled";
    }

    String connectionRequestRequired() {
        return language == TuiLanguage.SPANISH ? "La solicitud de conexion es requerida" : "Connection request is required";
    }

    String connectionNameRequired() {
        return language == TuiLanguage.SPANISH ? "El nombre de conexion es requerido" : "Connection name is required";
    }

    String jdbcUrlRequired() {
        return language == TuiLanguage.SPANISH ? "JDBC URL es requerido" : "JDBC URL is required";
    }

    String jdbcUrlDoesNotMatch(DatabaseType databaseType) {
        return language == TuiLanguage.SPANISH
            ? "JDBC URL no coincide con " + databaseTypeLabel(databaseType)
            : "JDBC URL does not match " + databaseTypeLabel(databaseType);
    }

    String usernameRequired() {
        return language == TuiLanguage.SPANISH ? "El usuario es requerido" : "Username is required";
    }

    String passwordRequired() {
        return language == TuiLanguage.SPANISH ? "La contrasena es requerida" : "Password is required";
    }

    public String resultsTitle(WorkspaceFocus focus, int horizontalOffset) {
        String title = language == TuiLanguage.SPANISH ? "Resultados" : "Results";
        if (focus == WorkspaceFocus.RESULTS) {
            title = title + " *";
        }
        return title + " | " + (language == TuiLanguage.SPANISH ? "←/→ Offset col " : "←/→ Col offset ") + horizontalOffset;
    }

    public String resultsTitle(WorkspaceFocus focus) {
        String title = language == TuiLanguage.SPANISH ? "Resultados" : "Results";
        return focus == WorkspaceFocus.RESULTS ? title + " *" : title;
    }

    String noQueryExecutedYet() {
        return language == TuiLanguage.SPANISH ? "Todavia no se ejecuto ninguna consulta" : "No query has been executed yet";
    }

    String resultRowsNeedMoreHeight() {
        return language == TuiLanguage.SPANISH
            ? "Aumenta la altura de la terminal para ver las filas del resultado"
            : "Increase terminal height to show result rows";
    }

    public String pageStatus(int page, String pageCount, int from, int to) {
        String rows;
        if (to == from) {
            rows = language == TuiLanguage.SPANISH ? "Filas 0" : "Rows 0";
        } else {
            rows = (language == TuiLanguage.SPANISH ? "Filas " : "Rows ") + (from + 1) + "-" + to;
        }
        if (language == TuiLanguage.SPANISH) {
            return "Pagina " + (page + 1) + "/" + pageCount + " | " + rows + " | Siguiente: PageDown | Anterior: PageUp";
        }
        return "Page " + (page + 1) + "/" + pageCount + " | " + rows + " | Next: PageDown | Previous: PageUp";
    }

    public String diagnosticStatus(SqlDiagnostic diagnostic) {
        if (diagnostic == null) {
            return "";
        }
        String message = localizeDiagnostic(diagnostic.message());
        return language == TuiLanguage.SPANISH ? "SQL: " + message : "SQL: " + message;
    }

    public String localizeDiagnostic(String value) {
        if (language != TuiLanguage.SPANISH || value == null) {
            return value;
        }
        if (value.contains("Unmatched string quote")) return "comilla de texto sin cerrar";
        if (value.contains("Unmatched opening parenthesis")) return "parentesis de apertura sin cerrar";
        if (value.contains("Unmatched closing parenthesis")) return "parentesis de cierre sin apertura";
        if (value.contains("SELECT is missing a column list before FROM")) return "SELECT no tiene lista de columnas antes de FROM";
        if (value.contains("Oracle constant SELECT may need FROM dual")) return "SELECT constante en Oracle puede necesitar FROM dual";
        if (value.contains("Did you mean tables?")) return "Sugerencia: tables";
        if (value.contains("Did you mean describe <table>?")) return "Sugerencia: describe <table>";
        if (value.contains("Did you mean indexes <table>?")) return "Sugerencia: indexes <table>";
        return value;
    }

    private static String databaseTypeLabel(DatabaseType databaseType) {
        return databaseType == DatabaseType.POSTGRESQL ? "PostgreSQL" : "Oracle";
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String activeConnectionLabel(WorkspaceDashboardRenderer.DashboardState state) {
        String activeName = valueOrDefault(state.activeConnectionName(), none());
        if (none().equals(activeName)) {
            return activeName;
        }
        for (WorkspaceDashboardRenderer.ConnectionSummary connection : state.connections()) {
            if (connection.active() && activeName.equals(connection.name())) {
                return activeName + " [" + connection.environment() + "]";
            }
        }
        return activeName;
    }
}
