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
import org.junit.Test;

public class HelpStatusComponentTest {

    @Test
    public void rendersStatusBarWithFocusConnectionAndExecutionStatus() {
        HelpStatusComponent component = new HelpStatusComponent();

        String status = component.statusBar(WorkspaceFocus.EDITOR, "qa-oracle", DatabaseType.ORACLE, "Ready");

        assertEquals("Focus: EDITOR | Active: qa-oracle [ORACLE] | Ready | Ctrl+R Run | F1/? Help | Esc Exit", status);
    }

    @Test
    public void rendersFooterWithNoActiveConnectionWhenNameIsBlank() {
        HelpStatusComponent component = new HelpStatusComponent();

        String status = component.statusBar(WorkspaceFocus.CONNECTIONS, "", null, "Nothing to execute");

        assertEquals("Focus: CONNECTIONS | Active: none | Nothing to execute | Ctrl+R Run | F1/? Help | Esc Exit", status);
    }

    @Test
    public void rendersHelpOverlayWithShortcutGuidanceAndFocusMarker() {
        HelpStatusComponent component = new HelpStatusComponent();

        HelpStatusComponent.RenderedHelp rendered = component.renderHelpOverlay(true, WorkspaceFocus.HELP);

        assertEquals("Help *", rendered.lines().get(0));
        assertTrue(rendered.lines().contains("Arrow Up/Down or j/k: move left-panel selection"));
        assertTrue(rendered.lines().contains("Enter: select connection or submit visible form"));
        assertTrue(rendered.lines().contains("Tab: show SQL keyword/table autocomplete"));
        assertTrue(rendered.lines().contains("Ctrl+R: run current SQL buffer"));
        assertTrue(rendered.lines().contains("F1/?: toggle this help outside the SQL editor"));
        assertTrue(rendered.lines().contains("Esc/Ctrl+C: exit workspace"));
    }

    @Test
    public void hidesHelpOverlayWhenHelpIsNotVisible() {
        HelpStatusComponent component = new HelpStatusComponent();

        HelpStatusComponent.RenderedHelp rendered = component.renderHelpOverlay(false, WorkspaceFocus.RESULTS);

        assertEquals(Arrays.<String>asList(), rendered.lines());
    }

    @Test
    public void tuiMessagesKeepHelpStatusLocalizedAndDatabaseNeutral() {
        TuiMessages english = new TuiMessages(TuiLanguage.ENGLISH);
        TuiMessages spanish = new TuiMessages(TuiLanguage.SPANISH);

        assertEquals("Database Script Workspace", english.windowTitle());
        assertEquals("Espacio de Scripts DB", spanish.windowTitle());
        assertTrue(english.helpBody().contains("New Oracle connection, New PostgreSQL connection"));
        assertTrue(spanish.helpBody().contains("Nueva conexion Oracle, Nueva conexion PostgreSQL"));
        assertTrue(english.helpHint().contains("F1/? help"));
        assertTrue(spanish.helpHint().contains("F1/? ayuda"));
    }
}
