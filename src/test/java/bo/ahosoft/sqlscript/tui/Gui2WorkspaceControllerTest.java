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
import com.googlecode.lanterna.gui2.ComboBox;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        assertEquals("* [DEV] local [ORACLE]", components.explorer().getItemAt(0).toString());
        assertEquals("  [DEV] reporting [POSTGRESQL]", components.explorer().getItemAt(1).toString());

        components.explorer().setSelectedIndex(1);
        controller.handleEnter();

        assertEquals("reporting", session.activeConnectionName());
        assertEquals("  [DEV] local [ORACLE]", components.explorer().getItemAt(0).toString());
        assertEquals("* [DEV] reporting [POSTGRESQL]", components.explorer().getItemAt(1).toString());
        assertEquals("editor", controller.focusName());
        assertTrue(components.sqlEditor().isFocused());
        assertEquals("Status: Active connection: reporting | Active: reporting [DEV]", components.statusText().getText());
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
        assertEquals(1, countComponents(textGUI.lastAddedWindow.getComponent(), ComboBox.class));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Name"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Environment"));
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
    public void connectionDialogEnvironmentIsConstrainedAndDefaultsToDev() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        controller.openConnectionDialog(DatabaseType.ORACLE);

        ComboBox<?> environment = findComponent(textGUI.lastAddedWindow.getComponent(), ComboBox.class);
        assertNotNull(environment);
        assertEquals(ConnectionEnvironment.DEV, environment.getSelectedItem());
        assertEquals(4, environment.getItemCount());
        assertEquals(ConnectionEnvironment.DEV, environment.getItem(0));
        assertEquals(ConnectionEnvironment.QA, environment.getItem(1));
        assertEquals(ConnectionEnvironment.STAGING, environment.getItem(2));
        assertEquals(ConnectionEnvironment.PROD, environment.getItem(3));
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
        assertEquals("Status: Query completed | Active: local [DEV]", components.statusText().getText());
    }

    @Test
    public void ctrlRExecutesStatementAtMultilineCaretOffset() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select *\nfrom users;\nselect *\nfrom orders;");
        components.sqlEditor().setCaretPosition(5, 3);
        controller.handleKeyStroke(new KeyStroke('r', true, false));

        assertEquals("select *\nfrom orders", executor.statement);
    }

    @Test
    public void editorFocusAcceptsPastedMultilineCharacterEventsAndQuestionMarks() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        controller.handleKeyStroke(new KeyStroke(KeyType.F3));
        String script = "select *\n  from users\n where note = '?';\n\nselect *\n  from orders;";
        for (int i = 0; i < script.length(); i++) {
            char character = script.charAt(i);
            if (character == '\n') {
                assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.Enter)));
            } else {
                assertTrue(controller.handleKeyStroke(new KeyStroke(character, false, false)));
            }
        }

        assertEquals(script, components.sqlEditor().getText());
        assertEquals(null, controller.activeHelpWindow());

        components.sqlEditor().setCaretPosition(5, 4);
        controller.handleKeyStroke(new KeyStroke('r', true, false));

        assertEquals("select *\n  from orders", executor.statement);
    }

    @Test
    public void f5ExecutesStatementAtMultilineCaretOffset() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select *\nfrom users;\nselect *\nfrom orders;");
        components.sqlEditor().setCaretPosition(5, 3);
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals("select *\nfrom orders", executor.statement);
    }

    @Test
    public void dangerousNonProdStatementOpensConfirmationAndDoesNotExecuteBeforeConfirmation() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("update users set enabled = 0 where id = 10");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals(null, executor.statement);
        assertNotNull(textGUI.lastAddedWindow);
        assertEquals("Dangerous SQL confirmation", textGUI.lastAddedWindow.getTitle());
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "UPDATE"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "Type RUN"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Run anyway"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Cancel"));
    }

    @Test
    public void gui2BlocksMutationWithoutTopLevelWhereBeforeOpeningConfirmation() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("update users set enabled = 0");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals(null, executor.statement);
        assertEquals(0, executor.executionCount);
        assertEquals(null, textGUI.lastAddedWindow);
        assertTrue(components.resultsText().getText().contains(SafetyGuard.MISSING_WHERE_MESSAGE));
        assertTrue(components.statusText().getText().contains(SafetyGuard.MISSING_WHERE_MESSAGE));
    }

    @Test
    public void dangerousNonProdConfirmExecutesOnce() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("delete from audit_log where created_at < sysdate - 30");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText("RUN");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Run anyway");

        assertEquals("delete from audit_log where created_at < sysdate - 30", executor.statement);
        assertEquals(1, executor.executionCount);
        assertEquals("RESULT: delete from audit_log where created_at < sysdate - 30", components.resultsText().getText());
        assertEquals("Status: Query completed | Active: local [DEV]", components.statusText().getText());

        clickButton(textGUI.lastAddedWindow.getComponent(), "Run anyway");
        assertEquals(1, executor.executionCount);
    }

    @Test
    public void dangerousNonProdCancelDoesNotExecute() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("drop table old_users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");

        assertEquals(null, executor.statement);
        assertEquals(0, executor.executionCount);
        assertTrue(components.resultsText().getText().contains("Dangerous SQL execution canceled"));
        assertEquals("Status: Dangerous SQL execution canceled | Active: local [DEV]", components.statusText().getText());
    }

    @Test
    public void dangerousProdStatementRequiresExactActiveConnectionName() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(productionSession(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("truncate table prod_orders");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals(null, executor.statement);
        assertEquals("Dangerous PROD SQL confirmation", textGUI.lastAddedWindow.getTitle());
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "PROD"));
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "Type prod-main"));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText("prod-main");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Run anyway");

        assertEquals("truncate table prod_orders", executor.statement);
        assertEquals(1, executor.executionCount);
        assertEquals("Status: Query completed | Active: prod-main [PROD]", components.statusText().getText());
    }

    @Test
    public void dangerousProdMismatchAndCancelDoNotExecute() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(productionSession(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("alter table customers add archived number(1)");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText("RUN");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Run anyway");

        assertEquals(null, executor.statement);
        assertEquals(0, executor.executionCount);
        assertTrue(components.resultsText().getText().contains("Confirmation did not match"));

        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");
        assertEquals(null, executor.statement);
        assertEquals(0, executor.executionCount);
        assertTrue(components.resultsText().getText().contains("Dangerous SQL execution canceled"));
    }

    @Test
    public void selectExecutesWithoutConfirmation() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select * from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertEquals("select * from users", executor.statement);
        assertEquals(1, executor.executionCount);
        assertEquals(null, textGUI.lastAddedWindow);
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
        assertEquals("Status: Ready | Active: local [DEV]", components.statusText().getText());
        assertTrue(components.helpText().getText().contains("F1/? help"));
        assertFalse(components.helpText().getText().contains("Ctrl+H help"));
        assertEquals("Language: English", components.explorer().getItemAt(9).toString());

        components.explorer().setSelectedIndex(9);
        controller.handleEnter();

        assertEquals("Estado: Listo | Activa: local [DEV]", components.statusText().getText());
        assertTrue(components.helpText().getText().contains("F1/? ayuda"));
        assertFalse(components.helpText().getText().contains("Ctrl+H ayuda"));
        assertEquals("Idioma: Espanol", components.explorer().getItemAt(9).toString());
        assertTrue(hasLabel(components.root(), "Editor SQL"));
        assertTrue(hasLabel(components.root(), "Resultados / Logs"));
        assertEquals("SPANISH", controller.languageName());

        components.explorer().setSelectedIndex(9);
        controller.handleEnter();

        assertEquals("Status: Ready | Active: local [DEV]", components.statusText().getText());
        assertEquals("Language: English", components.explorer().getItemAt(9).toString());
        assertTrue(hasLabel(components.root(), "SQL Editor"));
        assertTrue(hasLabel(components.root(), "Results / Logs"));
        assertEquals("ENGLISH", controller.languageName());
    }

    @Test
    public void spanishUiKeepsMetadataCommandKeywordsInEnglish() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.explorer().setSelectedIndex(9);
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
        assertTrue(controller.handleKeyStroke(new KeyStroke('?', false, false)));
        assertTrue(components.sqlEditor().getText().contains("?"));

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
        assertEquals(160, components.explorer().getPreferredSize().getColumns());
        assertEquals(160, components.sqlEditor().getPreferredSize().getColumns());
        assertEquals(160, components.resultsPanel().getPreferredSize().getColumns());
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
        assertEquals("Status: Nothing is ready to execute | Active: local [DEV]", components.statusText().getText());
    }

    @Test
    public void f6OpensSqlPathDialogAndDirectLoadReplacesCleanEditorAtStart() throws Exception {
        File sql = temporaryFolder.newFile("support.sql");
        Files.write(sql.toPath(), "select *\nfrom tickets;".getBytes(StandardCharsets.UTF_8));
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F6)));
        assertEquals("Open SQL File", textGUI.lastAddedWindow.getTitle());

        assertTrue(controller.openSqlFile(sql.getPath(), false));

        assertEquals("select *\nfrom tickets;", components.sqlEditor().getText());
        assertEquals("SQL file loaded: " + sql.getCanonicalPath(), components.resultsText().getText());
        assertEquals(0, components.sqlEditor().getCaretPosition().getColumn());
        assertEquals(0, components.sqlEditor().getCaretPosition().getRow());
    }

    @Test
    public void openSqlPathDialogCancelClosesWithoutChangingEditor() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select dirty;");
        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F6)));

        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");

        assertEquals(0, textGUI.windows.size());
        assertEquals("select dirty;", components.sqlEditor().getText());
    }

    @Test
    public void openSqlPathDialogSaveClosesAfterSuccessfulLoad() throws Exception {
        File sql = temporaryFolder.newFile("close-after-load.sql");
        Files.write(sql.toPath(), "select loaded;".getBytes(StandardCharsets.UTF_8));
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F6)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(sql.getPath());

        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");

        assertEquals(0, textGUI.windows.size());
        assertEquals("select loaded;", components.sqlEditor().getText());
    }

    @Test
    public void dirtySqlLoadRequiresConfirmationAndErrorsKeepEditorContent() throws Exception {
        File sql = temporaryFolder.newFile("replacement.sql");
        Files.write(sql.toPath(), "select 2;".getBytes(StandardCharsets.UTF_8));
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select 1;");

        assertFalse(controller.openSqlFile(sql.getPath(), false));
        assertEquals("select 1;", components.sqlEditor().getText());
        assertTrue(components.resultsText().getText().contains("Replace current editor content"));

        assertTrue(controller.openSqlFile(sql.getPath(), true));
        assertEquals("select 2;", components.sqlEditor().getText());

        components.sqlEditor().setText("select dirty;");
        assertFalse(controller.openSqlFile(new File(temporaryFolder.getRoot(), "missing.sql").getPath(), true));
        assertEquals("select dirty;", components.sqlEditor().getText());
        assertTrue(components.resultsText().getText().contains("File does not exist"));
    }

    @Test
    public void f7AndF8ExportCurrentAndAllPagesWithConfirmationGuardrails() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        controller.handleKeyStroke(new KeyStroke(KeyType.PageDown));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        assertEquals("Export Current Page CSV", textGUI.lastAddedWindow.getTitle());

        File current = new File(temporaryFolder.getRoot(), "current.csv");
        assertTrue(controller.exportResults(current.getPath(), ExportScope.CURRENT_PAGE, false, true));
        assertTrue(new String(Files.readAllBytes(current.toPath()), StandardCharsets.UTF_8).contains("101"));
        assertFalse(new String(Files.readAllBytes(current.toPath()), StandardCharsets.UTF_8).contains("100"));

        File all = new File(temporaryFolder.getRoot(), "all.csv");
        assertFalse(controller.exportResults(all.getPath(), ExportScope.ALL_PAGES, false, false));
        assertFalse(all.exists());
        assertTrue(components.resultsText().getText().contains("Export all pages cancelled"));

        assertTrue(controller.exportResults(all.getPath(), ExportScope.ALL_PAGES, false, true));
        assertTrue(new String(Files.readAllBytes(all.toPath()), StandardCharsets.UTF_8).contains("105"));
    }

    @Test
    public void exportPathDialogCancelClosesWithoutExportingBlankPath() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");

        assertEquals(0, textGUI.windows.size());
        assertFalse(components.resultsText().getText().contains("Path is required"));
        assertFalse(components.resultsText().getText().contains("CSV exported"));
    }

    @Test
    public void exportPathDialogSaveClosesAfterSuccessfulCurrentPageExport() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File target = new File(temporaryFolder.getRoot(), "modal-current.csv");

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(target.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");

        assertEquals(0, textGUI.windows.size());
        assertTrue(target.isFile());
    }

    @Test
    public void f8AllPagesExportConfirmedFromUiSucceedsAndClosesModal() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File target = new File(temporaryFolder.getRoot(), "modal-all.csv");

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F8)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(target.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");

        assertEquals("Export All Pages Confirmation", textGUI.lastAddedWindow.getTitle());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Continue");

        assertEquals(0, textGUI.windows.size());
        String csv = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        assertTrue(csv.contains("1"));
        assertTrue(csv.contains("105"));
        assertTrue(components.resultsText().getText().contains("CSV exported"));
    }

    @Test
    public void f8AllPagesExportCancelFromUiDoesNotExportAndClosesModal() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File target = new File(temporaryFolder.getRoot(), "modal-all-cancelled.csv");

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F8)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(target.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");

        assertEquals(0, textGUI.windows.size());
        assertFalse(target.exists());
        assertTrue(components.resultsText().getText().contains("Export all pages cancelled"));
    }

    @Test
    public void existingExportDestinationTriggersOverwriteConfirmationFromUi() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File existing = temporaryFolder.newFile("modal-existing.csv");
        Files.write(existing.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(existing.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");

        assertEquals("Overwrite Existing File", textGUI.lastAddedWindow.getTitle());
        assertEquals("original", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
        assertTrue(components.resultsText().getText().contains("Overwrite existing file"));
    }

    @Test
    public void overwriteConfirmFromUiExportsAndClosesModal() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File existing = temporaryFolder.newFile("modal-overwrite-confirm.csv");
        Files.write(existing.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(existing.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Overwrite");

        assertEquals(0, textGUI.windows.size());
        assertTrue(new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8).startsWith("ID"));
        assertTrue(components.resultsText().getText().contains("CSV exported"));
    }

    @Test
    public void overwriteCancelFromUiDoesNotModifyFileAndClosesModal() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File existing = temporaryFolder.newFile("modal-overwrite-cancel.csv");
        Files.write(existing.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F7)));
        findComponent(textGUI.lastAddedWindow.getComponent(), TextBox.class).setText(existing.getPath());
        clickButton(textGUI.lastAddedWindow.getComponent(), "Save");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Cancel");

        assertEquals(0, textGUI.windows.size());
        assertEquals("original", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
        assertTrue(components.resultsText().getText().contains("Export overwrite cancelled"));
    }

    @Test
    public void exportOverwriteAndPermissionErrorsAreReportedWithoutSuccess() throws Exception {
        CapturingExecutor executor = new CapturingExecutor(pagedResult());
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select id from users");
        controller.handleKeyStroke(new KeyStroke(KeyType.F5));
        File existing = temporaryFolder.newFile("existing.csv");
        Files.write(existing.toPath(), "original".getBytes(StandardCharsets.UTF_8));

        assertFalse(controller.exportResults(existing.getPath(), ExportScope.CURRENT_PAGE, false, true));
        assertEquals("original", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
        assertTrue(components.resultsText().getText().contains("Target file already exists"));

        assertTrue(controller.exportResults(existing.getPath(), ExportScope.CURRENT_PAGE, true, true));
        assertTrue(new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8).startsWith("ID"));
    }

    @Test
    public void f9OpensTerminalSaveDialogAndSavesCurrentSqlWithoutExecutingIt() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select * from customers");

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F9)));
        assertEquals("Save Query to Library", textGUI.lastAddedWindow.getTitle());
        assertEquals(4, countComponents(textGUI.lastAddedWindow.getComponent(), TextBox.class));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Name"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Description"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Tags"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "Favorite"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Save"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Cancel"));

        assertTrue(controller.saveCurrentQueryToLibrary("Customer List", "support lookup", "support,customers", true, false));

        assertEquals(null, executor.statement);
        assertTrue(components.resultsText().getText().contains("Saved query: customer-list"));
        assertTrue(components.resultsText().getText().contains("Saved SQL may contain sensitive data"));
    }

    @Test
    public void f9SaveDialogCanPersistCurrentSqlAsTemplateWithoutExecutingIt() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select * from customers where id = {{customer_id}} and status = {{status}}");

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F9)));

        assertEquals("Save Query to Library", textGUI.lastAddedWindow.getTitle());
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Save Template"));
        assertTrue(controller.saveCurrentQueryTemplateToLibrary("Customer Template", "support lookup", "support", false, false));
        QueryLibraryEntry entry = session.loadQueryLibraryEntry("customer-template");

        assertEquals(null, executor.statement);
        assertTrue(entry.template());
        assertEquals(Arrays.asList("customer_id", "status"), entry.templateParameters());
        assertTrue(components.resultsText().getText().contains("Saved template: customer-template"));
        assertTrue(components.resultsText().getText().contains("Raw substitution warning"));
    }

    @Test
    public void f10OpensKeyboardLibraryDialogAndSearchListsMetadataOnly() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components.sqlEditor().setText("select * from incidents");
        assertTrue(controller.saveCurrentQueryToLibrary("Incident Search", "support queue", "support,urgent", false, false));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F10)));

        assertEquals("Query Library", textGUI.lastAddedWindow.getTitle());
        assertTrue(
            hasLabelContaining(
                textGUI.lastAddedWindow.getComponent(),
                "incident-search | Incident Search | support, urgent | support queue"
            )
        );
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Search"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Load"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Delete"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Favorite"));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Unfavorite"));

        assertTrue(controller.searchQueryLibrary("urgent"));
        assertTrue(components.resultsText().getText().contains("incident-search | Incident Search | support, urgent | support queue"));
        assertFalse(components.resultsText().getText().contains("select * from incidents"));
    }

    @Test
    public void f10TemplateFillDialogPromptsUniqueParametersLoadsPreviewOnlyAndDoesNotExecute() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);
        RecordingTextGUI textGUI = new RecordingTextGUI();

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.window().setTextGUI(textGUI);
        components
            .sqlEditor()
            .setText("select * from orders where customer_id = {{customer_id}} or approver_id = {{customer_id}} and status = {{status}}");
        assertTrue(controller.saveCurrentQueryTemplateToLibrary("Order Template", "lookup", "support", false, false));

        assertTrue(controller.handleKeyStroke(new KeyStroke(KeyType.F10)));
        assertTrue(hasButton(textGUI.lastAddedWindow.getComponent(), "Fill Template"));
        assertTrue(controller.openTemplateFillDialog("order-template"));

        assertEquals("Fill Template", textGUI.lastAddedWindow.getTitle());
        assertTrue(hasLabelContaining(textGUI.lastAddedWindow.getComponent(), "Raw substitution warning"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "customer_id"));
        assertTrue(hasLabel(textGUI.lastAddedWindow.getComponent(), "status"));
        assertEquals(2, countComponents(textGUI.lastAddedWindow.getComponent(), TextBox.class));

        List<TextBox> fields = findComponents(textGUI.lastAddedWindow.getComponent(), TextBox.class);
        fields.get(0).setText("42");
        fields.get(1).setText("'ACTIVE'");
        clickButton(textGUI.lastAddedWindow.getComponent(), "Preview");

        assertEquals(
            "select * from orders where customer_id = {{customer_id}} or approver_id = {{customer_id}} and status = {{status}}",
            components.sqlEditor().getText()
        );
        assertEquals(null, executor.statement);
        assertTrue(components.resultsText().getText().contains("select * from orders where customer_id = 42"));
        assertTrue(components.resultsText().getText().contains("approver_id = 42"));
        assertTrue(components.resultsText().getText().contains("status = 'ACTIVE'"));

        clickButton(textGUI.lastAddedWindow.getComponent(), "Load");

        assertEquals(
            "select * from orders where customer_id = 42 or approver_id = 42 and status = 'ACTIVE'",
            components.sqlEditor().getText()
        );
        assertEquals(null, executor.statement);
        assertTrue(components.resultsText().getText().contains("Rendered template loaded into editor: order-template"));
    }

    @Test
    public void templateFillRequiresDirtyEditorConfirmationBeforeReplacingBuffer() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        InteractiveWorkspace.Session session = session(executor);
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session);

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select * from users where id = {{user_id}}");
        assertTrue(controller.saveCurrentQueryTemplateToLibrary("User Template", "lookup", "support", false, false));
        components.sqlEditor().setText("select dirty");

        assertFalse(controller.fillTemplateFromLibrary("user-template", Arrays.asList("7"), false));
        assertEquals("select dirty", components.sqlEditor().getText());
        assertTrue(components.resultsText().getText().contains("Replace current editor content"));

        assertTrue(controller.fillTemplateFromLibrary("user-template", Arrays.asList("7"), true));
        assertEquals("select * from users where id = 7", components.sqlEditor().getText());
        assertEquals(null, executor.statement);
    }

    @Test
    public void loadingQueryRequiresDirtyConfirmationAndDoesNotExecuteDangerousSql() throws Exception {
        CapturingExecutor executor = new CapturingExecutor();
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(executor));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("drop table customers");
        assertTrue(controller.saveCurrentQueryToLibrary("Dangerous", "manual review", "risk", false, false));
        components.sqlEditor().setText("select dirty");

        assertFalse(controller.loadQueryFromLibrary("dangerous", false));
        assertEquals("select dirty", components.sqlEditor().getText());
        assertTrue(components.resultsText().getText().contains("Replace current editor content"));

        assertTrue(controller.loadQueryFromLibrary("dangerous", true));
        assertEquals("drop table customers", components.sqlEditor().getText());
        assertEquals(null, executor.statement);
        assertTrue(components.resultsText().getText().contains("Loaded query into editor: dangerous"));
    }

    @Test
    public void deleteAndFavoriteQueryRequireKeyboardActionsAndConfirmation() throws Exception {
        Gui2WorkspaceController controller = new Gui2WorkspaceController(session(new CapturingExecutor()));

        Gui2WorkspaceLayout.WorkspaceComponents components = controller.build();
        components.sqlEditor().setText("select * from tickets");
        assertTrue(controller.saveCurrentQueryToLibrary("Ticket Search", "queue", "support", false, false));

        assertTrue(controller.setQueryLibraryFavorite("ticket-search", true));
        assertTrue(components.resultsText().getText().contains("Updated favorite: ticket-search = true"));
        assertTrue(controller.searchQueryLibrary("ticket"));
        assertTrue(components.resultsText().getText().contains("* ticket-search"));

        assertFalse(controller.deleteQueryFromLibrary("ticket-search", false));
        assertTrue(components.resultsText().getText().contains("Delete saved query: ticket-search?"));
        assertTrue(controller.deleteQueryFromLibrary("ticket-search", true));
        assertTrue(components.resultsText().getText().contains("Deleted query: ticket-search"));
        assertTrue(controller.searchQueryLibrary("ticket"));
        assertTrue(components.resultsText().getText().contains("No saved queries"));
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
        assertEquals(11, components.explorer().getItemCount());
        assertEquals("Status: Connection saved: analytics | Active: analytics [DEV]", components.statusText().getText());
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
        assertEquals(10, components.explorer().getItemCount());
    }

    private InteractiveWorkspace.Session session(CapturingExecutor executor) throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            new EditorStateStore(temporaryFolder.newFile("editor.properties"), 5),
            executor,
            new QueryLibraryStore(temporaryFolder.newFile("query-library.properties"), java.time.Clock.systemUTC())
        );
        session.addConnection("local", new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "ora", "secret"));
        session.addConnection(
            "reporting",
            new ConnectionConfig(DatabaseType.POSTGRESQL, "jdbc:postgresql://localhost:5432/app", "pg", "secret", Arrays.asList("audit"))
        );
        return session;
    }

    private InteractiveWorkspace.Session productionSession(CapturingExecutor executor) throws Exception {
        InteractiveWorkspace.Session session = new InteractiveWorkspace.Session(
            null,
            new EditorStateStore(temporaryFolder.newFile("prod-editor.properties"), 5),
            executor,
            new QueryLibraryStore(temporaryFolder.newFile("prod-query-library.properties"), java.time.Clock.systemUTC())
        );
        session.addConnection(
            "prod-main",
            new ConnectionConfig(
                DatabaseType.ORACLE,
                ConnectionEnvironment.PROD,
                "jdbc:oracle:thin:@prod:1521/PROD",
                "ora",
                "secret",
                Collections.<String>emptyList()
            )
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
        private int executionCount;
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
            this.executionCount++;
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

    private static void clickButton(Component component, String label) throws Exception {
        Button button = findButton(component, label);
        assertNotNull(button);
        java.lang.reflect.Method triggerActions = Button.class.getDeclaredMethod("triggerActions");
        triggerActions.setAccessible(true);
        triggerActions.invoke(button);
    }

    private static Button findButton(Component component, String label) {
        if (component instanceof Button && label.equals(((Button) component).getLabel())) {
            return (Button) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                Button found = findButton(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                T found = findComponent(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> List<T> findComponents(Component component, Class<T> type) {
        List<T> components = new ArrayList<T>();
        collectComponents(component, type, components);
        return components;
    }

    private static <T extends Component> void collectComponents(Component component, Class<T> type, List<T> components) {
        if (type.isInstance(component)) {
            components.add(type.cast(component));
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getChildren()) {
                collectComponents(child, type, components);
            }
        }
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
