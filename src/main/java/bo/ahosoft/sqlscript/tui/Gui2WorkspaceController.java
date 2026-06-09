package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Gui2WorkspaceController implements Gui2WorkspaceLayout.WorkspaceUiActions {

    private final InteractiveWorkspace.Session session;
    private final Gui2WorkspaceLayout layout;
    private Gui2WorkspaceLayout.WorkspaceComponents components;
    private Gui2ConnectionDialog.Form activeConnectionDialog;
    private BasicWindow activeConnectionDialogWindow;
    private BasicWindow activeHelpWindow;
    private boolean closeRequested;
    private FocusTarget focusTarget = FocusTarget.EXPLORER;
    private int resultScrollOffset;
    private int resultHorizontalOffset;
    private TuiLanguage language = TuiLanguage.ENGLISH;
    private String editorCleanSnapshot;
    private final SupportWorkflowFileService fileService = new SupportWorkflowFileService();
    private final CsvResultExporter csvExporter = new CsvResultExporter();

    public Gui2WorkspaceController(InteractiveWorkspace.Session session) {
        this(session, null);
    }

    public Gui2WorkspaceController(InteractiveWorkspace.Session session, Gui2WorkspaceLayout layout) {
        this.session = session;
        this.layout = layout == null ? new Gui2WorkspaceLayout(messages()) : layout;
    }

    public Gui2WorkspaceLayout.WorkspaceComponents build() {
        layout.setMessages(messages());
        components = layout.build(session.dashboardState(), this);
        editorCleanSnapshot = components.sqlEditor().getText();
        focus(FocusTarget.EXPLORER);
        return components;
    }

    public Gui2WorkspaceLayout.WorkspaceComponents build(TerminalSize terminalSize) {
        layout.setMessages(messages());
        components = layout.build(session.dashboardState(), this, terminalSize);
        editorCleanSnapshot = components.sqlEditor().getText();
        focus(FocusTarget.EXPLORER);
        return components;
    }

    public Gui2WorkspaceLayout.WorkspaceComponents components() {
        return components;
    }

    public boolean handleKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return false;
        }
        if (keyStroke.getKeyType() == KeyType.Tab) {
            moveFocus(FocusDirection.NEXT);
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.ReverseTab || (keyStroke.isShiftDown() && keyStroke.getKeyType() == KeyType.Tab)) {
            moveFocus(FocusDirection.PREVIOUS);
            return true;
        }
        if (shouldHandleHelpShortcut(keyStroke)) {
            showHelp();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F2 || (keyStroke.isCtrlDown() && Character.valueOf('b').equals(lowerCharacter(keyStroke)))) {
            focus(FocusTarget.EXPLORER);
            return true;
        }
        if (
            keyStroke.getKeyType() == KeyType.F3 ||
            (keyStroke.isCtrlDown() &&
                (Character.valueOf('e').equals(lowerCharacter(keyStroke)) || Character.valueOf('l').equals(lowerCharacter(keyStroke))))
        ) {
            focus(FocusTarget.EDITOR);
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F5 || (keyStroke.isCtrlDown() && Character.valueOf('r').equals(lowerCharacter(keyStroke)))) {
            runCurrentSql();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F6) {
            openSqlFileDialog();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F7) {
            openExportDialog(ExportScope.CURRENT_PAGE);
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F8) {
            openExportDialog(ExportScope.ALL_PAGES);
            return true;
        }
        if (focusTarget == FocusTarget.EDITOR && isEditorTextInput(keyStroke)) {
            return handleEditorTextInput(keyStroke);
        }
        if (focusTarget == FocusTarget.RESULTS && keyStroke.getKeyType() == KeyType.ArrowDown) {
            scrollResults(1);
            return true;
        }
        if (focusTarget == FocusTarget.RESULTS && keyStroke.getKeyType() == KeyType.ArrowUp) {
            scrollResults(-1);
            return true;
        }
        if (focusTarget == FocusTarget.RESULTS && keyStroke.getKeyType() == KeyType.ArrowRight) {
            scrollResultsHorizontally(resultsViewportColumns(ensureBuilt()));
            return true;
        }
        if (focusTarget == FocusTarget.RESULTS && keyStroke.getKeyType() == KeyType.ArrowLeft) {
            scrollResultsHorizontally(-resultsViewportColumns(ensureBuilt()));
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.PageDown) {
            nextResultPage();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.PageUp) {
            previousResultPage();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.Escape) {
            requestClose();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.Enter) {
            handleEnter();
            return true;
        }
        return false;
    }

    public void handleEnter() {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        int selectedIndex = current.explorer().getSelectedIndex();
        ensureBuilt().explorer().runSelectedItem();
        if (selectedIndex >= 0 && selectedIndex < session.dashboardState().connections().size()) {
            focus(FocusTarget.EDITOR);
        }
    }

    public void selectConnection(int index) {
        session.selectConnectionAt(index);
        ensureBuilt().rebuildExplorer(session.dashboardState(), this);
        refresh();
    }

    public void openConnectionDialog(DatabaseType databaseType) {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        activeConnectionDialog = new Gui2ConnectionDialog(session, messages()).open(databaseType);
        activeConnectionDialogWindow = connectionDialogWindow(activeConnectionDialog);
        WindowBasedTextGUI textGUI = current.window().getTextGUI();
        if (textGUI != null) {
            textGUI.addWindow(activeConnectionDialogWindow);
        }
        current.resultsText().setText(activeConnectionDialog.feedback());
    }

    public void switchLanguage() {
        language = language.toggle();
        layout.setMessages(messages());
        ensureBuilt().rebuildExplorer(session.dashboardState(), this);
        refresh();
    }

    public void openSqlFileDialog() {
        final TextBox path = new TextBox(new TerminalSize(60, 1));
        BasicWindow window = pathDialog(
            messages().openSqlFileTitle(),
            path,
            new DialogAction() {
                public boolean run(BasicWindow dialog) {
                    return openSqlFile(path.getText(), false);
                }
            }
        );
        addOrRenderWindow(window, messages().openSqlFileTitle());
    }

    public void openExportDialog(final ExportScope scope) {
        final TextBox path = new TextBox(new TerminalSize(60, 1));
        BasicWindow window = pathDialog(
            messages().exportTitle(scope),
            path,
            new DialogAction() {
                public boolean run(BasicWindow dialog) {
                    return beginExportFromDialog(path.getText(), scope, dialog);
                }
            }
        );
        addOrRenderWindow(window, messages().exportTitle(scope));
    }

    private boolean beginExportFromDialog(String typedPath, ExportScope scope, BasicWindow pathDialog) {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        File target;
        try {
            target = fileService.validateExportTarget(typedPath, true);
        } catch (Exception ex) {
            current.resultsText().setText(ex.getMessage());
            return false;
        }
        if (scope == ExportScope.ALL_PAGES) {
            current.resultsText().setText(messages().exportAllPagesConfirmation());
            showAllPagesConfirmation(pathDialog, target);
            return false;
        }
        if (target.exists()) {
            current.resultsText().setText(messages().overwriteConfirmation(target.getPath()));
            showOverwriteConfirmation(pathDialog, target, scope, true);
            return false;
        }
        return exportResults(target.getPath(), scope, false, true);
    }

    public boolean openSqlFile(String typedPath, boolean replaceDirty) {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        if (isDirty(current) && !replaceDirty) {
            current.resultsText().setText(messages().replaceDirtyEditorConfirmation());
            return false;
        }
        try {
            SupportWorkflowFileService.LoadedSqlFile loaded = fileService.readSqlFile(typedPath);
            current.sqlEditor().setText(loaded.content());
            current.sqlEditor().setCaretPosition(0, 0);
            editorCleanSnapshot = loaded.content();
            session.replaceBuffer(loaded.content());
            current.resultsText().setText(messages().sqlFileLoaded(loaded.file().getCanonicalPath()));
            refreshEditorDiagnostics(current);
            return true;
        } catch (Exception ex) {
            current.resultsText().setText(ex.getMessage());
            return false;
        }
    }

    public boolean exportResults(String typedPath, ExportScope scope, boolean overwrite, boolean confirmAllPages) {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        if (scope == ExportScope.ALL_PAGES && !confirmAllPages) {
            current.resultsText().setText(messages().exportAllPagesCancelled());
            return false;
        }
        try {
            File target = fileService.validateExportTarget(typedPath, overwrite);
            csvExporter.export(session.lastExecutionResult(), target, scope, overwrite);
            current.resultsText().setText(messages().csvExported(target.getCanonicalPath()));
            return true;
        } catch (Exception ex) {
            current.resultsText().setText(ex.getMessage());
            return false;
        }
    }

    public boolean handleWorkspaceKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return false;
        }
        KeyType keyType = keyStroke.getKeyType();
        if (keyType == KeyType.Enter) {
            Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
            if (focusTarget == FocusTarget.EXPLORER || current.explorer().isFocused()) {
                return handleKeyStroke(keyStroke);
            }
            return false;
        }
        if (shouldHandleHelpShortcut(keyStroke)) {
            return handleKeyStroke(keyStroke);
        }
        if (isWorkspaceShortcut(keyStroke)) {
            return handleKeyStroke(keyStroke);
        }
        return false;
    }

    private boolean handleEditorTextInput(KeyStroke keyStroke) {
        Interactable.Result result = ensureBuilt().sqlEditor().handleInput(keyStroke);
        return result != Interactable.Result.UNHANDLED;
    }

    public Gui2ConnectionDialog.Form activeConnectionDialog() {
        return activeConnectionDialog;
    }

    public Gui2ConnectionDialog.Result cancelActiveConnectionDialog() {
        Gui2ConnectionDialog.Form dialog = activeConnectionDialog;
        if (dialog == null) {
            dialog = new Gui2ConnectionDialog(session, messages()).open(DatabaseType.ORACLE);
        }
        Gui2ConnectionDialog.Result result = dialog.cancel();
        activeConnectionDialog = null;
        closeActiveConnectionDialogWindow();
        ensureBuilt().resultsText().setText(result.message());
        return result;
    }

    public Gui2ConnectionDialog.Result submitConnection(Gui2ConnectionDialog.Request request) {
        Gui2ConnectionDialog.Result result = new Gui2ConnectionDialog(session, messages()).submit(request);
        if (result.created()) {
            Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
            WorkspaceDashboardRenderer.DashboardState state = session.dashboardState();
            current.rebuildExplorer(state, this);
            current.refresh(state);
        }
        return result;
    }

    private BasicWindow connectionDialogWindow(final Gui2ConnectionDialog.Form form) {
        final TextBox name = new TextBox(new TerminalSize(42, 1));
        final ComboBox<ConnectionEnvironment> environment = new ComboBox<ConnectionEnvironment>(
            Arrays.asList(ConnectionEnvironment.values())
        );
        environment.setReadOnly(true);
        environment.setSelectedItem(ConnectionEnvironment.DEV);
        final TextBox jdbcUrl = new TextBox(new TerminalSize(42, 1), defaultJdbcUrl(form.databaseType()));
        final TextBox username = new TextBox(new TerminalSize(42, 1));
        final TextBox password = new TextBox(new TerminalSize(42, 1)).setMask('*');
        final TextBox schemas = new TextBox(new TerminalSize(42, 1));

        Panel fields = new Panel(new GridLayout(2));
        final TuiMessages messages = messages();
        fields.addComponent(new Label(messages.name()));
        fields.addComponent(name);
        fields.addComponent(new Label(messages.environment()));
        fields.addComponent(environment);
        fields.addComponent(new Label(messages.jdbcUrl()));
        fields.addComponent(jdbcUrl);
        fields.addComponent(new Label(messages.username()));
        fields.addComponent(username);
        fields.addComponent(new Label(messages.password()));
        fields.addComponent(password);
        fields.addComponent(new Label(messages.schemas()));
        fields.addComponent(schemas);

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(
            new Button(
                messages.save(),
                new Runnable() {
                    public void run() {
                        Gui2ConnectionDialog.Result result = submitConnection(
                            new Gui2ConnectionDialog.Request(
                                form.databaseType(),
                                environment.getSelectedItem(),
                                name.getText(),
                                jdbcUrl.getText(),
                                username.getText(),
                                password.getText(),
                                splitSchemas(schemas.getText()),
                                splitSchemas(schemas.getText())
                            )
                        );
                        ensureBuilt().resultsText().setText(result.message());
                        if (result.created()) {
                            activeConnectionDialog = null;
                            closeActiveConnectionDialogWindow();
                        }
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages.cancel(),
                new Runnable() {
                    public void run() {
                        cancelActiveConnectionDialog();
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages.back(),
                new Runnable() {
                    public void run() {
                        cancelActiveConnectionDialog();
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages.defaults(),
                new Runnable() {
                    public void run() {
                        jdbcUrl.setText(defaultJdbcUrl(form.databaseType()));
                        environment.setSelectedItem(ConnectionEnvironment.DEV);
                        schemas.setText(defaultSchemas(form.databaseType()));
                    }
                }
            )
        );

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(form.feedback()));
        root.addComponent(fields);
        root.addComponent(buttons);

        BasicWindow window = new BasicWindow(messages.connectionWindowTitle(form.databaseType()));
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.MODAL));
        window.setComponent(root);
        return window;
    }

    private BasicWindow pathDialog(String title, final TextBox path, final DialogAction action) {
        final BasicWindow[] dialog = new BasicWindow[1];
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(messages().pathLabel()));
        root.addComponent(path);
        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(
            new Button(
                messages().save(),
                new Runnable() {
                    public void run() {
                        if (action.run(dialog[0])) {
                            closeDialog(dialog[0]);
                        }
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages().cancel(),
                new Runnable() {
                    public void run() {
                        closeDialog(dialog[0]);
                    }
                }
            )
        );
        root.addComponent(buttons);
        BasicWindow window = new BasicWindow(title);
        dialog[0] = window;
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.MODAL));
        window.setComponent(root);
        return window;
    }

    private interface DialogAction {
        boolean run(BasicWindow dialog);
    }

    private void showAllPagesConfirmation(final BasicWindow pathDialog, final File target) {
        final BasicWindow[] confirmDialog = new BasicWindow[1];
        Panel root = confirmationRoot(messages().exportAllPagesConfirmation());
        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(
            new Button(
                messages().continueAction(),
                new Runnable() {
                    public void run() {
                        if (target.exists()) {
                            ensureBuilt().resultsText().setText(messages().overwriteConfirmation(target.getPath()));
                            showOverwriteConfirmation(pathDialog, target, ExportScope.ALL_PAGES, true);
                            closeDialog(confirmDialog[0]);
                            return;
                        }
                        if (exportResults(target.getPath(), ExportScope.ALL_PAGES, false, true)) {
                            closeDialog(confirmDialog[0]);
                            closeDialog(pathDialog);
                        }
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages().cancel(),
                new Runnable() {
                    public void run() {
                        ensureBuilt().resultsText().setText(messages().exportAllPagesCancelled());
                        closeDialog(confirmDialog[0]);
                        closeDialog(pathDialog);
                    }
                }
            )
        );
        root.addComponent(buttons);
        confirmDialog[0] = confirmationWindow(messages().exportAllPagesConfirmationTitle(), root);
        addOrRenderWindow(confirmDialog[0], messages().exportAllPagesConfirmation());
    }

    private void showOverwriteConfirmation(
        final BasicWindow pathDialog,
        final File target,
        final ExportScope scope,
        final boolean allPagesConfirmed
    ) {
        final BasicWindow[] confirmDialog = new BasicWindow[1];
        Panel root = confirmationRoot(messages().overwriteConfirmation(target.getPath()));
        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(
            new Button(
                messages().overwriteAction(),
                new Runnable() {
                    public void run() {
                        if (exportResults(target.getPath(), scope, true, allPagesConfirmed)) {
                            closeDialog(confirmDialog[0]);
                            closeDialog(pathDialog);
                        }
                    }
                }
            )
        );
        buttons.addComponent(
            new Button(
                messages().cancel(),
                new Runnable() {
                    public void run() {
                        ensureBuilt().resultsText().setText(messages().exportOverwriteCancelled());
                        closeDialog(confirmDialog[0]);
                        closeDialog(pathDialog);
                    }
                }
            )
        );
        root.addComponent(buttons);
        confirmDialog[0] = confirmationWindow(messages().overwriteConfirmationTitle(), root);
        addOrRenderWindow(confirmDialog[0], messages().overwriteConfirmation(target.getPath()));
    }

    private Panel confirmationRoot(String message) {
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(message));
        return root;
    }

    private BasicWindow confirmationWindow(String title, Panel root) {
        BasicWindow window = new BasicWindow(title);
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.MODAL));
        window.setComponent(root);
        return window;
    }

    private static void closeDialog(BasicWindow window) {
        if (window == null) {
            return;
        }
        TextGUI textGUI = window.getTextGUI();
        if (textGUI instanceof WindowBasedTextGUI) {
            ((WindowBasedTextGUI) textGUI).removeWindow(window);
        }
        window.close();
    }

    private void addOrRenderWindow(BasicWindow window, String fallback) {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        WindowBasedTextGUI textGUI = current.window().getTextGUI();
        if (textGUI != null) {
            textGUI.addWindow(window);
        } else {
            current.resultsText().setText(fallback);
        }
    }

    private void closeActiveConnectionDialogWindow() {
        if (activeConnectionDialogWindow != null) {
            activeConnectionDialogWindow.close();
            activeConnectionDialogWindow = null;
        }
    }

    private static List<String> splitSchemas(String value) {
        List<String> schemas = new ArrayList<String>();
        if (value == null || value.trim().isEmpty()) {
            return schemas;
        }
        for (String token : value.split(",")) {
            String schema = token.trim();
            if (!schema.isEmpty()) {
                schemas.add(schema);
            }
        }
        return schemas;
    }

    private static String defaultJdbcUrl(DatabaseType databaseType) {
        if (databaseType == DatabaseType.POSTGRESQL) {
            return "jdbc:postgresql://localhost:5432/postgres";
        }
        return "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    }

    private static String defaultSchemas(DatabaseType databaseType) {
        if (databaseType == DatabaseType.POSTGRESQL) {
            return "public";
        }
        return "";
    }

    public void runCurrentSql() {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        session.runCurrentBuffer(current.sqlEditor().getText(), caretOffset(current));
        resultScrollOffset = 0;
        resultHorizontalOffset = 0;
        refresh();
    }

    public void showHelp() {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        activeHelpWindow = helpWindow();
        WindowBasedTextGUI textGUI = current.window().getTextGUI();
        if (textGUI != null) {
            textGUI.addWindow(activeHelpWindow);
        } else {
            current.resultsText().setText(messages().helpBody());
        }
    }

    public BasicWindow activeHelpWindow() {
        return activeHelpWindow;
    }

    public String languageName() {
        return language.name();
    }

    public boolean closeRequested() {
        return closeRequested;
    }

    public void resize(TerminalSize terminalSize) {
        layout.resize(ensureBuilt(), terminalSize);
    }

    public String focusName() {
        return focusTarget.name;
    }

    private void requestClose() {
        closeRequested = true;
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        current.window().setVisible(false);
        current.window().close();
    }

    private void refresh() {
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        current.refresh(session.dashboardState());
        refreshEditorDiagnostics(current);
        renderResultsViewport(current);
    }

    private void scrollResults(int delta) {
        resultScrollOffset = Math.max(0, resultScrollOffset + delta);
        renderResultsViewport(ensureBuilt());
    }

    private void scrollResultsHorizontally(int delta) {
        resultHorizontalOffset = Math.max(0, resultHorizontalOffset + delta);
        renderResultsViewport(ensureBuilt());
    }

    private void nextResultPage() {
        if (session.nextResultPage()) {
            resultScrollOffset = 0;
            resultHorizontalOffset = 0;
            refresh();
        }
    }

    private void previousResultPage() {
        if (session.previousResultPage()) {
            resultScrollOffset = 0;
            resultHorizontalOffset = 0;
            refresh();
        }
    }

    private void renderResultsViewport(Gui2WorkspaceLayout.WorkspaceComponents current) {
        WorkspaceDashboardRenderer.DashboardState state = session.dashboardState();
        TuiMessages messages = messages();
        String content = Gui2WorkspaceLayout.resultText(state, messages);
        SqlExecutionResult result = session.lastExecutionResult() == null ? new SqlExecutionResult(content) : session.lastExecutionResult();
        ResultsPanelComponent.RenderedPanel rendered = ResultsPanelComponent.success(result).render(
            resultsViewportRows(current),
            WorkspaceFocus.RESULTS,
            resultScrollOffset,
            resultHorizontalOffset,
            resultsViewportColumns(current),
            messages
        );
        List<String> lines = new ArrayList<String>(rendered.lines());
        if (!result.hasTabularRows() && !lines.isEmpty() && (lines.get(0).startsWith("Results") || lines.get(0).startsWith("Resultados"))) {
            lines.remove(0);
        }
        current.resultsText().setText(joinLines(lines));
    }

    private void refreshEditorDiagnostics(Gui2WorkspaceLayout.WorkspaceComponents current) {
        WorkspaceDashboardRenderer.DashboardState state = session.dashboardState();
        TuiMessages messages = messages();
        String baseStatus = messages.statusText(state);
        List<SqlDiagnostic> diagnostics = SqlDiagnostics.analyze(current.sqlEditor().getText(), activeDialect(state));
        if (!diagnostics.isEmpty()) {
            current.statusText().setText(baseStatus + " | " + messages.diagnosticStatus(diagnostics.get(0)));
        } else {
            current.statusText().setText(baseStatus);
        }
    }

    private static SqlDialect activeDialect(WorkspaceDashboardRenderer.DashboardState state) {
        for (WorkspaceDashboardRenderer.ConnectionSummary connection : state.connections()) {
            if (connection.active()) {
                return SqlDialect.fromDatabaseTypeName(connection.databaseType());
            }
        }
        return SqlDialect.GENERIC;
    }

    private static int resultsViewportRows(Gui2WorkspaceLayout.WorkspaceComponents current) {
        TerminalSize size = current.resultsPanel().getPreferredSize();
        return Math.max(1, (size == null ? 8 : size.getRows()) - 1);
    }

    private static int resultsViewportColumns(Gui2WorkspaceLayout.WorkspaceComponents current) {
        TerminalSize size = current.resultsPanel().getPreferredSize();
        return Math.max(20, (size == null ? 80 : size.getColumns()) - 2);
    }

    private static String joinLines(List<String> lines) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                joined.append(System.lineSeparator());
            }
            joined.append(lines.get(i));
        }
        return joined.toString();
    }

    private BasicWindow helpWindow() {
        final BasicWindow window = new BasicWindow(messages().helpTitle());
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label(messages().helpBody()));
        root.addComponent(
            new Button(
                messages().close(),
                new Runnable() {
                    public void run() {
                        window.close();
                        activeHelpWindow = null;
                    }
                }
            )
        );
        window.setHints(Arrays.asList(Window.Hint.CENTERED, Window.Hint.MODAL));
        window.setCloseWindowWithEscape(true);
        window.setComponent(root);
        return window;
    }

    private TuiMessages messages() {
        return new TuiMessages(language);
    }

    private void moveFocus(FocusDirection direction) {
        if (direction == FocusDirection.NEXT) {
            focus(focusTarget.next());
        } else {
            focus(focusTarget.previous());
        }
    }

    private void focus(FocusTarget target) {
        focusTarget = target;
        Gui2WorkspaceLayout.WorkspaceComponents current = ensureBuilt();
        if (target == FocusTarget.EXPLORER) {
            current.window().setFocusedInteractable(current.explorer());
            current.explorer().takeFocus();
        } else if (target == FocusTarget.EDITOR) {
            current.window().setFocusedInteractable(current.sqlEditor());
            current.sqlEditor().takeFocus();
        }
    }

    private Gui2WorkspaceLayout.WorkspaceComponents ensureBuilt() {
        if (components == null) {
            return build();
        }
        return components;
    }

    private static int caretOffset(Gui2WorkspaceLayout.WorkspaceComponents components) {
        String text = components.sqlEditor().getText();
        com.googlecode.lanterna.TerminalPosition caretPosition = components.sqlEditor().getCaretPosition();
        int targetRow = Math.max(0, caretPosition.getRow());
        int targetColumn = Math.max(0, caretPosition.getColumn());
        int offset = 0;
        int row = 0;
        while (row < targetRow && offset < text.length()) {
            int lineBreak = text.indexOf('\n', offset);
            if (lineBreak < 0) {
                return text.length();
            }
            offset = lineBreak + 1;
            row++;
        }
        int lineEnd = text.indexOf('\n', offset);
        if (lineEnd < 0) {
            lineEnd = text.length();
        }
        return Math.min(lineEnd, offset + targetColumn);
    }

    private static Character lowerCharacter(KeyStroke keyStroke) {
        Character character = keyStroke.getCharacter();
        return character == null ? null : Character.toLowerCase(character.charValue());
    }

    private static boolean isWorkspaceShortcut(KeyStroke keyStroke) {
        KeyType keyType = keyStroke.getKeyType();
        Character character = lowerCharacter(keyStroke);
        return (
            keyType == KeyType.Tab ||
            keyType == KeyType.ReverseTab ||
            keyType == KeyType.PageDown ||
            keyType == KeyType.PageUp ||
            keyType == KeyType.ArrowDown ||
            keyType == KeyType.ArrowUp ||
            keyType == KeyType.ArrowLeft ||
            keyType == KeyType.ArrowRight ||
            isNonTypingHelpShortcut(keyStroke) ||
            keyType == KeyType.F2 ||
            keyType == KeyType.F3 ||
            keyType == KeyType.F5 ||
            keyType == KeyType.F6 ||
            keyType == KeyType.F7 ||
            keyType == KeyType.F8 ||
            keyType == KeyType.Escape ||
            (keyStroke.isShiftDown() && keyType == KeyType.Tab) ||
            (keyStroke.isCtrlDown() &&
                (Character.valueOf('b').equals(character) ||
                    Character.valueOf('e').equals(character) ||
                    Character.valueOf('h').equals(character) ||
                    Character.valueOf('l').equals(character) ||
                    Character.valueOf('r').equals(character)))
        );
    }

    private static boolean isEditorTextInput(KeyStroke keyStroke) {
        KeyType keyType = keyStroke.getKeyType();
        return (
            keyType == KeyType.Character ||
            keyType == KeyType.Enter ||
            keyType == KeyType.Backspace ||
            keyType == KeyType.Delete ||
            keyType == KeyType.ArrowDown ||
            keyType == KeyType.ArrowUp ||
            keyType == KeyType.ArrowLeft ||
            keyType == KeyType.ArrowRight ||
            keyType == KeyType.Home ||
            keyType == KeyType.End
        );
    }

    private boolean shouldHandleHelpShortcut(KeyStroke keyStroke) {
        return isNonTypingHelpShortcut(keyStroke) || (isQuestionHelpShortcut(keyStroke) && focusTarget != FocusTarget.EDITOR);
    }

    private boolean isDirty(Gui2WorkspaceLayout.WorkspaceComponents current) {
        String clean = editorCleanSnapshot == null ? "" : editorCleanSnapshot;
        String currentText = current.sqlEditor().getText() == null ? "" : current.sqlEditor().getText();
        return !clean.equals(currentText);
    }

    private static boolean isHelpShortcut(KeyStroke keyStroke) {
        return isNonTypingHelpShortcut(keyStroke) || isQuestionHelpShortcut(keyStroke);
    }

    private static boolean isNonTypingHelpShortcut(KeyStroke keyStroke) {
        Character character = lowerCharacter(keyStroke);
        return (
            keyStroke.getKeyType() == KeyType.F1 ||
            (keyStroke.isCtrlDown() && Character.valueOf('h').equals(character)) ||
            (keyStroke.getKeyType() == KeyType.Character && Character.valueOf('\b').equals(character))
        );
    }

    private static boolean isQuestionHelpShortcut(KeyStroke keyStroke) {
        return keyStroke.getKeyType() == KeyType.Character && Character.valueOf('?').equals(keyStroke.getCharacter());
    }

    private enum FocusDirection {
        NEXT,
        PREVIOUS,
    }

    private enum FocusTarget {
        EXPLORER("explorer"),
        EDITOR("editor"),
        RESULTS("results"),
        FOOTER("footer");

        private final String name;

        FocusTarget(String name) {
            this.name = name;
        }

        public FocusTarget next() {
            FocusTarget[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public FocusTarget previous() {
            FocusTarget[] values = values();
            return values[(ordinal() + values.length - 1) % values.length];
        }
    }
}
