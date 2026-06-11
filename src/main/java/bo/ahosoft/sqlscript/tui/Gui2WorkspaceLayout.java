package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.util.List;

public class Gui2WorkspaceLayout {

    private static final TerminalSize DEFAULT_TERMINAL_SIZE = new TerminalSize(120, 36);
    private static final int COMPACT_WIDTH = 80;
    private static final int COMPACT_HEIGHT = 24;
    private static final int CHROME_ROWS = 6;
    private static final int MIN_CONTENT_ROWS = 12;
    private TuiMessages messages;

    public Gui2WorkspaceLayout() {
        this(new TuiMessages(TuiLanguage.ENGLISH));
    }

    public Gui2WorkspaceLayout(TuiMessages messages) {
        this.messages = messages == null ? new TuiMessages(TuiLanguage.ENGLISH) : messages;
    }

    public void setMessages(TuiMessages messages) {
        this.messages = messages == null ? new TuiMessages(TuiLanguage.ENGLISH) : messages;
    }

    public WorkspaceComponents build(WorkspaceDashboardRenderer.DashboardState state) {
        return build(state, WorkspaceUiActions.noop());
    }

    public WorkspaceComponents build(WorkspaceDashboardRenderer.DashboardState state, WorkspaceUiActions actions) {
        return build(state, actions, DEFAULT_TERMINAL_SIZE);
    }

    public WorkspaceComponents build(
        WorkspaceDashboardRenderer.DashboardState state,
        WorkspaceUiActions actions,
        TerminalSize terminalSize
    ) {
        LayoutSize layoutSize = LayoutSize.from(terminalSize);
        BasicWindow window = windowFor(actions);
        window.setHints(java.util.Arrays.asList(Window.Hint.FULL_SCREEN));
        window.setCloseWindowWithEscape(true);

        ActionListBox explorer = explorerFor(state, actions, layoutSize.explorerSize());
        TextBox sqlEditor = new TextBox(layoutSize.editorSize(), valueOrEmpty(state.buffer()), TextBox.Style.MULTI_LINE);
        sqlEditor.setCaretWarp(false);
        sqlEditor.setHorizontalFocusSwitching(false);
        sqlEditor.setVerticalFocusSwitching(false);
        Label resultsText = new Label(resultText(state, messages));
        resultsText.setLabelWidth(Integer.valueOf(layoutSize.resultsTextSize().getColumns()));
        resultsText.setPreferredSize(layoutSize.resultsTextSize());
        Label statusText = new Label(messages.statusText(state));
        statusText.setPreferredSize(layoutSize.footerSize());
        Label helpText = new Label(messages.helpHint());
        helpText.setPreferredSize(layoutSize.footerSize());

        Panel editorPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        Label editorTitle = new Label(messages.sqlEditorTitle());
        editorPanel.addComponent(editorTitle);
        editorPanel.addComponent(sqlEditor);

        Panel resultsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        resultsPanel.setPreferredSize(layoutSize.resultsSize());
        Label resultsTitle = new Label(messages.resultsLogsTitle());
        resultsPanel.addComponent(resultsTitle);
        resultsPanel.addComponent(resultsText);

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        Label titleText = new Label(messages.windowTitle());
        titleText.setPreferredSize(layoutSize.footerSize());
        root.addComponent(titleText);
        root.addComponent(explorer);
        root.addComponent(editorPanel);
        root.addComponent(resultsPanel);
        root.addComponent(statusText);
        root.addComponent(helpText);
        root.setPreferredSize(layoutSize.rootSize());

        window.setComponent(root);
        return new WorkspaceComponents(
            window,
            root,
            explorer,
            sqlEditor,
            resultsPanel,
            resultsText,
            statusText,
            helpText,
            titleText,
            editorTitle,
            resultsTitle
        );
    }

    public void resize(WorkspaceComponents components, TerminalSize terminalSize) {
        LayoutSize layoutSize = LayoutSize.from(terminalSize);
        components.root().setPreferredSize(layoutSize.rootSize());
        components.explorer().setPreferredSize(layoutSize.explorerSize());
        components.sqlEditor().setPreferredSize(layoutSize.editorSize());
        components.resultsPanel().setPreferredSize(layoutSize.resultsSize());
        components.resultsText().setLabelWidth(Integer.valueOf(layoutSize.resultsTextSize().getColumns()));
        components.resultsText().setPreferredSize(layoutSize.resultsTextSize());
        components.statusText().setPreferredSize(layoutSize.footerSize());
        components.helpText().setPreferredSize(layoutSize.footerSize());
        components.titleText().setPreferredSize(layoutSize.footerSize());
        components.window().invalidate();
    }

    private ActionListBox explorerFor(
        WorkspaceDashboardRenderer.DashboardState state,
        WorkspaceUiActions actions,
        TerminalSize preferredSize
    ) {
        ActionListBox explorer = new ClampedActionListBox(preferredSize);
        populateExplorer(explorer, state, actions);
        return explorer;
    }

    private BasicWindow windowFor(final WorkspaceUiActions actions) {
        return new BasicWindow(messages.windowTitle()) {
            @Override
            public boolean handleInput(KeyStroke keyStroke) {
                if (actions.handleWorkspaceKeyStroke(keyStroke)) {
                    return true;
                }
                return super.handleInput(keyStroke);
            }
        };
    }

    private void populateExplorer(ActionListBox explorer, WorkspaceDashboardRenderer.DashboardState state, WorkspaceUiActions actions) {
        List<WorkspaceDashboardRenderer.ConnectionSummary> connections = state.connections();
        int index = 0;
        for (WorkspaceDashboardRenderer.ConnectionSummary connection : connections) {
            final int selectedIndex = index;
            explorer.addItem(
                connectionLabel(connection),
                new Runnable() {
                    public void run() {
                        actions.selectConnection(selectedIndex);
                    }
                }
            );
            index++;
        }
        if (!connections.isEmpty()) {
            explorer.addItem(
                messages.editSelectedConnectionAction(),
                new Runnable() {
                    public void run() {
                        actions.editSelectedConnection();
                    }
                }
            );
            explorer.addItem(
                messages.testSelectedConnectionAction(),
                new Runnable() {
                    public void run() {
                        actions.testSelectedConnection();
                    }
                }
            );
            explorer.addItem(
                messages.deleteSelectedConnectionAction(),
                new Runnable() {
                    public void run() {
                        actions.deleteSelectedConnection();
                    }
                }
            );
        }
        explorer.addItem(messages.newOracleConnection(), dialogAction(actions, DatabaseType.ORACLE));
        explorer.addItem(messages.newPostgresqlConnection(), dialogAction(actions, DatabaseType.POSTGRESQL));
        explorer.addItem(
            messages.loadSqlFileAction(),
            new Runnable() {
                public void run() {
                    actions.openSqlFileDialog();
                }
            }
        );
        explorer.addItem(
            messages.saveQueryToLibraryAction(),
            new Runnable() {
                public void run() {
                    actions.openSaveQueryDialog();
                }
            }
        );
        explorer.addItem(
            messages.openQueryLibraryAction(),
            new Runnable() {
                public void run() {
                    actions.openQueryLibraryDialog();
                }
            }
        );
        explorer.addItem(
            messages.exportCurrentPageAction(),
            new Runnable() {
                public void run() {
                    actions.openExportDialog(ExportScope.CURRENT_PAGE);
                }
            }
        );
        explorer.addItem(
            messages.exportAllPagesAction(),
            new Runnable() {
                public void run() {
                    actions.openExportDialog(ExportScope.ALL_PAGES);
                }
            }
        );
        explorer.addItem(
            messages.languageAction(),
            new Runnable() {
                public void run() {
                    actions.switchLanguage();
                }
            }
        );
    }

    private static Runnable dialogAction(final WorkspaceUiActions actions, final DatabaseType databaseType) {
        return new Runnable() {
            public void run() {
                actions.openConnectionDialog(databaseType);
            }
        };
    }

    private static String connectionLabel(WorkspaceDashboardRenderer.ConnectionSummary connection) {
        String marker = connection.active() ? "* " : "  ";
        String prodMarker = "PROD".equalsIgnoreCase(connection.environment()) ? "!! PROD !! " : "";
        return marker + prodMarker + "[" + connection.environment() + "] " + connection.name() + " [" + connection.databaseType() + "]";
    }

    public static String resultText(WorkspaceDashboardRenderer.DashboardState state) {
        return resultText(state, new TuiMessages(TuiLanguage.ENGLISH));
    }

    public static String resultText(WorkspaceDashboardRenderer.DashboardState state, TuiMessages messages) {
        if (!isBlank(state.lastError())) {
            return messages.localizeResultText(state.lastError());
        }
        return messages.localizeResultText(state.lastResult());
    }

    public static String statusText(WorkspaceDashboardRenderer.DashboardState state) {
        return new TuiMessages(TuiLanguage.ENGLISH).statusText(state);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String valueOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public final class WorkspaceComponents {

        private final BasicWindow window;
        private final Panel root;
        private final ActionListBox explorer;
        private final TextBox sqlEditor;
        private final Panel resultsPanel;
        private final Label resultsText;
        private final Label statusText;
        private final Label helpText;
        private final Label titleText;
        private final Label editorTitle;
        private final Label resultsTitle;

        public WorkspaceComponents(
            BasicWindow window,
            Panel root,
            ActionListBox explorer,
            TextBox sqlEditor,
            Panel resultsPanel,
            Label resultsText,
            Label statusText,
            Label helpText,
            Label titleText,
            Label editorTitle,
            Label resultsTitle
        ) {
            this.window = window;
            this.root = root;
            this.explorer = explorer;
            this.sqlEditor = sqlEditor;
            this.resultsPanel = resultsPanel;
            this.resultsText = resultsText;
            this.statusText = statusText;
            this.helpText = helpText;
            this.titleText = titleText;
            this.editorTitle = editorTitle;
            this.resultsTitle = resultsTitle;
        }

        public BasicWindow window() {
            return window;
        }

        public Panel root() {
            return root;
        }

        public ActionListBox explorer() {
            return explorer;
        }

        public TextBox sqlEditor() {
            return sqlEditor;
        }

        public Panel resultsPanel() {
            return resultsPanel;
        }

        public Label resultsText() {
            return resultsText;
        }

        public Label statusText() {
            return statusText;
        }

        public Label helpText() {
            return helpText;
        }

        public Label titleText() {
            return titleText;
        }

        public void refresh(WorkspaceDashboardRenderer.DashboardState state) {
            window.setTitle(Gui2WorkspaceLayout.this.messages.windowTitle());
            resultsText.setText(Gui2WorkspaceLayout.resultText(state, Gui2WorkspaceLayout.this.messages));
            statusText.setText(Gui2WorkspaceLayout.this.messages.statusText(state));
            helpText.setText(Gui2WorkspaceLayout.this.messages.helpHint());
            titleText.setText(Gui2WorkspaceLayout.this.messages.windowTitle());
            editorTitle.setText(Gui2WorkspaceLayout.this.messages.sqlEditorTitle());
            resultsTitle.setText(Gui2WorkspaceLayout.this.messages.resultsLogsTitle());
        }

        public void rebuildExplorer(WorkspaceDashboardRenderer.DashboardState state, WorkspaceUiActions actions) {
            explorer.clearItems();
            Gui2WorkspaceLayout.this.populateExplorer(explorer, state, actions);
            if (explorer.getItemCount() > 0) {
                explorer.setSelectedIndex(Math.max(0, Math.min(explorer.getSelectedIndex(), explorer.getItemCount() - 1)));
            }
        }
    }

    private static final class LayoutSize {

        private final int columns;
        private final int rows;
        private final int contentRows;
        private final int explorerRows;
        private final int editorRows;
        private final int resultsRows;

        private LayoutSize(TerminalSize terminalSize) {
            int terminalColumns = Math.max(1, terminalSize.getColumns());
            int terminalRows = Math.max(1, terminalSize.getRows());
            boolean compact = terminalColumns < COMPACT_WIDTH || terminalRows < COMPACT_HEIGHT;
            this.columns = terminalColumns;
            this.rows = terminalRows;
            this.contentRows = Math.max(MIN_CONTENT_ROWS, terminalRows - CHROME_ROWS);
            this.explorerRows = compact ? 4 : 6;
            this.editorRows = compact ? Math.max(6, contentRows / 2) : Math.max(6, ((contentRows * 2) / 3) - 1);
            this.resultsRows = Math.max(3, contentRows - editorRows - 2);
        }

        public static LayoutSize from(TerminalSize terminalSize) {
            return new LayoutSize(terminalSize == null ? DEFAULT_TERMINAL_SIZE : terminalSize);
        }

        public TerminalSize rootSize() {
            return new TerminalSize(columns, rows);
        }

        public TerminalSize explorerSize() {
            return new TerminalSize(columns, explorerRows);
        }

        public TerminalSize editorSize() {
            return new TerminalSize(columns, editorRows);
        }

        public TerminalSize resultsSize() {
            return new TerminalSize(columns, resultsRows);
        }

        public TerminalSize resultsTextSize() {
            return new TerminalSize(columns, Math.max(1, resultsRows - 1));
        }

        public TerminalSize footerSize() {
            return new TerminalSize(columns, 1);
        }
    }

    public interface WorkspaceUiActions {
        void selectConnection(int index);

        void openConnectionDialog(DatabaseType databaseType);

        void switchLanguage();

        void openSqlFileDialog();

        void openSaveQueryDialog();

        void openQueryLibraryDialog();

        void openExportDialog(ExportScope scope);

        void editSelectedConnection();

        void testSelectedConnection();

        void deleteSelectedConnection();

        boolean handleWorkspaceKeyStroke(KeyStroke keyStroke);

        public static WorkspaceUiActions noop() {
            return new WorkspaceUiActions() {
                public void selectConnection(int index) {}

                public void openConnectionDialog(DatabaseType databaseType) {}

                public void switchLanguage() {}

                public void openSqlFileDialog() {}

                public void openSaveQueryDialog() {}

                public void openQueryLibraryDialog() {}

                public void openExportDialog(ExportScope scope) {}

                public void editSelectedConnection() {}

                public void testSelectedConnection() {}

                public void deleteSelectedConnection() {}

                public boolean handleWorkspaceKeyStroke(KeyStroke keyStroke) {
                    return false;
                }
            };
        }
    }

    private static final class ClampedActionListBox extends ActionListBox {

        public ClampedActionListBox(TerminalSize preferredSize) {
            super(preferredSize);
        }

        @Override
        public Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
            Interactable.Result result = super.handleKeyStroke(keyStroke);
            if (keyStroke == null || getItemCount() == 0) {
                return result;
            }
            KeyType keyType = keyStroke.getKeyType();
            if (keyType == KeyType.ArrowDown && result == Interactable.Result.MOVE_FOCUS_DOWN) {
                setSelectedIndex(getItemCount() - 1);
                return Interactable.Result.HANDLED;
            }
            if (keyType == KeyType.ArrowUp && result == Interactable.Result.MOVE_FOCUS_UP) {
                setSelectedIndex(0);
                return Interactable.Result.HANDLED;
            }
            return result;
        }
    }
}
