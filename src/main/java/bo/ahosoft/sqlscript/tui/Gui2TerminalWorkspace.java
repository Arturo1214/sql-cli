package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;

public final class Gui2TerminalWorkspace implements WorkspaceLauncher.WorkspaceRunner {

    private final ScreenFactory screenFactory;
    private final InteractiveWorkspace.Session session;
    private final Gui2WorkspaceLayout layout;
    private final GuiRunner guiRunner;

    public Gui2TerminalWorkspace(InteractiveWorkspace.Session session) {
        this(new DefaultScreenFactory(), session, new Gui2WorkspaceLayout(), new LanternaGuiRunner());
    }

    public Gui2TerminalWorkspace(ScreenFactory screenFactory, InteractiveWorkspace.Session session, Gui2WorkspaceLayout layout) {
        this(screenFactory, session, layout, new LanternaGuiRunner());
    }

    public Gui2TerminalWorkspace(
        ScreenFactory screenFactory,
        InteractiveWorkspace.Session session,
        Gui2WorkspaceLayout layout,
        GuiRunner guiRunner
    ) {
        this.screenFactory = screenFactory;
        this.session = session;
        this.layout = layout;
        this.guiRunner = guiRunner;
    }

    public int run() throws IOException {
        Screen screen = screenFactory.createScreen();
        screen.startScreen();
        try {
            final Gui2WorkspaceController controller = new Gui2WorkspaceController(session, layout);
            Gui2WorkspaceLayout.WorkspaceComponents components = controller.build(screen.getTerminalSize());
            guiRunner.run(screen, components.window(), resizeHook(screen, controller));
            return 0;
        } catch (IOException ex) {
            return 1;
        } catch (RuntimeException ex) {
            return 1;
        } finally {
            screen.stopScreen();
        }
    }

    public interface ScreenFactory {
        Screen createScreen() throws IOException;
    }

    public interface GuiRunner {
        void run(Screen screen, BasicWindow window, Runnable resizeHook) throws IOException;
    }

    private static Runnable resizeHook(final Screen screen, final Gui2WorkspaceController controller) {
        return new Runnable() {
            public void run() {
                TerminalSize resized = screen.doResizeIfNecessary();
                if (resized != null) {
                    controller.resize(resized);
                }
            }
        };
    }

    private static final class LanternaGuiRunner implements GuiRunner {

        public void run(Screen screen, BasicWindow window, Runnable resizeHook) {
            resizeHook.run();
            new MultiWindowTextGUI(screen).addWindowAndWait(window);
        }
    }

    private static final class DefaultScreenFactory implements ScreenFactory {

        public Screen createScreen() throws IOException {
            return new DefaultTerminalFactory().createScreen();
        }
    }
}
