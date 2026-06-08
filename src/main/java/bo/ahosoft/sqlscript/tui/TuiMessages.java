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
            ? "Ctrl+R ejecutar | F1/? ayuda | Tab foco | Esc cerrar | diagnosticos SQL"
            : "Ctrl+R run | F1/? help | Tab focus | Esc close | SQL diagnostics";
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
        if (value.startsWith("Safety mode blocked a dangerous SQL statement")) {
            return value.replace("Safety mode blocked a dangerous SQL statement", "Modo seguro bloqueo una sentencia SQL peligrosa");
        }
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

    String cancel() {
        return language == TuiLanguage.SPANISH ? "Cancelar" : "Cancel";
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
                "Editor: resaltado SQL y diagnosticos no destructivos\n" +
                "ArrowUp/ArrowDown: desplazar resultados verticalmente\n" +
                "ArrowLeft/ArrowRight: desplazar resultados horizontalmente\n" +
                "PageDown/PageUp: pagina siguiente/anterior\n" +
                "Conexiones: Nueva conexion Oracle, Nueva conexion PostgreSQL\n" +
                "Enter: seleccionar conexion guardada o abrir accion seleccionada\n" +
                "Comandos de metadatos: tables, describe <table>, indexes <table>\n" +
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
            "Editor: SQL highlighting and non-destructive diagnostics\n" +
            "ArrowUp/ArrowDown: scroll results vertically\n" +
            "ArrowLeft/ArrowRight: scroll results horizontally\n" +
            "PageDown/PageUp: next/previous result page\n" +
            "Connections: New Oracle connection, New PostgreSQL connection\n" +
            "Enter: select saved connection or open selected action\n" +
            "Metadata commands: tables, describe <table>, indexes <table>\n" +
            "Language: use the Language action in the left pane\n" +
            "Close help: Esc or Enter on Close"
        );
    }

    String connectionSaved(String name) {
        return language == TuiLanguage.SPANISH ? "Conexion guardada: " + name : "Connection saved: " + name;
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
