package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.graphics.Theme;
import com.googlecode.lanterna.gui2.BasePane;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Container;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUI;
import com.googlecode.lanterna.gui2.TextGUIThread;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowManager;
import com.googlecode.lanterna.gui2.WindowPostRenderer;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class Gui2WorkspaceControllerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void enterOnConnectionItemSelectsConnectionAndRefreshesStatus() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        assertEquals("* local [ORACLE]", components.explorer().getItemAt(0).toString());
        assertEquals("  reporting [POSTGRESQL]", components.explorer().getItemAt(1).toString());

        components.explorer().setSelectedIndex(1);
        controller.handleEnter();

        assertEquals("reporting", session.activeConnectionName());
        assertEquals("  local [ORACLE]", components.explorer().getItemAt(0).toString());
        assertEquals("* reporting [POSTGRESQL]", components.explorer().getItemAt(1).toString());
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());
        assertEquals("Status: Active connection: reporting | Active: reporting", components.statusText().getText());
    }

    @Test
    public void enterOnCreationActionOpensConnectionDialogWithoutMetadataMenuItems() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.explorer().setSelectedIndex(2);
        controller.handleEnter();

        assertEquals(null, executor.statement);
        assertTrue(components.resultsText().getText().contains("Oracle connection wizard"));
    }

    @Test
    public void enterOnNewConnectionActionsOpensActualEditableDialogWindow() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.explorer().setSelectedIndex(2);
        controller.handleEnter();

        assertNotNull(textGUI.lastAddedWindow);
        assertEquals("New Oracle connection", textGUI.lastAddedWindow.getTitle());
        assertEquals(5, countComponents(textGUI.lastAddedWindow.getComponent(), TextBox.class));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Name"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Save"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Cancel"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Back"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Defaults"));

        components.explorer().setSelectedIndex(3);
        controller.handleEnter();

        assertEquals("New PostgreSQL connection", textGUI.lastAddedWindow.getTitle());
        assertEquals(DatabaseType.POSTGRESQL, controller.activeConnectionDialog().databaseType());
    }

    @Test
    public void ctrlRExecutesCurrentEditorStatementAtCaretAndRefreshesResults() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select * from users; select * from orders");
        components.sqlEditor().setCaretPosition(8);
        controller.handleKeyStroke(new KeyStroke('r', true, false));

        assertEquals("select * from users", executor.statement);
        assertEquals("RESULT: select * from users", components.resultsText().getText());
        assertEquals("Status: Query completed | Active: local", components.statusText().getText());
    }

    @Test
    public void resultPaneSupportsScrollAndPageNavigationKeys() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        controller.handleKeyStroke(new KeyStroke(KeyType.Tab));

        assertEquals("results", controller.focusName());
        assertTrue(components.resultsText().getText().contains("Rows 1-100"));

        controller.handleKeyStroke(new KeyStroke(KeyType.ArrowDown));
        assertFalse(components.resultsText().getText().contains("SQL #1: select id from users"));

        controller.handleKeyStroke(new KeyStroke(KeyType.PageDown));
        assertTrue(components.resultsText().getText().contains("Rows 101-105"));
        assertTrue(components.resultsText().getText().contains("105"));

        controller.handleKeyStroke(new KeyStroke(KeyType.PageUp));
        assertTrue(components.resultsText().getText().contains("Rows 1-100"));
    }

    @Test
    public void resultPaneSupportsHorizontalScrollOnlyWhenFocused() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(widePagedResult());
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build(new com.googlecode.lanterna.TerminalSize(80, 36));
        components.sqlEditor().setText("select * from wide_table");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        String editorFocused = components.resultsText().getText();
        assertTrue(editorFocused.contains("COL_0"));
        assertFalse(editorFocused.contains("COL_10"));
        controller.handleKeyStroke(new KeyStroke(KeyType.ArrowRight));
        assertEquals(editorFocused, components.resultsText().getText());

        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        controller.handleKeyStroke(new KeyStroke(KeyType.Tab));
        assertEquals("results", controller.focusName());
        controller.handleKeyStroke(new KeyStroke(KeyType.ArrowRight));
        String scrolledRight = components.resultsText().getText();

        assertFalse(scrolledRight.contains("COL_01"));
        assertTrue(scrolledRight.contains("COL_09"));
        assertTrue(scrolledRight.contains("Col offset"));
        assertTrue(scrolledRight.contains("←/→"));

        controller.handleKeyStroke(new KeyStroke(KeyType.ArrowLeft));
        assertTrue(components.resultsText().getText().contains("COL_01"));
    }

    @Test
    public void questionMarkShowsHelpAndEscRequestsCloseWithoutExecutingSql() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        controller.handleKeyStroke(new KeyStroke('?', false, false));
        controller.handleKeyStroke(new KeyStroke(KeyType.Escape));

        assertNotNull(textGUI.lastAddedWindow);
        assertEquals("Help", textGUI.lastAddedWindow.getTitle());
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "F1/?: open help"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "F2/Ctrl+B"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "F3/Ctrl+E"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "F5/Ctrl+R"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "PageDown/PageUp"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "tables"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "describe <table>"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "indexes <table>"));
        assertTrue(controller.closeRequested());
        assertFalse(components.window().isVisible());
        assertEquals(null, executor.statement);
    }

    @Test
    public void languageActionSwitchesEnglishAndSpanishUiCopyInSession() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        assertEquals("Status: Ready | Active: local", components.statusText().getText());
        assertTrue(components.helpText().getText().contains("F1/? help"));
        assertFalse(components.helpText().getText().contains("Ctrl+H help"));
        assertEquals("Language: English", components.explorer().getItemAt(4).toString());

        components.explorer().setSelectedIndex(4);
        controller.handleEnter();

        assertEquals("Estado: Listo | Activa: local", components.statusText().getText());
        assertTrue(components.helpText().getText().contains("F1/? ayuda"));
        assertFalse(components.helpText().getText().contains("Ctrl+H ayuda"));
        assertEquals("Idioma: Espanol", components.explorer().getItemAt(4).toString());
        assertTrue(hasLabel(components.root(), "Editor SQL"));
        assertTrue(hasLabel(components.root(), "Resultados / Logs"));
        assertEquals("SPANISH", controller.languageName());

        components.explorer().setSelectedIndex(4);
        controller.handleEnter();

        assertEquals("Status: Ready | Active: local", components.statusText().getText());
        assertEquals("Language: English", components.explorer().getItemAt(4).toString());
        assertTrue(hasLabel(components.root(), "SQL Editor"));
        assertTrue(hasLabel(components.root(), "Results / Logs"));
        assertEquals("ENGLISH", controller.languageName());
    }

    @Test
    public void spanishUiKeepsMetadataCommandKeywordsInEnglish() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.explorer().setSelectedIndex(4);
        controller.handleEnter();

        components.sqlEditor().setText("tables user");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertTrue(executor.statement.contains("user_tables"));

        components.sqlEditor().setText("describe customers");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertTrue(executor.statement.contains("upper('customers')"));

        components.sqlEditor().setText("indexes users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertTrue(executor.statement.contains("user_indexes"));
        assertTrue(components.statusText().getText().contains("Estado:"));
    }

    @Test
    public void plainTypingIsNotHandledByGlobalControllerShortcuts() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        controller.build();

        assertFalse(controller.handleKeyStroke(new KeyStroke('x', false, false)));
    }

    @Test
    public void tabShiftTabFunctionKeysAndCtrlAlternativesRouteKeyboardFocus() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("tables");
        assertEquals("explorer", controller.focusName());

        controller.handleKeyStroke(new KeyStroke(KeyType.Tab));
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());

        controller.handleKeyStroke(new KeyStroke(KeyType.Tab));
        assertEquals("results", controller.focusName());
        controller.handleKeyStroke(new KeyStroke(KeyType.ReverseTab));
        assertEquals("editor", controller.focusName());

        controller.handleKeyStroke(new KeyStroke(KeyType.F2));
        assertEquals("explorer", controller.focusName());
        assertTrue(components.explorer().isFocused());
        controller.handleKeyStroke(new KeyStroke('b', true, false));
        assertEquals("explorer", controller.focusName());

        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());
        controller.handleKeyStroke(new KeyStroke('e', true, false));
        assertEquals("editor", controller.focusName());

        controller.handleKeyStroke(new KeyStroke(KeyType.F1));
        assertTrue(components.resultsText().getText().contains("Keyboard shortcuts"));

        controller.handleKeyStroke(new KeyStroke('?', false, false));
        assertTrue(components.resultsText().getText().contains("Keyboard shortcuts"));

        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        assertFalse(controller.handleKeyStroke(new KeyStroke('?', false, false)));

        components.sqlEditor().setText("select * from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertEquals("select * from users", executor.statement);
        controller.handleKeyStroke(new KeyStroke('r', true, false));
        assertEquals("select * from users", executor.statement);
    }

    @Test
    public void windowRoutesFunctionKeysTabAndEnterThroughWorkspaceController() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.explorer().setSelectedIndex(1);

        assertTrue(components.window().handleInput(new KeyStroke('h', true, false)));
        assertEquals("Help", textGUI.lastAddedWindow.getTitle());

        textGUI.lastAddedWindow = null;
        assertTrue(components.window().handleInput(new KeyStroke('?', false, false)));
        assertEquals("Help", textGUI.lastAddedWindow.getTitle());

        textGUI.lastAddedWindow = null;
        assertTrue(components.window().handleInput(new KeyStroke(Character.valueOf('\b'), false, false)));
        assertEquals("Help", textGUI.lastAddedWindow.getTitle());

        textGUI.lastAddedWindow = null;
        assertTrue(components.window().handleInput(new KeyStroke(KeyType.F1)));
        assertEquals("Help", textGUI.lastAddedWindow.getTitle());

        assertTrue(components.window().handleInput(new KeyStroke(KeyType.F3)));
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());
        textGUI.lastAddedWindow = null;
        components.window().handleInput(new KeyStroke('?', false, false));
        assertEquals(null, textGUI.lastAddedWindow);

        assertTrue(components.window().handleInput(new KeyStroke(KeyType.F2)));
        assertEquals("explorer", controller.focusName());
        assertTrue(components.explorer().isFocused());

        assertTrue(components.window().handleInput(new KeyStroke(KeyType.Enter)));
        assertEquals("reporting", session.activeConnectionName());
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());

        assertTrue(components.window().handleInput(new KeyStroke(KeyType.Tab)));
        assertEquals("results", controller.focusName());
        assertTrue(components.window().handleInput(new KeyStroke(KeyType.ReverseTab)));
        assertEquals("editor", controller.focusName());
    }

    @Test
    public void resizeRefreshesResponsiveComponentSizes() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build(new com.googlecode.lanterna.TerminalSize(120, 36));
        controller.resize(new com.googlecode.lanterna.TerminalSize(160, 48));

        assertEquals(new com.googlecode.lanterna.TerminalSize(160, 48), components.root().getPreferredSize());
        assertEquals(new com.googlecode.lanterna.TerminalSize(40, 42), components.explorer().getPreferredSize());
        assertEquals(new com.googlecode.lanterna.TerminalSize(116, 27), components.sqlEditor().getPreferredSize());
    }

    @Test
    public void editorMetadataCommandsRunThroughExecuteShortcutOnly() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("tables user");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertTrue(executor.statement.contains("user_tables"));
        assertTrue(executor.statement.contains("upper('%user%')"));

        components.sqlEditor().setText("describe customers");
        controller.handleKeyStroke(new KeyStroke('r', true, false));
        assertTrue(executor.statement.contains("upper('customers')"));

        components.sqlEditor().setText("indexes users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        assertTrue(executor.statement.contains("user_indexes"));
        assertTrue(executor.statement.contains("upper('users')"));
    }

    @Test
    public void executeShortcutRejectsEmptyBufferWithoutRunningExecutor() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("   ");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals(null, executor.statement);
        assertEquals("Nothing is ready to execute", components.resultsText().getText());
        assertEquals("Status: Nothing is ready to execute | Active: local", components.statusText().getText());
    }

    @Test
    public void submitConnectionDialogCreatesConnectionAndRebuildsExplorerStatus() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        Gui2ConnectionDialog.Result result = controller.submitConnection(
            new Gui2ConnectionDialog.Request(
                DatabaseType.POSTGRESQL,
                "analytics",
                "jdbc:postgresql://localhost:5432/app",
                "pg",
                "secret",
                Collections.<String>emptyList(),
                Arrays.asList("public")
            )
        );

        assertTrue(result.created());
        assertEquals("pg", result.config().username());
        assertEquals(6, components.explorer().getItemCount());
        assertEquals("Status: Connection saved: analytics | Active: analytics", components.statusText().getText());
    }

    @Test
    public void openConnectionDialogCreatesWizardAndCancelReturnsToWorkspaceWithoutSaving() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        controller.openConnectionDialog(DatabaseType.POSTGRESQL);
        Gui2ConnectionDialog.Form form = controller.activeConnectionDialog();
        assertEquals(DatabaseType.POSTGRESQL, form.databaseType());
        assertTrue(components.resultsText().getText().contains("PostgreSQL connection wizard"));

        Gui2ConnectionDialog.Result cancelled = controller.cancelActiveConnectionDialog();

        assertFalse(cancelled.created());
        assertEquals("Connection creation cancelled", components.resultsText().getText());
        assertEquals(5, components.explorer().getItemCount());
    }

    private InteractiveWorkspace.Session session(CapturingExecutor executor) throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            new EditorStateStore(temporaryFolder.newFile("editor.properties"), 5),
            executor
        );
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        session.addConnection(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );
        return session;
    }

    private static SqlExecutionResult pagedResult() {
        List<List<String>> rows = new ArrayList<List<String>>();
        for (int i = 1; i <= 105; i++) {
            rows.add(Arrays.asList(String.valueOf(i)));
        }
        return SqlExecutionResult.paged("SQL #1: select id from users", Arrays.asList("ID"), rows);
    }

    private static SqlExecutionResult widePagedResult() {
        List<String> headers = new ArrayList<String>();
        List<String> row = new ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            headers.add(String.format("COL_%02d", Integer.valueOf(i)));
            row.add(String.format("VALUE_%02d", Integer.valueOf(i)));
        }
        return SqlExecutionResult.paged("SQL #1: select * from wide_table", headers, Arrays.asList(row));
    }

    private static final class CapturingExecutor implements InteractiveWorkspace.WorkspaceSqlExecutor {

        private String statement;
        private final SqlExecutionResult result;

        CapturingExecutor() {
            this(null);
        }

        CapturingExecutor(SqlExecutionResult result) {
            this.result = result;
        }

        @Override
        public SqlExecutionResult executeSingle(ConnectionConfig config, String statement) throws SQLException {
            this.statement = statement;
            if (result != null) {
                return result;
            }
            return new SqlExecutionResult("RESULT: " + statement);
        }
    }

    private static int countComponents(Component component, Class<?> type) {
        int count = type.isInstance(component) ? 1 : 0;
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                count += countComponents(child, type);
            }
        }
        return count;
    }

    private static boolean hasLabel(Component component, String text) {
        if (component instanceof Label && text.equals(((Label) component).getText())) {
            return true;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                if (hasLabel(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasLabelContaining(Component component, String text) {
        if (component instanceof Label && ((Label) component).getText().contains(text)) {
            return true;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                if (hasLabelContaining(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasButton(Component component, String label) {
        if (component instanceof Button && label.equals(((Button) component).getLabel())) {
            return true;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                if (hasButton(child, label)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class RecordingTextGUI implements WindowBasedTextGUI {

        private final List<Window> windows = new ArrayList<Window>();
        private Window lastAddedWindow;

        public WindowBasedTextGUI addWindow(Window window) {
            lastAddedWindow = window;
            windows.add(window);
            window.setTextGUI(this);
            return this;
        }

        public WindowBasedTextGUI addWindowAndWait(Window window) {
            return addWindow(window);
        }

        public WindowBasedTextGUI removeWindow(Window window) {
            windows.remove(window);
            return this;
        }

        public Collection<Window> getWindows() {
            return windows;
        }

        public WindowBasedTextGUI setActiveWindow(Window window) {
            return this;
        }

        public Window getActiveWindow() {
            return lastAddedWindow;
        }

        public WindowManager getWindowManager() {
            return null;
        }

        public BasePane getBackgroundPane() {
            return null;
        }

        public WindowPostRenderer getWindowPostRenderer() {
            return null;
        }

        public WindowBasedTextGUI moveToTop(Window window) {
            return this;
        }

        public WindowBasedTextGUI cycleActiveWindow(boolean reverse) {
            return this;
        }

        public void waitForWindowToClose(Window window) {}

        public Theme getTheme() {
            return null;
        }

        public void setTheme(Theme theme) {}

        public boolean processInput() throws IOException {
            return false;
        }

        public Screen getScreen() {
            return null;
        }

        public void updateScreen() throws IOException {}

        public boolean isPendingUpdate() {
            return false;
        }

        public void setVirtualScreenEnabled(boolean virtualScreenEnabled) {}

        public TextGUIThread getGUIThread() {
            return null;
        }

        public Interactable getFocusedInteractable() {
            return null;
        }

        public void addListener(TextGUI.Listener listener) {}

        public void removeListener(TextGUI.Listener listener) {}
    }
}
