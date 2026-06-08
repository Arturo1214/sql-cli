package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class WorkspaceStateReducerTest {

    @Test
    public void movesFocusInExplicitAndCyclicOrder() {
        WorkspaceScreenState state = WorkspaceScreenState.initial().withFocus(WorkspaceFocus.CONNECTIONS);

        assertEquals(WorkspaceFocus.EDITOR, WorkspaceStateReducer.reduce(state, WorkspaceAction.nextFocus()).focus());
        assertEquals(WorkspaceFocus.HELP, WorkspaceStateReducer.reduce(state, WorkspaceAction.previousFocus()).focus());
        assertEquals(WorkspaceFocus.RESULTS, WorkspaceStateReducer.reduce(state, WorkspaceAction.focus(WorkspaceFocus.RESULTS)).focus());
    }

    @Test
    public void refreshPreservesActiveStateAndRecordsFocusedView() {
        WorkspaceScreenState state = WorkspaceScreenState.initial()
            .withFocus(WorkspaceFocus.EDITOR)
            .withSelectedConnectionIndex(2)
            .withEditorText("select 1")
            .withHelpVisible(true);

        WorkspaceScreenState refreshed = WorkspaceStateReducer.reduce(state, WorkspaceAction.refresh());

        assertEquals(WorkspaceFocus.EDITOR, refreshed.focus());
        assertEquals(2, refreshed.selectedConnectionIndex());
        assertEquals("select 1", refreshed.editorText());
        assertTrue(refreshed.helpVisible());
        assertEquals(WorkspaceFocus.EDITOR, refreshed.lastRefreshedFocus());
    }

    @Test
    public void movesLeftPanelSelectionWithinAvailableRows() {
        WorkspaceScreenState state = WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR).withLeftPanelRowCount(3);

        WorkspaceScreenState moved = WorkspaceStateReducer.reduce(state, WorkspaceAction.moveSelection(1));

        assertEquals(WorkspaceFocus.CONNECTIONS, moved.focus());
        assertEquals(0, WorkspaceStateReducer.reduce(state, WorkspaceAction.moveSelection(-1)).selectedConnectionIndex());
        assertEquals(1, moved.selectedConnectionIndex());
        assertEquals(
            2,
            WorkspaceStateReducer.reduce(state.withSelectedConnectionIndex(2), WorkspaceAction.moveSelection(1)).selectedConnectionIndex()
        );
    }

    @Test
    public void selectCurrentConnectionRecordsActiveConnectionAndMovesFocusToEditor() {
        WorkspaceScreenState state = WorkspaceScreenState.initial()
            .withFocus(WorkspaceFocus.CONNECTIONS)
            .withLeftPanelRowCount(3)
            .withSelectedConnectionIndex(1);

        WorkspaceScreenState selected = WorkspaceStateReducer.reduce(state, WorkspaceAction.selectCurrent());

        assertEquals(1, selected.activeConnectionIndex());
        assertEquals(WorkspaceFocus.EDITOR, selected.focus());
    }

    @Test
    public void closesVisibleHelpBeforeExitingWorkspace() {
        WorkspaceScreenState state = WorkspaceStateReducer.reduce(WorkspaceScreenState.initial(), WorkspaceAction.toggleHelp());

        WorkspaceScreenState closed = WorkspaceStateReducer.reduce(state, WorkspaceAction.closeModal());

        assertFalse(closed.helpVisible());
        assertFalse(closed.exitRequested());
    }

    @Test
    public void startsSubmitsAndCancelsConnectionFormState() {
        WorkspaceScreenState form = WorkspaceStateReducer.reduce(
            WorkspaceScreenState.initial(),
            WorkspaceAction.startConnectionForm(DatabaseType.POSTGRESQL)
        );

        assertTrue(form.creationFormVisible());
        assertEquals(DatabaseType.POSTGRESQL, form.creationFormType());
        assertTrue(WorkspaceStateReducer.reduce(form, WorkspaceAction.submitForm()).formSubmitRequested());

        WorkspaceScreenState cancelled = WorkspaceStateReducer.reduce(form, WorkspaceAction.closeModal());
        assertFalse(cancelled.creationFormVisible());
        assertFalse(cancelled.formSubmitRequested());
    }

    @Test
    public void togglesHelpAndSwitchesCompactLayoutBySize() {
        WorkspaceScreenState state = WorkspaceStateReducer.reduce(WorkspaceScreenState.initial(), WorkspaceAction.toggleHelp());
        assertTrue(state.helpVisible());

        WorkspaceScreenState compact = WorkspaceStateReducer.reduce(state, WorkspaceAction.resize(70, 20));
        assertTrue(compact.compactLayout());

        WorkspaceScreenState full = WorkspaceStateReducer.reduce(compact, WorkspaceAction.resize(120, 35));
        assertFalse(full.compactLayout());
        assertTrue(full.helpVisible());
    }
}
