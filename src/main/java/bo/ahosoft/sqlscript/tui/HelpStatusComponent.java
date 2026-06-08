package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HelpStatusComponent {

    public String statusBar(WorkspaceFocus focus, String connectionName, String executionStatus) {
        return statusBar(focus, connectionName, null, executionStatus);
    }

    public String statusBar(WorkspaceFocus focus, String connectionName, DatabaseType databaseType, String executionStatus) {
        String safeConnection = connectionName == null || connectionName.trim().isEmpty() ? "none" : connectionName;
        String safeStatus = executionStatus == null || executionStatus.trim().isEmpty() ? "Ready" : executionStatus;
        String activeContext = databaseType == null || "none".equals(safeConnection)
            ? safeConnection
            : safeConnection + " [" + databaseType + "]";
        return "Focus: " + focus + " | Active: " + activeContext + " | " + safeStatus + " | Ctrl+R Run | F1/? Help | Esc Exit";
    }

    public RenderedHelp renderHelpOverlay(boolean visible, WorkspaceFocus focus) {
        return renderHelpOverlay(visible, focus, null);
    }

    public RenderedHelp renderHelpOverlay(boolean visible, WorkspaceFocus focus, DatabaseType creationFormType) {
        if (!visible) {
            return new RenderedHelp(Collections.<String>emptyList());
        }

        List<String> lines = new ArrayList<>();
        lines.add(focus == WorkspaceFocus.HELP ? "Help *" : "Help");
        if (creationFormType != null) {
            lines.add("Form: New " + displayName(creationFormType) + " connection");
            lines.add("Enter: submit form | Esc: cancel form");
        }
        lines.add("Arrow Up/Down or j/k: move left-panel selection");
        lines.add("Enter: select connection or submit visible form");
        lines.add("Tab: show SQL keyword/table autocomplete");
        lines.add("Ctrl+R: run current SQL buffer");
        lines.add("F1/?: toggle this help outside the SQL editor");
        lines.add("Esc/Ctrl+C: exit workspace");
        return new RenderedHelp(lines);
    }

    public RenderedHelp renderConnectionForm(
        boolean visible,
        DatabaseType databaseType,
        int fieldIndex,
        String name,
        String jdbcUrl,
        String username,
        String schemas,
        String validationMessage
    ) {
        if (!visible) {
            return new RenderedHelp(Collections.<String>emptyList());
        }
        List<String> lines = new ArrayList<>();
        lines.add("Connection Form: New " + displayName(databaseType));
        lines.add(fieldMarker(fieldIndex, 0) + "Name: " + safe(name));
        lines.add(fieldMarker(fieldIndex, 1) + "JDBC URL: " + safe(jdbcUrl));
        lines.add(fieldMarker(fieldIndex, 2) + "Username: " + safe(username));
        lines.add(fieldMarker(fieldIndex, 3) + "Password: " + maskedPassword(fieldIndex));
        lines.add(fieldMarker(fieldIndex, 4) + "Schemas: " + safe(schemas));
        if (validationMessage != null && !validationMessage.trim().isEmpty()) {
            lines.add("Validation: " + validationMessage);
        }
        lines.add("Enter: next/submit | Esc: cancel");
        return new RenderedHelp(lines);
    }

    private static String fieldMarker(int current, int expected) {
        return current == expected ? "> " : "  ";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String maskedPassword(int fieldIndex) {
        return fieldIndex == 3 ? "" : "********";
    }

    private static String displayName(DatabaseType databaseType) {
        return databaseType == DatabaseType.POSTGRESQL ? "PostgreSQL" : "Oracle";
    }

    public static final class RenderedHelp {

        private final List<String> lines;

        public RenderedHelp(List<String> lines) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }

        List<String> lines() {
            return lines;
        }
    }
}
