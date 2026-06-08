package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class WorkspaceViewModelTest {

    @Test
    public void composesFullWorkspaceSectionsFromPanelComponents() {
        ConnectionListComponent connections = new ConnectionListComponent(
            Arrays.asList(
                new ConnectionListComponent.ConnectionItem(
                    "qa-oracle",
                    new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/XEPDB1", "qa", "secret")
                )
            )
        );
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from users", 6);
        ResultsPanelComponent results = ResultsPanelComponent.success(new SqlExecutionResult("OK\nTotal rows: 1\n"));
        HelpStatusComponent help = new HelpStatusComponent();

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR),
            connections,
            editor,
            results,
            help,
            100,
            30,
            "qa-oracle"
        );

        assertEquals("Workspace | Active: qa-oracle [DEV] [ORACLE]", rendered.headerLine());
        assertTrue(rendered.leftPaneLines().contains("* [DEV] qa-oracle [ORACLE]"));
        assertTrue(rendered.leftPaneLines().contains("  New PostgreSQL connection"));
        assertEquals(rendered.leftPaneLines(), rendered.connectionLines());
        assertEquals(Arrays.asList("SQL Editor *", "select * from users"), rendered.editorLines());
        assertEquals(rendered.editorLines(), rendered.rightPaneLines());
        assertTrue(rendered.bottomPaneLines().contains("OK"));
        assertEquals(rendered.bottomPaneLines(), rendered.resultLines());
        assertEquals("Focus: EDITOR | Active: qa-oracle [DEV] [ORACLE] | Ready | Ctrl+R Run | F1/? Help | Esc Exit", rendered.footerLine());
        assertEquals(rendered.footerLine(), rendered.statusLine());
        assertTrue(rendered.helpLines().isEmpty());
    }

    @Test
    public void rendersFormStateAsFocusedHelpOverlayGuidanceWithoutRunningFormPersistence() {
        WorkspaceScreenState state = WorkspaceScreenState.initial()
            .withFocus(WorkspaceFocus.CONNECTIONS)
            .withCreationForm(DatabaseType.POSTGRESQL)
            .withHelpVisible(true);

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            state,
            new ConnectionListComponent(Arrays.<ConnectionListComponent.ConnectionItem>asList()),
            new BasicSqlEditorComponent("", 0),
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            100,
            30,
            "none"
        );

        assertTrue(rendered.helpLines().contains("Form: New PostgreSQL connection"));
        assertTrue(rendered.helpLines().contains("Enter: submit form | Esc: cancel form"));
        assertTrue(rendered.leftPaneLines().contains("> New Oracle connection"));
        assertTrue(rendered.leftPaneLines().contains("  New PostgreSQL connection"));
    }

    @Test
    public void carriesStyledEditorRowsAndPreservesPlainEditorLinesFallback() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select * from users", 6);

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR),
            new ConnectionListComponent(Arrays.<ConnectionListComponent.ConnectionItem>asList()),
            editor,
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            100,
            30,
            "none"
        );

        assertEquals(Arrays.asList("SQL Editor *", "select * from users"), rendered.editorLines());
        assertEquals("select * from users", rendered.editorRenderModel().lines().get(0).text());
        List<SqlRenderSpan> spans = rendered.editorRenderModel().lines().get(0).spans();
        assertEquals(4, spans.size());
        assertEquals(SqlStyle.KEYWORD, spans.get(0).style());
        assertEquals(SqlStyle.OPERATOR, spans.get(1).style());
        assertEquals(SqlStyle.KEYWORD, spans.get(2).style());
        assertEquals(SqlStyle.IDENTIFIER, spans.get(3).style());
    }

    @Test
    public void carriesCompletionPopupCandidatesFromEditorModel() {
        CompletionProvider provider = new CompositeCompletionProvider(
            Arrays.<CompletionProvider>asList(new SqlKeywordCompletionProvider(), new CatalogCompletionProvider())
        );
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent(
            "select * from us",
            16,
            provider,
            MetadataCatalogSnapshot.ofTables(Arrays.asList("users", "orders"))
        );

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR),
            new ConnectionListComponent(Arrays.<ConnectionListComponent.ConnectionItem>asList()),
            editor,
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            100,
            30,
            "none"
        );

        assertEquals(1, rendered.completionCandidates().size());
        assertEquals("users", rendered.completionCandidates().get(0).value());
        assertEquals("table", rendered.completionCandidates().get(0).kind());
        assertEquals(Arrays.asList("Autocomplete", "  users [table]"), rendered.completionPopupLines());
    }

    @Test
    public void hidesCompletionPopupWhenNoCandidatesExist() {
        BasicSqlEditorComponent editor = new BasicSqlEditorComponent("select ", 7);

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            WorkspaceScreenState.initial().withFocus(WorkspaceFocus.EDITOR),
            new ConnectionListComponent(Arrays.<ConnectionListComponent.ConnectionItem>asList()),
            editor,
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            100,
            30,
            "none"
        );

        assertTrue(rendered.completionPopupLines().isEmpty());
    }

    @Test
    public void composesCompactWorkspaceWithHelpOverlayWhenSmallTerminalRequestsHelp() {
        WorkspaceScreenState state = WorkspaceScreenState.initial()
            .withFocus(WorkspaceFocus.HELP)
            .withHelpVisible(true)
            .withCompactLayout(true);

        WorkspaceViewModel.RenderedWorkspace rendered = WorkspaceViewModel.compose(
            state,
            new ConnectionListComponent(Arrays.<ConnectionListComponent.ConnectionItem>asList()),
            new BasicSqlEditorComponent("select 1", 8),
            ResultsPanelComponent.empty(),
            new HelpStatusComponent(),
            50,
            10,
            "none"
        );

        assertTrue(rendered.compact());
        assertEquals("Compact workspace", rendered.title());
        assertEquals("Help *", rendered.helpLines().get(0));
        assertTrue(rendered.connectionLines().contains("No saved connections"));
    }
}
