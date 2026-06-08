package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.junit.Test;

public class TuiEventRouterTest {

    @Test
    public void mapsControlShortcutsToWorkspaceActions() {
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.CONNECTIONS), TuiEventRouter.route(new KeyStroke(KeyType.F2)));
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.CONNECTIONS), TuiEventRouter.route(new KeyStroke('b', true, false)));
        assertEquals(
            WorkspaceAction.focus(WorkspaceFocus.CONNECTIONS),
            TuiEventRouter.route(new KeyStroke(KeyType.ArrowLeft, true, false))
        );
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.CONNECTIONS), TuiEventRouter.route(new KeyStroke('j', true, false)));
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.EDITOR), TuiEventRouter.route(new KeyStroke(KeyType.F3)));
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.EDITOR), TuiEventRouter.route(new KeyStroke('e', true, false)));
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.EDITOR), TuiEventRouter.route(new KeyStroke(KeyType.ArrowRight, true, false)));
        assertEquals(WorkspaceAction.focus(WorkspaceFocus.EDITOR), TuiEventRouter.route(new KeyStroke('l', true, false)));
        assertEquals(WorkspaceAction.executeCurrentStatement(), TuiEventRouter.route(new KeyStroke('r', true, false)));
        assertEquals(WorkspaceAction.toggleHelp(), TuiEventRouter.route(new KeyStroke('h', true, false)));
    }

    @Test
    public void mapsPanelNavigationKeysToSelectionMovement() {
        assertEquals(WorkspaceAction.moveSelection(1), TuiEventRouter.route(new KeyStroke(KeyType.ArrowDown)));
        assertEquals(WorkspaceAction.moveSelection(1), TuiEventRouter.route(new KeyStroke('j', false, false)));
        assertEquals(WorkspaceAction.moveSelection(-1), TuiEventRouter.route(new KeyStroke(KeyType.ArrowUp)));
        assertEquals(WorkspaceAction.moveSelection(-1), TuiEventRouter.route(new KeyStroke('k', false, false)));
    }

    @Test
    public void mapsEnterAndExitKeysWithoutDoubleEnterExecution() {
        assertEquals(
            WorkspaceAction.selectCurrent(),
            TuiEventRouter.route(new KeyStroke(KeyType.Enter), WorkspaceScreenState.initial().withFocus(WorkspaceFocus.CONNECTIONS), false)
        );
        assertEquals(
            WorkspaceAction.editorEnter(),
            TuiEventRouter.route(new KeyStroke(KeyType.Enter), WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR), false)
        );
        assertEquals(
            WorkspaceAction.advanceFormField(),
            TuiEventRouter.route(
                new KeyStroke(KeyType.Enter),
                WorkspaceScreenState.initial().withCreationForm(DatabaseType.POSTGRESQL),
                false
            )
        );
        assertEquals(WorkspaceAction.editorEnter(), TuiEventRouter.route(new KeyStroke(KeyType.Enter), true));
        assertEquals(WorkspaceAction.exit(), TuiEventRouter.route(new KeyStroke(KeyType.Escape)));
        assertEquals(
            WorkspaceAction.closeModal(),
            TuiEventRouter.route(new KeyStroke(KeyType.Escape), WorkspaceScreenState.initial().withHelpVisible(true), false)
        );
        assertEquals(WorkspaceAction.exit(), TuiEventRouter.route(new KeyStroke('c', true, false)));
    }

    @Test
    public void mapsCtrlRAsOnlyExecutionShortcut() {
        WorkspaceScreenState editor = WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR);

        assertEquals(WorkspaceAction.executeCurrentStatement(), TuiEventRouter.route(new KeyStroke('r', true, false), editor, false));
        assertEquals(WorkspaceAction.editorEnter(), TuiEventRouter.route(new KeyStroke(KeyType.Enter), editor, false));
        assertEquals(WorkspaceAction.editorEnter(), TuiEventRouter.route(new KeyStroke(KeyType.Enter), editor, true));
    }

    @Test
    public void mapsFormTypingKeysToFieldUpdates() {
        WorkspaceScreenState form = WorkspaceScreenState.initial().withCreationForm(DatabaseType.ORACLE);

        assertEquals(WorkspaceAction.updateFormField("a"), TuiEventRouter.route(new KeyStroke('a', false, false), form, false));
        assertEquals(WorkspaceAction.deleteFormCharacter(), TuiEventRouter.route(new KeyStroke(KeyType.Backspace), form, false));
        assertEquals(WorkspaceAction.closeModal(), TuiEventRouter.route(new KeyStroke(KeyType.Escape), form, false));
    }

    @Test
    public void mapsTabAndShiftTabToFocusCycling() {
        assertEquals(WorkspaceAction.nextFocus(), TuiEventRouter.route(new KeyStroke(KeyType.Tab)));
        assertEquals(WorkspaceAction.previousFocus(), TuiEventRouter.route(new KeyStroke(KeyType.ReverseTab)));
    }
}
