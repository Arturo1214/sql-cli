package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.Arrays;
import org.junit.Test;

public class Gui2WorkspaceLayoutTest {

    @Test
    public void buildsNvimStyleSplitWorkspaceWidgets() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Arrays.asList("public"),
            "select *\nfrom users",
            Arrays.asList("select 1"),
            "RESULT: 2 rows",
            null,
            Arrays.asList(
                new WorkspaceDashboardRenderer.ConnectionSummary("local", "ORACLE", false),
                new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "POSTGRESQL", true)
            ),
            "Query completed"
        );

        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout().build(state);

        assertEquals("Database Script Workspace", components.window().getTitle());
        assertSame(components.root(), components.window().getComponent());
        assertTrue(components.root() instanceof Panel);
        assertTrue(components.explorer() instanceof ActionListBox);
        assertEquals(13, components.explorer().getItemCount());
        assertTrue(components.sqlEditor() instanceof TextBox);
        assertEquals("select *\nfrom users", components.sqlEditor().getText());
        assertFalse(components.sqlEditor().isReadOnly());
        assertEquals("RESULT: 2 rows", components.resultsText().getText());
        assertEquals("Status: Query completed | Active: reporting [DEV]", components.statusText().getText());
        assertTrue(components.helpText().getText().contains("Ctrl+R run"));
        assertTrue(components.helpText().getText().contains("F1/? help"));
        assertFalse(components.helpText().getText().contains("Ctrl+H help"));
        assertFalse(components.explorer().toString().contains("Tables"));
    }

    @Test
    public void buildsDatabaseNeutralSpanishWorkspaceTitle() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout(new TuiMessages(TuiLanguage.SPANISH)).build(
            stateWithConnection()
        );

        assertEquals("Espacio de Scripts DB", components.window().getTitle());
        assertEquals("Espacio de Scripts DB", components.titleText().getText());
    }

    @Test
    public void buildsEmptyStateWithExplicitPlaceholders() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            null,
            Arrays.<String>asList(),
            "",
            Arrays.<String>asList(),
            null,
            null,
            Arrays.<WorkspaceDashboardRenderer.ConnectionSummary>asList(),
            null
        );

        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout().build(state);

        assertEquals(8, components.explorer().getItemCount());
        assertEquals("", components.sqlEditor().getText());
        assertEquals("No results yet", components.resultsText().getText());
        assertEquals("Status: Ready | Active: none", components.statusText().getText());
    }

    @Test
    public void explorerKeepsVisibleSelectionAtNavigationBoundaries() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout().build(stateWithConnection());
        ActionListBox explorer = components.explorer();
        explorer.takeFocus();

        for (int i = 0; i < 20; i++) {
            assertEquals(Interactable.Result.HANDLED, explorer.handleInput(new KeyStroke(KeyType.ArrowDown)));
        }
        assertEquals(explorer.getItemCount() - 1, explorer.getSelectedIndex());
        assertTrue(explorer.isFocused());

        for (int i = 0; i < 20; i++) {
            assertEquals(Interactable.Result.HANDLED, explorer.handleInput(new KeyStroke(KeyType.ArrowUp)));
        }
        assertEquals(0, explorer.getSelectedIndex());
        assertTrue(explorer.isFocused());
    }

    @Test
    public void buildsFullscreenLayoutWithoutSmallFixedPreferredSizes() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout()
            .build(stateWithConnection(), Gui2WorkspaceLayout.WorkspaceUiActions.noop(), new TerminalSize(120, 36));

        assertEquals(120, components.explorer().getPreferredSize().getColumns());
        assertEquals(120, components.sqlEditor().getPreferredSize().getColumns());
        assertTrue(components.resultsPanel().getPreferredSize().getRows() >= 8);
        assertFalse(new TerminalSize(28, 12).equals(components.explorer().getPreferredSize()));
        assertFalse(new TerminalSize(60, 12).equals(components.sqlEditor().getPreferredSize()));
        assertTrue(components.helpText().getText().contains("Esc close"));
    }

    @Test
    public void resizesBetweenCompactAndRestoredFullscreenProportions() {
        Gui2WorkspaceLayout layout = new Gui2WorkspaceLayout();
        Gui2WorkspaceLayout.WorkspaceComponents components = layout.build(
            stateWithConnection(),
            Gui2WorkspaceLayout.WorkspaceUiActions.noop(),
            new TerminalSize(120, 36)
        );

        layout.resize(components, new TerminalSize(72, 18));
        assertEquals(72, components.explorer().getPreferredSize().getColumns());
        assertEquals(72, components.sqlEditor().getPreferredSize().getColumns());
        assertTrue(components.explorer().getPreferredSize().getRows() >= 4);
        assertTrue(components.sqlEditor().getPreferredSize().getRows() >= 6);
        assertTrue(components.resultsPanel().getPreferredSize().getRows() >= 3);

        layout.resize(components, new TerminalSize(160, 48));
        assertEquals(160, components.explorer().getPreferredSize().getColumns());
        assertEquals(160, components.sqlEditor().getPreferredSize().getColumns());
        assertTrue(components.sqlEditor().getPreferredSize().getRows() >= 24);
        assertTrue(components.resultsPanel().getPreferredSize().getRows() >= 12);
    }

    @Test
    public void keepsMainPanePreferredSizesWithinNarrowTerminalBudget() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout()
            .build(stateWithConnection(), Gui2WorkspaceLayout.WorkspaceUiActions.noop(), new TerminalSize(44, 16));

        assertPaneSizesFit(components, 44);
        assertEquals(44, components.explorer().getPreferredSize().getColumns());
        assertEquals(44, components.sqlEditor().getPreferredSize().getColumns());
        assertEquals(components.resultsPanel().getPreferredSize().getColumns(), components.resultsText().getPreferredSize().getColumns());
    }

    @Test
    public void longResultTextIsBoundedToResultsPaneWidth() {
        WorkspaceDashboardRenderer.DashboardState state = new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Arrays.asList("public"),
            "select 1",
            Arrays.<String>asList(),
            "this_is_a_long_result_value_that_must_not_force_the_results_pane_to_grow_past_the_editor_width",
            null,
            Arrays.asList(new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "POSTGRESQL", true)),
            "Ready"
        );

        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout()
            .build(state, Gui2WorkspaceLayout.WorkspaceUiActions.noop(), new TerminalSize(72, 18));

        assertTrue(components.resultsText().getPreferredSize().getColumns() <= components.resultsPanel().getPreferredSize().getColumns());
    }

    @Test
    public void editorAndResultsUseAvailableWideTerminalColumns() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout()
            .build(stateWithConnection(), Gui2WorkspaceLayout.WorkspaceUiActions.noop(), new TerminalSize(200, 48));

        assertEquals(200, components.explorer().getPreferredSize().getColumns());
        assertEquals(200, components.sqlEditor().getPreferredSize().getColumns());
        assertEquals(200, components.resultsPanel().getPreferredSize().getColumns());
        assertEquals(components.sqlEditor().getPreferredSize().getColumns(), components.resultsPanel().getPreferredSize().getColumns());
        assertEquals(components.resultsPanel().getPreferredSize().getColumns(), components.resultsText().getPreferredSize().getColumns());
    }

    @Test
    public void explorerIncludesKeyboardAccessibleSupportWorkflowActions() {
        RecordingActions actions = new RecordingActions();
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout().build(stateWithConnection(), actions);

        components.explorer().setSelectedIndex(6);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(7);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(8);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(9);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(10);
        components.explorer().runSelectedItem();

        assertEquals("load,save-library,open-library,current,all", actions.calls);
        assertEquals("Load SQL File (F6)", components.explorer().getItemAt(6).toString());
        assertEquals("Save Query to Library (F9)", components.explorer().getItemAt(7).toString());
        assertEquals("Open Query Library (F10)", components.explorer().getItemAt(8).toString());
        assertEquals("Export Current Page CSV (F7)", components.explorer().getItemAt(9).toString());
        assertEquals("Export All Pages CSV (F8)", components.explorer().getItemAt(10).toString());
    }

    @Test
    public void explorerIncludesKeyboardActionsForSelectedConnection() {
        RecordingActions actions = new RecordingActions();
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout().build(stateWithConnection(), actions);

        components.explorer().setSelectedIndex(1);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(2);
        components.explorer().runSelectedItem();
        components.explorer().setSelectedIndex(3);
        components.explorer().runSelectedItem();

        assertEquals("edit,test,delete", actions.calls);
        assertEquals("Edit selected connection", components.explorer().getItemAt(1).toString());
        assertEquals("Test selected connection", components.explorer().getItemAt(2).toString());
        assertEquals("Delete selected connection", components.explorer().getItemAt(3).toString());
    }

    @Test
    public void stackedActiveLayoutGivesEditorResultsAndFooterFullTerminalWidth() {
        Gui2WorkspaceLayout.WorkspaceComponents components = new Gui2WorkspaceLayout()
            .build(stateWithConnection(), Gui2WorkspaceLayout.WorkspaceUiActions.noop(), new TerminalSize(143, 40));

        assertEquals(143, components.root().getPreferredSize().getColumns());
        assertEquals(143, components.explorer().getPreferredSize().getColumns());
        assertEquals(143, components.sqlEditor().getPreferredSize().getColumns());
        assertEquals(143, components.resultsPanel().getPreferredSize().getColumns());
        assertEquals(143, components.resultsText().getPreferredSize().getColumns());
        assertTrue(
            components.statusText().getPreferredSize().getColumns() == 0 || components.statusText().getPreferredSize().getColumns() == 143
        );
        assertTrue(
            components.helpText().getPreferredSize().getColumns() == 0 || components.helpText().getPreferredSize().getColumns() == 143
        );
    }

    private static WorkspaceDashboardRenderer.DashboardState stateWithConnection() {
        return new WorkspaceDashboardRenderer.DashboardState(
            "reporting",
            Arrays.asList("public"),
            "select * from users",
            Arrays.asList("select 1"),
            "RESULT: 2 rows",
            null,
            Arrays.asList(new WorkspaceDashboardRenderer.ConnectionSummary("reporting", "POSTGRESQL", true)),
            "Ready"
        );
    }

    private static final class RecordingActions implements Gui2WorkspaceLayout.WorkspaceUiActions {

        private String calls = "";

        public void selectConnection(int index) {}

        public void openConnectionDialog(DatabaseType databaseType) {}

        public void switchLanguage() {}

        public void openSqlFileDialog() {
            calls = append(calls, "load");
        }

        public void openSaveQueryDialog() {
            calls = append(calls, "save-library");
        }

        public void openQueryLibraryDialog() {
            calls = append(calls, "open-library");
        }

        public void openExportDialog(ExportScope scope) {
            calls = append(calls, scope == ExportScope.CURRENT_PAGE ? "current" : "all");
        }

        public void editSelectedConnection() {
            calls = append(calls, "edit");
        }

        public void testSelectedConnection() {
            calls = append(calls, "test");
        }

        public void deleteSelectedConnection() {
            calls = append(calls, "delete");
        }

        public boolean handleWorkspaceKeyStroke(KeyStroke keyStroke) {
            return false;
        }

        private static String append(String current, String next) {
            return current.length() == 0 ? next : current + "," + next;
        }
    }

    private static void assertPaneSizesFit(Gui2WorkspaceLayout.WorkspaceComponents components, int terminalColumns) {
        int explorerColumns = components.explorer().getPreferredSize().getColumns();
        int rightColumns = components.sqlEditor().getPreferredSize().getColumns();
        assertTrue("explorer width should fit terminal", explorerColumns <= terminalColumns);
        assertTrue("editor width should fit terminal", rightColumns <= terminalColumns);
        assertEquals(rightColumns, components.resultsPanel().getPreferredSize().getColumns());
    }
}
