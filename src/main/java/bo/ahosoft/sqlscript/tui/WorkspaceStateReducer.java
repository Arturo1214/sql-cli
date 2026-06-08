package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

@Deprecated
public final class WorkspaceStateReducer {

    private WorkspaceStateReducer() {}

    public static WorkspaceScreenState reduce(WorkspaceScreenState state, WorkspaceAction action) {
        if (state == null) {
            state = WorkspaceScreenState.initial();
        }
        if (action == null) {
            return state;
        }

        switch (action.type()) {
            case FOCUS:
                return state.withFocus(action.focus());
            case NEXT_FOCUS:
                return state.withFocus(state.focus().next());
            case PREVIOUS_FOCUS:
                return state.withFocus(state.focus().previous());
            case REFRESH:
                return state.withLastRefreshedFocus(state.focus());
            case TOGGLE_HELP:
                return state.withHelpVisible(!state.helpVisible());
            case MOVE_SELECTION:
                return state.moveSelection(action.selectionDelta()).withFocus(WorkspaceFocus.CONNECTIONS);
            case SELECT_CURRENT:
                return state.withActiveConnectionIndex(state.selectedConnectionIndex()).withFocus(WorkspaceFocus.EDITOR);
            case START_CONNECTION_FORM:
                return state.withCreationForm(action.databaseType());
            case UPDATE_FORM_FIELD:
                return action.deleteBeforeCursor() ? state.deleteFormCharacter() : state.appendFormText(action.formText());
            case ADVANCE_FORM_FIELD:
                return state.advanceFormField();
            case SUBMIT_FORM:
                return state.creationFormVisible() ? state.withFormSubmitRequested(true) : state;
            case CLOSE_MODAL:
                if (state.creationFormVisible()) {
                    return state.withCreationForm(null).withFormSubmitRequested(false);
                }
                if (state.helpVisible()) {
                    return state.withHelpVisible(false);
                }
                return state;
            case RESIZE:
                return state.withCompactLayout(
                    action.width() < WorkspaceScreenState.MIN_FULL_WIDTH || action.height() < WorkspaceScreenState.MIN_FULL_HEIGHT
                );
            case EXIT:
                return state.withExitRequested(true);
            default:
                return state;
        }
    }
}
