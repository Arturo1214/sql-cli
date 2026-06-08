package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TabBehaviour;
import java.io.IOException;
import org.junit.Test;

public class Gui2TerminalWorkspaceTest {

    @Test
    public void startsGui2WindowAndStopsScreenCleanly() throws Exception {
        RecordingScreen screen = new RecordingScreen();
        RecordingGuiRunner runner = new RecordingGuiRunner();
        Gui2TerminalWorkspace workspace = new Gui2TerminalWorkspace(
            new FixedScreenFactory(screen),
            new InteractiveWorkspace.Session(),
            new Gui2WorkspaceLayout(),
            runner
        );

        int exitCode = workspace.run();

        assertEquals(0, exitCode);
        assertTrue(screen.started);
        assertTrue(screen.stopped);
        assertEquals("Database Script Workspace", runner.window.getTitle());
    }

    @Test
    public void startsGui2WindowAsFullscreenWindow() throws Exception {
        RecordingScreen screen = new RecordingScreen();
        RecordingGuiRunner runner = new RecordingGuiRunner();
        Gui2TerminalWorkspace workspace = new Gui2TerminalWorkspace(
            new FixedScreenFactory(screen),
            new InteractiveWorkspace.Session(),
            new Gui2WorkspaceLayout(),
            runner
        );

        int exitCode = workspace.run();

        assertEquals(0, exitCode);
        assertTrue(runner.window.getHints().contains(Window.Hint.FULL_SCREEN));
    }

    @Test
    public void resizeHookPollsScreenAndResizesLayout() throws Exception {
        RecordingScreen screen = new RecordingScreen();
        screen.nextResize = new TerminalSize(144, 42);
        RecordingLayout layout = new RecordingLayout();
        RecordingGuiRunner runner = new RecordingGuiRunner();
        Gui2TerminalWorkspace workspace = new Gui2TerminalWorkspace(
            new FixedScreenFactory(screen),
            new InteractiveWorkspace.Session(),
            layout,
            runner
        );

        int exitCode = workspace.run();
        assertNotNull(runner.resizeHook);
        runner.resizeHook.run();

        assertEquals(0, exitCode);
        assertEquals(new TerminalSize(144, 42), layout.lastResize);
    }

    @Test
    public void stopsScreenWhenGuiRunnerFails() throws Exception {
        RecordingScreen screen = new RecordingScreen();
        Gui2TerminalWorkspace workspace = new Gui2TerminalWorkspace(
            new FixedScreenFactory(screen),
            new InteractiveWorkspace.Session(),
            new Gui2WorkspaceLayout(),
            new FailingGuiRunner()
        );

        int exitCode = workspace.run();

        assertEquals(1, exitCode);
        assertTrue(screen.started);
        assertTrue(screen.stopped);
    }

    private static final class FixedScreenFactory implements Gui2TerminalWorkspace.ScreenFactory {

        private final Screen screen;

        FixedScreenFactory(Screen screen) {
            this.screen = screen;
        }

        public Screen createScreen() {
            return screen;
        }
    }

    private static final class RecordingGuiRunner implements Gui2TerminalWorkspace.GuiRunner {

        private BasicWindow window;
        private Runnable resizeHook;

        public void run(Screen screen, BasicWindow window, Runnable resizeHook) {
            this.window = window;
            this.resizeHook = resizeHook;
        }
    }

    private static final class FailingGuiRunner implements Gui2TerminalWorkspace.GuiRunner {

        public void run(Screen screen, BasicWindow window, Runnable resizeHook) throws IOException {
            throw new IOException("boom");
        }
    }

    private static final class RecordingLayout extends Gui2WorkspaceLayout {

        private TerminalSize lastResize;

        @Override
        public WorkspaceComponents build(
            WorkspaceDashboardRenderer.DashboardState state,
            WorkspaceUiActions actions,
            TerminalSize terminalSize
        ) {
            WorkspaceComponents components = super.build(state, actions, terminalSize);
            lastResize = terminalSize;
            return components;
        }

        @Override
        public void resize(WorkspaceComponents components, TerminalSize terminalSize) {
            lastResize = terminalSize;
            super.resize(components, terminalSize);
        }
    }

    private static final class RecordingScreen implements Screen {

        private boolean started;
        private boolean stopped;

        public void startScreen() {
            started = true;
        }

        public void stopScreen() {
            stopped = true;
        }

        public void close() {
            stopped = true;
        }

        public void clear() {}

        public TerminalPosition getCursorPosition() {
            return TerminalPosition.TOP_LEFT_CORNER;
        }

        public void setCursorPosition(TerminalPosition position) {}

        public TabBehaviour getTabBehaviour() {
            return TabBehaviour.ALIGN_TO_COLUMN_4;
        }

        public void setTabBehaviour(TabBehaviour tabBehaviour) {}

        public TerminalSize getTerminalSize() {
            return new TerminalSize(120, 35);
        }

        public void setCharacter(int column, int row, TextCharacter screenCharacter) {}

        public void setCharacter(TerminalPosition position, TextCharacter screenCharacter) {}

        public TextGraphics newTextGraphics() {
            return null;
        }

        public TextCharacter getFrontCharacter(int column, int row) {
            return TextCharacter.DEFAULT_CHARACTER;
        }

        public TextCharacter getFrontCharacter(TerminalPosition position) {
            return TextCharacter.DEFAULT_CHARACTER;
        }

        public TextCharacter getBackCharacter(int column, int row) {
            return TextCharacter.DEFAULT_CHARACTER;
        }

        public TextCharacter getBackCharacter(TerminalPosition position) {
            return TextCharacter.DEFAULT_CHARACTER;
        }

        public void refresh() {}

        public void refresh(RefreshType refreshType) {}

        public TerminalSize doResizeIfNecessary() {
            TerminalSize resize = nextResize;
            nextResize = null;
            return resize;
        }

        public void scrollLines(int firstLine, int lastLine, int distance) {}

        public KeyStroke pollInput() {
            return null;
        }

        public KeyStroke readInput() {
            return null;
        }

        private TerminalSize nextResize;
    }
}
