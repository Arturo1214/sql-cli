package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

@Deprecated
public final class WorkspaceScreenState {

    static final int MIN_FULL_WIDTH = 80;
    static final int MIN_FULL_HEIGHT = 24;

    private final WorkspaceFocus focus;
    private final int selectedConnectionIndex;
    private final int leftPanelRowCount;
    private final int activeConnectionIndex;
    private final String editorText;
    private final boolean helpVisible;
    private final DatabaseType creationFormType;
    private final int formFieldIndex;
    private final String formName;
    private final String formJdbcUrl;
    private final String formUsername;
    private final String formPassword;
    private final String formSchemas;
    private final String formValidationMessage;
    private final boolean formSubmitRequested;
    private final boolean compactLayout;
    private final WorkspaceFocus lastRefreshedFocus;
    private final boolean exitRequested;

    private WorkspaceScreenState(
        WorkspaceFocus focus,
        int selectedConnectionIndex,
        int leftPanelRowCount,
        int activeConnectionIndex,
        String editorText,
        boolean helpVisible,
        DatabaseType creationFormType,
        int formFieldIndex,
        String formName,
        String formJdbcUrl,
        String formUsername,
        String formPassword,
        String formSchemas,
        String formValidationMessage,
        boolean formSubmitRequested,
        boolean compactLayout,
        WorkspaceFocus lastRefreshedFocus,
        boolean exitRequested
    ) {
        this.focus = focus;
        this.selectedConnectionIndex = selectedConnectionIndex;
        this.leftPanelRowCount = leftPanelRowCount;
        this.activeConnectionIndex = activeConnectionIndex;
        this.editorText = editorText;
        this.helpVisible = helpVisible;
        this.creationFormType = creationFormType;
        this.formFieldIndex = formFieldIndex;
        this.formName = formName == null ? "" : formName;
        this.formJdbcUrl = formJdbcUrl == null ? "" : formJdbcUrl;
        this.formUsername = formUsername == null ? "" : formUsername;
        this.formPassword = formPassword == null ? "" : formPassword;
        this.formSchemas = formSchemas == null ? "" : formSchemas;
        this.formValidationMessage = formValidationMessage == null ? "" : formValidationMessage;
        this.formSubmitRequested = formSubmitRequested;
        this.compactLayout = compactLayout;
        this.lastRefreshedFocus = lastRefreshedFocus;
        this.exitRequested = exitRequested;
    }

    public static WorkspaceScreenState initial() {
        return new WorkspaceScreenState(
            WorkspaceFocus.EDITOR,
            0,
            1,
            -1,
            "",
            false,
            null,
            0,
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            false,
            null,
            false
        );
    }

    public WorkspaceScreenState withFocus(WorkspaceFocus focus) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withSelectedConnectionIndex(int selectedConnectionIndex) {
        return copy(
            focus,
            Math.max(0, selectedConnectionIndex),
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState moveSelection(int selectionDelta) {
        return copy(
            focus,
            clamp(selectedConnectionIndex + selectionDelta, leftPanelRowCount),
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withLeftPanelRowCount(int leftPanelRowCount) {
        int safeRowCount = Math.max(1, leftPanelRowCount);
        return copy(
            focus,
            clamp(selectedConnectionIndex, safeRowCount),
            safeRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withActiveConnectionIndex(int activeConnectionIndex) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withEditorText(String editorText) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText == null ? "" : editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withHelpVisible(boolean helpVisible) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withCreationForm(DatabaseType creationFormType) {
        if (creationFormType == null) {
            return copy(
                focus,
                selectedConnectionIndex,
                leftPanelRowCount,
                activeConnectionIndex,
                editorText,
                helpVisible,
                null,
                0,
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                compactLayout,
                lastRefreshedFocus,
                exitRequested
            );
        }
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            0,
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState appendFormText(String text) {
        String value = text == null ? "" : text;
        return withFormFieldValue(currentFormFieldValue() + value).withFormValidationMessage("");
    }

    public WorkspaceScreenState deleteFormCharacter() {
        String current = currentFormFieldValue();
        String next = current.isEmpty() ? current : current.substring(0, current.length() - 1);
        return withFormFieldValue(next).withFormValidationMessage("");
    }

    public WorkspaceScreenState advanceFormField() {
        if (formFieldIndex >= 4) {
            return withFormSubmitRequested(true);
        }
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex + 1,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            false,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withFormValidationMessage(String formValidationMessage) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    private WorkspaceScreenState withFormFieldValue(String value) {
        if (formFieldIndex == 0) {
            return copy(
                focus,
                selectedConnectionIndex,
                leftPanelRowCount,
                activeConnectionIndex,
                editorText,
                helpVisible,
                creationFormType,
                formFieldIndex,
                value,
                formJdbcUrl,
                formUsername,
                formPassword,
                formSchemas,
                formValidationMessage,
                false,
                compactLayout,
                lastRefreshedFocus,
                exitRequested
            );
        }
        if (formFieldIndex == 1) {
            return copy(
                focus,
                selectedConnectionIndex,
                leftPanelRowCount,
                activeConnectionIndex,
                editorText,
                helpVisible,
                creationFormType,
                formFieldIndex,
                formName,
                value,
                formUsername,
                formPassword,
                formSchemas,
                formValidationMessage,
                false,
                compactLayout,
                lastRefreshedFocus,
                exitRequested
            );
        }
        if (formFieldIndex == 2) {
            return copy(
                focus,
                selectedConnectionIndex,
                leftPanelRowCount,
                activeConnectionIndex,
                editorText,
                helpVisible,
                creationFormType,
                formFieldIndex,
                formName,
                formJdbcUrl,
                value,
                formPassword,
                formSchemas,
                formValidationMessage,
                false,
                compactLayout,
                lastRefreshedFocus,
                exitRequested
            );
        }
        if (formFieldIndex == 3) {
            return copy(
                focus,
                selectedConnectionIndex,
                leftPanelRowCount,
                activeConnectionIndex,
                editorText,
                helpVisible,
                creationFormType,
                formFieldIndex,
                formName,
                formJdbcUrl,
                formUsername,
                value,
                formSchemas,
                formValidationMessage,
                false,
                compactLayout,
                lastRefreshedFocus,
                exitRequested
            );
        }
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            value,
            formValidationMessage,
            false,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withFormSubmitRequested(boolean formSubmitRequested) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withCompactLayout(boolean compactLayout) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withLastRefreshedFocus(WorkspaceFocus lastRefreshedFocus) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    public WorkspaceScreenState withExitRequested(boolean exitRequested) {
        return copy(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    WorkspaceFocus focus() {
        return focus;
    }

    int selectedConnectionIndex() {
        return selectedConnectionIndex;
    }

    int leftPanelRowCount() {
        return leftPanelRowCount;
    }

    int activeConnectionIndex() {
        return activeConnectionIndex;
    }

    String editorText() {
        return editorText;
    }

    boolean helpVisible() {
        return helpVisible;
    }

    boolean creationFormVisible() {
        return creationFormType != null;
    }

    DatabaseType creationFormType() {
        return creationFormType;
    }

    int formFieldIndex() {
        return formFieldIndex;
    }

    String formName() {
        return formName;
    }

    String formJdbcUrl() {
        return formJdbcUrl;
    }

    String formUsername() {
        return formUsername;
    }

    String formPassword() {
        return formPassword;
    }

    String formSchemas() {
        return formSchemas;
    }

    String formValidationMessage() {
        return formValidationMessage;
    }

    boolean formSubmitRequested() {
        return formSubmitRequested;
    }

    boolean compactLayout() {
        return compactLayout;
    }

    WorkspaceFocus lastRefreshedFocus() {
        return lastRefreshedFocus;
    }

    boolean exitRequested() {
        return exitRequested;
    }

    private WorkspaceScreenState copy(
        WorkspaceFocus focus,
        int selectedConnectionIndex,
        int leftPanelRowCount,
        int activeConnectionIndex,
        String editorText,
        boolean helpVisible,
        DatabaseType creationFormType,
        int formFieldIndex,
        String formName,
        String formJdbcUrl,
        String formUsername,
        String formPassword,
        String formSchemas,
        String formValidationMessage,
        boolean formSubmitRequested,
        boolean compactLayout,
        WorkspaceFocus lastRefreshedFocus,
        boolean exitRequested
    ) {
        return new WorkspaceScreenState(
            focus,
            selectedConnectionIndex,
            leftPanelRowCount,
            activeConnectionIndex,
            editorText,
            helpVisible,
            creationFormType,
            formFieldIndex,
            formName,
            formJdbcUrl,
            formUsername,
            formPassword,
            formSchemas,
            formValidationMessage,
            formSubmitRequested,
            compactLayout,
            lastRefreshedFocus,
            exitRequested
        );
    }

    private String currentFormFieldValue() {
        if (formFieldIndex == 0) return formName;
        if (formFieldIndex == 1) return formJdbcUrl;
        if (formFieldIndex == 2) return formUsername;
        if (formFieldIndex == 3) return formPassword;
        return formSchemas;
    }

    private int clamp(int selectedConnectionIndex, int rowCount) {
        return Math.max(0, Math.min(selectedConnectionIndex, Math.max(1, rowCount) - 1));
    }
}
