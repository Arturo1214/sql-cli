package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ConnectionListComponent {

    private static final List<String> CREATION_ACTIONS = Collections.unmodifiableList(
        Arrays.asList("New Oracle connection", "New PostgreSQL connection")
    );

    private final List<ConnectionItem> connections;

    public ConnectionListComponent(List<ConnectionItem> connections) {
        this.connections = connections == null ? Collections.<ConnectionItem>emptyList() : new ArrayList<>(connections);
    }

    public RenderedPanel render(int selectedIndex, WorkspaceFocus focus) {
        return render(selectedIndex, -1, focus);
    }

    public RenderedPanel render(int selectedIndex, int activeIndex, WorkspaceFocus focus) {
        List<String> lines = new ArrayList<>();
        lines.add(focus == WorkspaceFocus.CONNECTIONS ? "Connections *" : "Connections");
        int rowCount = rowCount();
        int clampedIndex = Math.max(0, Math.min(selectedIndex, rowCount - 1));

        if (connections.isEmpty()) {
            lines.add("No saved connections");
            appendActionRows(lines, CREATION_ACTIONS, clampedIndex, 0);
            return new RenderedPanel(lines, Selection.empty(), Selection.empty(), CREATION_ACTIONS, rowCount);
        }

        for (int i = 0; i < connections.size(); i++) {
            ConnectionItem item = connections.get(i);
            lines.add(markerFor(i, clampedIndex, activeIndex) + renderConnection(item));
        }

        List<String> actions = new ArrayList<>(CREATION_ACTIONS);
        lines.add("Actions");
        appendActionRows(lines, actions, clampedIndex, connections.size());
        Selection selected = clampedIndex < connections.size() ? Selection.of(connections.get(clampedIndex)) : Selection.empty();
        Selection active = activeIndex >= 0 && activeIndex < connections.size()
            ? Selection.of(connections.get(activeIndex))
            : Selection.empty();
        return new RenderedPanel(lines, selected, active, actions, rowCount);
    }

    public RenderedPanel render(int selectedIndex, int activeIndex, WorkspaceFocus focus, int maxLines, int maxColumns) {
        RenderedPanel full = render(selectedIndex, activeIndex, focus);
        int safeMaxLines = Math.max(1, maxLines);
        int safeMaxColumns = Math.max(1, maxColumns);
        if (full.lines().size() <= safeMaxLines) {
            return full.withLines(clipLines(full.lines(), safeMaxColumns));
        }
        List<String> lines = new ArrayList<String>();
        lines.add(full.lines().get(0));
        int bodyLines = Math.max(0, safeMaxLines - 1);
        if (bodyLines > 0) {
            int selectedLine = selectedLineIndex(selectedIndex);
            int bodyStart = Math.max(1, Math.min(selectedLine - bodyLines + 1, full.lines().size() - bodyLines));
            int bodyEnd = Math.min(full.lines().size(), bodyStart + bodyLines);
            lines.addAll(full.lines().subList(bodyStart, bodyEnd));
        }
        return full.withLines(clipLines(lines, safeMaxColumns));
    }

    public int rowCount() {
        return connections.isEmpty() ? CREATION_ACTIONS.size() : connections.size() + CREATION_ACTIONS.size();
    }

    public ConnectionItem activeItem(int activeIndex) {
        if (activeIndex < 0 || activeIndex >= connections.size()) {
            return null;
        }
        return connections.get(activeIndex);
    }

    public ConnectionItem activeItemByName(String name) {
        int index = activeIndexByName(name);
        return index < 0 ? null : connections.get(index);
    }

    public int activeIndexByName(String name) {
        if (name == null || name.trim().isEmpty() || "none".equals(name)) {
            return -1;
        }
        for (int index = 0; index < connections.size(); index++) {
            if (name.equals(connections.get(index).name())) {
                return index;
            }
        }
        return -1;
    }

    private static void appendActionRows(List<String> lines, List<String> actions, int selectedIndex, int offset) {
        for (int i = 0; i < actions.size(); i++) {
            int rowIndex = offset + i;
            lines.add((rowIndex == selectedIndex ? "> " : "  ") + actions.get(i));
        }
    }

    private static String markerFor(int index, int selectedIndex, int activeIndex) {
        if (index == selectedIndex) {
            return index == activeIndex ? "* " : "> ";
        }
        if (index == activeIndex) {
            return "* ";
        }
        return "  ";
    }

    public static ConnectionConfig prepareConnection(
        DatabaseType databaseType,
        String jdbcUrl,
        String username,
        String password,
        List<String> schemas
    ) {
        return prepareConnection(databaseType, ConnectionEnvironment.DEV, jdbcUrl, username, password, schemas);
    }

    public static ConnectionConfig prepareConnection(
        DatabaseType databaseType,
        ConnectionEnvironment environment,
        String jdbcUrl,
        String username,
        String password,
        List<String> schemas
    ) {
        if (databaseType == DatabaseType.POSTGRESQL && (schemas == null || schemas.isEmpty())) {
            return new ConnectionConfig(databaseType, environment, jdbcUrl, username, password, Collections.singletonList("public"));
        }
        return new ConnectionConfig(databaseType, environment, jdbcUrl, username, password, schemas);
    }

    private static String renderConnection(ConnectionItem item) {
        StringBuilder line = new StringBuilder();
        if (item.config().environment().isProduction()) {
            line.append("!! PROD !! ");
        }
        line.append('[').append(item.config().environment()).append("] ");
        line.append(item.name()).append(" [").append(item.config().databaseType()).append(']');
        if (!item.config().schemas().isEmpty()) {
            line.append(" schema=").append(item.config().schemas().get(0));
        }
        return line.toString();
    }

    private int selectedLineIndex(int selectedIndex) {
        int clampedIndex = Math.max(0, Math.min(selectedIndex, rowCount() - 1));
        if (connections.isEmpty()) {
            return 2 + clampedIndex;
        }
        if (clampedIndex < connections.size()) {
            return 1 + clampedIndex;
        }
        return 2 + connections.size() + (clampedIndex - connections.size());
    }

    private static List<String> clipLines(List<String> lines, int maxColumns) {
        List<String> clipped = new ArrayList<String>();
        for (String line : lines) {
            clipped.add(clipLine(line, maxColumns));
        }
        return clipped;
    }

    private static String clipLine(String line, int maxColumns) {
        String value = line == null ? "" : line;
        if (value.length() <= maxColumns) {
            return value;
        }
        if (maxColumns <= 1) {
            return "…";
        }
        return value.substring(0, maxColumns - 1) + "…";
    }

    public static final class ConnectionItem {

        private final String name;
        private final ConnectionConfig config;

        public ConnectionItem(String name, ConnectionConfig config) {
            this.name = name;
            this.config = config;
        }

        String name() {
            return name;
        }

        ConnectionConfig config() {
            return config;
        }
    }

    public static final class RenderedPanel {

        private final List<String> lines;
        private final Selection selected;
        private final Selection active;
        private final List<String> actionLabels;
        private final int rowCount;

        public RenderedPanel(List<String> lines, Selection selected, Selection active, List<String> actionLabels, int rowCount) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.selected = selected;
            this.active = active;
            this.actionLabels = Collections.unmodifiableList(new ArrayList<>(actionLabels));
            this.rowCount = rowCount;
        }

        List<String> lines() {
            return lines;
        }

        Selection selected() {
            return selected;
        }

        Selection active() {
            return active;
        }

        List<String> actionLabels() {
            return actionLabels;
        }

        int rowCount() {
            return rowCount;
        }

        public RenderedPanel withLines(List<String> lines) {
            return new RenderedPanel(lines, selected, active, actionLabels, rowCount);
        }
    }

    public static final class Selection {

        private final ConnectionItem item;

        private Selection(ConnectionItem item) {
            this.item = item;
        }

        public static Selection of(ConnectionItem item) {
            return new Selection(item);
        }

        public static Selection empty() {
            return new Selection(null);
        }

        boolean isPresent() {
            return item != null;
        }

        String name() {
            return item == null ? "" : item.name();
        }

        ConnectionConfig config() {
            return item == null ? null : item.config();
        }
    }
}
