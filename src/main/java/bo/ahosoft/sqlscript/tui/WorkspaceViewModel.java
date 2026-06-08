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

@Deprecated
public final class WorkspaceViewModel {

    private WorkspaceViewModel() {}

    static RenderedWorkspace compose(
        WorkspaceScreenState state,
        ConnectionListComponent connections,
        BasicSqlEditorComponent editor,
        ResultsPanelComponent results,
        HelpStatusComponent help,
        int width,
        int height,
        String activeConnectionName
    ) {
        boolean compact =
            state.compactLayout() || width < WorkspaceScreenState.MIN_FULL_WIDTH || height < WorkspaceScreenState.MIN_FULL_HEIGHT;
        int activeIndex = state.activeConnectionIndex();
        ConnectionListComponent.ConnectionItem activeItem = connections.activeItem(activeIndex);
        if (activeItem == null) {
            activeIndex = connections.activeIndexByName(activeConnectionName);
            activeItem = connections.activeItemByName(activeConnectionName);
        }
        String activeName = activeItem == null ? activeConnectionName : activeItem.name();
        DatabaseType activeType = activeItem == null ? null : activeItem.config().databaseType();
        ConnectionEnvironment activeEnvironment = activeItem == null ? null : activeItem.config().environment();
        ConnectionListComponent.RenderedPanel connectionPanel = connections.render(
            state.selectedConnectionIndex(),
            activeIndex,
            state.focus()
        );
        SqlRenderModel editorRenderModel = editor.renderModel(
            compact ? width : Math.max(20, width - (width / 3) - 2),
            compact ? 3 : Math.max(4, height / 3)
        );
        List<String> editorLines = new ArrayList<>();
        editorLines.add(state.focus() == WorkspaceFocus.EDITOR ? "SQL Editor *" : "SQL Editor");
        for (SqlRenderLine line : editorRenderModel.lines()) {
            editorLines.add(line.text());
        }

        List<String> helpLines = new ArrayList<>();
        helpLines.addAll(
            help
                .renderConnectionForm(
                    state.creationFormVisible(),
                    state.creationFormType(),
                    state.formFieldIndex(),
                    state.formName(),
                    state.formJdbcUrl(),
                    state.formUsername(),
                    state.formSchemas(),
                    state.formValidationMessage()
                )
                .lines()
        );
        helpLines.addAll(help.renderHelpOverlay(state.helpVisible(), state.focus(), state.creationFormType()).lines());

        return new RenderedWorkspace(
            compact ? "Compact workspace" : activeHeader(activeName, activeEnvironment, activeType),
            compact,
            connectionPanel.lines(),
            editorLines,
            editorRenderModel,
            completionPopupLines(editorRenderModel.completionCandidates()),
            results.render(compact ? 3 : Math.max(3, height / 4), state.focus()).lines(),
            help.statusBar(state.focus(), activeName, activeEnvironment, activeType, editor.executionReadinessMessage()),
            helpLines
        );
    }

    private static String activeHeader(String activeName, ConnectionEnvironment environment, DatabaseType activeType) {
        String safeName = activeName == null || activeName.trim().isEmpty() || "none".equals(activeName) ? "none" : activeName;
        if (activeType == null || "none".equals(safeName)) {
            return "Workspace | Active: " + safeName;
        }
        return (
            "Workspace | Active: " +
            safeName +
            " [" +
            (environment == null ? ConnectionEnvironment.DEV : environment) +
            "] [" +
            activeType +
            "]"
        );
    }

    private static List<String> completionPopupLines(List<CompletionCandidate> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        lines.add("Autocomplete");
        for (CompletionCandidate candidate : candidates) {
            lines.add("  " + candidate.label() + " [" + candidate.kind() + "]");
        }
        return lines;
    }

    public static final class RenderedWorkspace {

        private final String title;
        private final boolean compact;
        private final List<String> connectionLines;
        private final List<String> editorLines;
        private final SqlRenderModel editorRenderModel;
        private final List<String> completionPopupLines;
        private final List<String> resultLines;
        private final String statusLine;
        private final List<String> helpLines;

        RenderedWorkspace(
            String title,
            boolean compact,
            List<String> connectionLines,
            List<String> editorLines,
            SqlRenderModel editorRenderModel,
            List<String> completionPopupLines,
            List<String> resultLines,
            String statusLine,
            List<String> helpLines
        ) {
            this.title = title;
            this.compact = compact;
            this.connectionLines = immutable(connectionLines);
            this.editorLines = immutable(editorLines);
            this.editorRenderModel = editorRenderModel;
            this.completionPopupLines = immutable(completionPopupLines);
            this.resultLines = immutable(resultLines);
            this.statusLine = statusLine;
            this.helpLines = immutable(helpLines);
        }

        String title() {
            return title;
        }

        String headerLine() {
            return title;
        }

        boolean compact() {
            return compact;
        }

        List<String> connectionLines() {
            return connectionLines;
        }

        List<String> leftPaneLines() {
            return connectionLines;
        }

        List<String> editorLines() {
            return editorLines;
        }

        List<String> rightPaneLines() {
            return editorLines;
        }

        SqlRenderModel editorRenderModel() {
            return editorRenderModel;
        }

        List<CompletionCandidate> completionCandidates() {
            return editorRenderModel.completionCandidates();
        }

        List<String> completionPopupLines() {
            return completionPopupLines;
        }

        List<String> resultLines() {
            return resultLines;
        }

        List<String> bottomPaneLines() {
            return resultLines;
        }

        String statusLine() {
            return statusLine;
        }

        String footerLine() {
            return statusLine;
        }

        List<String> helpLines() {
            return helpLines;
        }

        private static List<String> immutable(List<String> lines) {
            return Collections.unmodifiableList(new ArrayList<>(lines));
        }
    }
}
