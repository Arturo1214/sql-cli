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

public final class WorkspaceDashboardRenderer {

    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 30;
    private static final int MIN_SPLIT_WIDTH = 80;
    private static final int MIN_SPLIT_HEIGHT = 24;

    public String render(DashboardState state) {
        return render(state, resolveCapabilities());
    }

    public String render(DashboardState state, TerminalCapabilities capabilities) {
        TerminalCapabilities safeCapabilities = capabilities == null
            ? new TerminalCapabilities(false, new TerminalSize(DEFAULT_WIDTH, DEFAULT_HEIGHT))
            : capabilities;
        TerminalSize size = safeCapabilities.size();
        if (safeCapabilities.ansi() && size.width() >= MIN_SPLIT_WIDTH && size.height() >= MIN_SPLIT_HEIGHT) {
            return renderSplit(state, size);
        }
        return renderCompact(state);
    }

    private String renderCompact(DashboardState state) {
        StringBuilder output = new StringBuilder();
        output.append("== Interactive Workspace (Compact) ==").append(System.lineSeparator());
        output.append("Active connection: ").append(activeConnectionLabel(state)).append(System.lineSeparator());
        output.append("Schemas: ").append(joinOrPlaceholder(state.schemas(), "none")).append(System.lineSeparator());
        output.append("Connections:").append(System.lineSeparator());
        appendConnections(output, state.connections());
        output.append("Buffer: ").append(bufferPreview(state.buffer())).append(System.lineSeparator());
        output
            .append("SQL Buffer:")
            .append(System.lineSeparator())
            .append(fullOrPlaceholder(state.buffer(), "empty"))
            .append(System.lineSeparator());
        output.append("History: ").append(joinOrPlaceholder(state.history(), "empty", " | ")).append(System.lineSeparator());
        output.append("Results:").append(System.lineSeparator());
        if (state.lastResult() != null && !state.lastResult().trim().isEmpty()) {
            output.append("Last result: ").append(state.lastResult()).append(System.lineSeparator());
        } else {
            output.append("No results yet").append(System.lineSeparator());
        }
        if (state.lastError() != null && !state.lastError().trim().isEmpty()) {
            output.append("Last error: ").append(state.lastError()).append(System.lineSeparator());
        }
        output.append("Status: ").append(valueOrDefault(state.statusMessage(), "Ready")).append(System.lineSeparator());
        output
            .append("Commands: help, connections, use <name>, new connection, schemas, buffer, run, tables, desc, sample, history, exit")
            .append(System.lineSeparator());
        return output.toString();
    }

    private String renderSplit(DashboardState state, TerminalSize size) {
        int width = size.width();
        int leftWidth = Math.max(24, Math.min(30, width / 3));
        int rightWidth = width - leftWidth + 1;
        int editorHeight = Math.max(8, size.height() - 13);
        StringBuilder output = new StringBuilder();
        output.append("\u001B[2J\u001B[H");
        appendBorder(output, width);
        output.append(singlePaneLine("Interactive Workspace", width)).append(System.lineSeparator());
        appendBorder(output, width);
        List<String> connections = connectionLines(state);
        List<String> editor = wrapLines(fullOrPlaceholder(state.buffer(), "empty"), rightWidth - 4);
        int middleRows = Math.max(connections.size() + 4, editorHeight);
        output.append(paneLine("Connections", "SQL Buffer", leftWidth, rightWidth)).append(System.lineSeparator());
        for (int i = 0; i < middleRows; i++) {
            String left = i < connections.size() ? connections.get(i) : "";
            String right = i < editor.size() ? editor.get(i) : "";
            output.append(paneLine(left, right, leftWidth, rightWidth)).append(System.lineSeparator());
        }
        appendBorder(output, width);
        output.append(singlePaneLine("Results", width)).append(System.lineSeparator());
        List<String> results = wrapLines(resultText(state), width - 4);
        int resultRows = Math.max(3, Math.min(6, size.height() - middleRows - 7));
        for (int i = 0; i < resultRows; i++) {
            output.append(singlePaneLine(i < results.size() ? results.get(i) : "", width)).append(System.lineSeparator());
        }
        appendBorder(output, width);
        output
            .append(singlePaneLine(valueOrDefault(state.statusMessage(), "Ready") + " | " + commandHints(), width))
            .append(System.lineSeparator());
        appendBorder(output, width);
        if (width >= DEFAULT_WIDTH) {
            output.append(commandHints()).append(System.lineSeparator());
        }
        return output.toString();
    }

    public static String commandHints() {
        return "Commands: help, connections, use <name>, new connection, schemas, buffer, run, tables, desc, sample, history, exit";
    }

    private static TerminalCapabilities resolveCapabilities() {
        boolean ansi =
            isAnsiForced() ||
            (System.console() != null &&
                !"dumb".equalsIgnoreCase(valueOrDefault(System.getenv("TERM"), "")) &&
                System.getenv("NO_COLOR") == null);
        return new TerminalCapabilities(
            ansi,
            new TerminalSize(readPositiveInt("COLUMNS", DEFAULT_WIDTH), readPositiveInt("LINES", DEFAULT_HEIGHT))
        );
    }

    private static boolean isAnsiForced() {
        return "true".equalsIgnoreCase(System.getProperty("oracleScriptCli.forceAnsi"));
    }

    private static int readPositiveInt(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(name);
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static void appendConnections(StringBuilder output, List<ConnectionSummary> connections) {
        for (String line : connectionLines(connections)) {
            output.append(line).append(System.lineSeparator());
        }
    }

    private static List<String> connectionLines(DashboardState state) {
        return connectionLines(state.connections());
    }

    private static List<String> connectionLines(List<ConnectionSummary> connections) {
        if (connections == null || connections.isEmpty()) {
            return Collections.singletonList("none");
        }
        List<String> lines = new ArrayList<>();
        for (ConnectionSummary connection : connections) {
            lines.add(
                (connection.active() ? "* " : "  ") +
                prodMarker(connection.environment()) +
                "[" +
                valueOrDefault(connection.environment(), "DEV") +
                "] " +
                valueOrNone(connection.name()) +
                " [" +
                valueOrDefault(connection.databaseType(), "unknown") +
                "]"
            );
        }
        return lines;
    }

    private static String resultText(DashboardState state) {
        if (state.lastError() != null && !state.lastError().trim().isEmpty()) {
            return state.lastError();
        }
        return fullOrPlaceholder(state.lastResult(), "No results yet");
    }

    private static void appendBorder(StringBuilder output, int width) {
        output.append("┌").append(repeat("─", width - 2)).append("┐").append(System.lineSeparator());
    }

    private static String paneLine(String left, String right, int leftWidth, int rightWidth) {
        return "│" + pad(clip(left, leftWidth - 2), leftWidth - 2) + "│" + pad(clip(right, rightWidth - 2), rightWidth - 2) + "│";
    }

    private static String singlePaneLine(String value, int width) {
        return "│" + pad(clip(value, width - 2), width - 2) + "│";
    }

    private static String clip(String value, int maxLength) {
        String normalized = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return maxLength <= 1 ? "…" : normalized.substring(0, maxLength - 1) + "…";
    }

    private static String pad(String value, int length) {
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < length) {
            padded.append(' ');
        }
        return padded.toString();
    }

    private static String repeat(String value, int count) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < count; i++) {
            output.append(value);
        }
        return output.toString();
    }

    private static List<String> wrapLines(String value, int maxLength) {
        String[] rawLines = value.split("\\R", -1);
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            lines.add(clip(rawLine, maxLength));
        }
        return lines;
    }

    private static String valueOrNone(String value) {
        return value == null || value.trim().isEmpty() ? "none" : value;
    }

    private static String activeConnectionLabel(DashboardState state) {
        String activeName = valueOrNone(state.activeConnectionName());
        if ("none".equals(activeName)) {
            return activeName;
        }
        for (ConnectionSummary connection : state.connections()) {
            if (connection.active() && activeName.equals(connection.name())) {
                return activeName + " [" + valueOrDefault(connection.environment(), "DEV") + "]";
            }
        }
        return activeName;
    }

    private static String prodMarker(String environment) {
        return "PROD".equalsIgnoreCase(environment) ? "!! PROD !! " : "";
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String fullOrPlaceholder(String value, String placeholder) {
        return value == null || value.trim().isEmpty() ? placeholder : value.trim();
    }

    private static String bufferPreview(String buffer) {
        if (buffer == null || buffer.trim().isEmpty()) {
            return "empty";
        }
        String oneLine = buffer.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= 30 ? oneLine : oneLine.substring(0, 30);
    }

    private static String joinOrPlaceholder(List<String> values, String placeholder) {
        return joinOrPlaceholder(values, placeholder, ", ");
    }

    private static String joinOrPlaceholder(List<String> values, String placeholder, String separator) {
        if (values == null || values.isEmpty()) {
            return placeholder;
        }
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (joined.length() > 0) {
                joined.append(separator);
            }
            joined.append(value);
        }
        return joined.toString();
    }

    public static final class DashboardState {

        private final String activeConnectionName;
        private final List<String> schemas;
        private final String buffer;
        private final List<String> history;
        private final String lastResult;
        private final String lastError;
        private final List<ConnectionSummary> connections;
        private final String statusMessage;

        public DashboardState(
            String activeConnectionName,
            List<String> schemas,
            String buffer,
            List<String> history,
            String lastResult,
            String lastError
        ) {
            this(
                activeConnectionName,
                schemas,
                buffer,
                history,
                lastResult,
                lastError,
                Collections.<ConnectionSummary>emptyList(),
                "Ready"
            );
        }

        public DashboardState(
            String activeConnectionName,
            List<String> schemas,
            String buffer,
            List<String> history,
            String lastResult,
            String lastError,
            List<ConnectionSummary> connections,
            String statusMessage
        ) {
            this.activeConnectionName = activeConnectionName;
            this.schemas = schemas == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(schemas));
            this.buffer = buffer;
            this.history = history == null ? Collections.<String>emptyList() : Collections.unmodifiableList(new ArrayList<>(history));
            this.lastResult = lastResult;
            this.lastError = lastError;
            this.connections = connections == null
                ? Collections.<ConnectionSummary>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(connections));
            this.statusMessage = statusMessage;
        }

        public String activeConnectionName() {
            return activeConnectionName;
        }

        public List<String> schemas() {
            return schemas;
        }

        public String buffer() {
            return buffer;
        }

        public List<String> history() {
            return history;
        }

        public String lastResult() {
            return lastResult;
        }

        public String lastError() {
            return lastError;
        }

        public List<ConnectionSummary> connections() {
            return connections;
        }

        public String statusMessage() {
            return statusMessage;
        }
    }

    public static final class ConnectionSummary {

        private final String name;
        private final String databaseType;
        private final String environment;
        private final boolean active;

        public ConnectionSummary(String name, String databaseType, boolean active) {
            this(name, databaseType, "DEV", active);
        }

        public ConnectionSummary(String name, String databaseType, String environment, boolean active) {
            this.name = name;
            this.databaseType = databaseType;
            this.environment = environment == null || environment.trim().isEmpty() ? "DEV" : environment;
            this.active = active;
        }

        public String name() {
            return name;
        }

        public String databaseType() {
            return databaseType;
        }

        public String environment() {
            return environment;
        }

        public boolean active() {
            return active;
        }
    }

    public static final class TerminalSize {

        private final int width;
        private final int height;

        public TerminalSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }

    public static final class TerminalCapabilities {

        private final boolean ansi;
        private final TerminalSize size;

        public TerminalCapabilities(boolean ansi, TerminalSize size) {
            this.ansi = ansi;
            this.size = size == null ? new TerminalSize(DEFAULT_WIDTH, DEFAULT_HEIGHT) : size;
        }

        public boolean ansi() {
            return ansi;
        }

        public TerminalSize size() {
            return size;
        }
    }
}
