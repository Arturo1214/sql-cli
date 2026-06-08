package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Objects;

@Deprecated
public final class WorkspaceAction {

    public enum Type {
        FOCUS,
        NEXT_FOCUS,
        PREVIOUS_FOCUS,
        REFRESH,
        TOGGLE_HELP,
        MOVE_SELECTION,
        SELECT_CURRENT,
        START_CONNECTION_FORM,
        UPDATE_FORM_FIELD,
        ADVANCE_FORM_FIELD,
        SUBMIT_FORM,
        CLOSE_MODAL,
        TRIGGER_AUTOCOMPLETE,
        EDITOR_ENTER,
        EXECUTE_CURRENT_STATEMENT,
        EXIT,
        RESIZE,
        IGNORE,
    }

    private final Type type;
    private final WorkspaceFocus focus;
    private final int selectionDelta;
    private final DatabaseType databaseType;
    private final String formText;
    private final boolean deleteBeforeCursor;
    private final int width;
    private final int height;

    private WorkspaceAction(
        Type type,
        WorkspaceFocus focus,
        int selectionDelta,
        DatabaseType databaseType,
        String formText,
        boolean deleteBeforeCursor,
        int width,
        int height
    ) {
        this.type = type;
        this.focus = focus;
        this.selectionDelta = selectionDelta;
        this.databaseType = databaseType;
        this.formText = formText;
        this.deleteBeforeCursor = deleteBeforeCursor;
        this.width = width;
        this.height = height;
    }

    public static WorkspaceAction focus(WorkspaceFocus focus) {
        return new WorkspaceAction(Type.FOCUS, focus, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction nextFocus() {
        return new WorkspaceAction(Type.NEXT_FOCUS, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction previousFocus() {
        return new WorkspaceAction(Type.PREVIOUS_FOCUS, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction refresh() {
        return new WorkspaceAction(Type.REFRESH, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction toggleHelp() {
        return new WorkspaceAction(Type.TOGGLE_HELP, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction moveSelection(int selectionDelta) {
        return new WorkspaceAction(Type.MOVE_SELECTION, null, selectionDelta, null, null, false, 0, 0);
    }

    public static WorkspaceAction selectCurrent() {
        return new WorkspaceAction(Type.SELECT_CURRENT, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction startConnectionForm(DatabaseType databaseType) {
        return new WorkspaceAction(Type.START_CONNECTION_FORM, null, 0, databaseType, null, false, 0, 0);
    }

    public static WorkspaceAction updateFormField(String formText) {
        return new WorkspaceAction(Type.UPDATE_FORM_FIELD, null, 0, null, formText, false, 0, 0);
    }

    public static WorkspaceAction deleteFormCharacter() {
        return new WorkspaceAction(Type.UPDATE_FORM_FIELD, null, 0, null, null, true, 0, 0);
    }

    public static WorkspaceAction advanceFormField() {
        return new WorkspaceAction(Type.ADVANCE_FORM_FIELD, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction submitForm() {
        return new WorkspaceAction(Type.SUBMIT_FORM, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction closeModal() {
        return new WorkspaceAction(Type.CLOSE_MODAL, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction triggerAutocomplete() {
        return new WorkspaceAction(Type.TRIGGER_AUTOCOMPLETE, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction editorEnter() {
        return new WorkspaceAction(Type.EDITOR_ENTER, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction executeCurrentStatement() {
        return new WorkspaceAction(Type.EXECUTE_CURRENT_STATEMENT, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction exit() {
        return new WorkspaceAction(Type.EXIT, null, 0, null, null, false, 0, 0);
    }

    public static WorkspaceAction resize(int width, int height) {
        return new WorkspaceAction(Type.RESIZE, null, 0, null, null, false, width, height);
    }

    public static WorkspaceAction ignore() {
        return new WorkspaceAction(Type.IGNORE, null, 0, null, null, false, 0, 0);
    }

    public Type type() {
        return type;
    }

    public WorkspaceFocus focus() {
        return focus;
    }

    public int selectionDelta() {
        return selectionDelta;
    }

    public DatabaseType databaseType() {
        return databaseType;
    }

    public String formText() {
        return formText;
    }

    public boolean deleteBeforeCursor() {
        return deleteBeforeCursor;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkspaceAction)) {
            return false;
        }
        WorkspaceAction that = (WorkspaceAction) other;
        return (
            width == that.width &&
            height == that.height &&
            selectionDelta == that.selectionDelta &&
            deleteBeforeCursor == that.deleteBeforeCursor &&
            type == that.type &&
            focus == that.focus &&
            databaseType == that.databaseType &&
            Objects.equals(formText, that.formText)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, focus, selectionDelta, databaseType, formText, deleteBeforeCursor, width, height);
    }
}
