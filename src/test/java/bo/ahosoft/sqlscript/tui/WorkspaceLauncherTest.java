package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class WorkspaceLauncherTest {

    @Test
    public void launchesGui2WorkspaceWhenTerminalIsSupported() throws Exception {
        RecordingWorkspaceRunner gui2 = new RecordingWorkspaceRunner(7);
        RecordingFallback fallback = new RecordingFallback(3);
        WorkspaceLauncher launcher = new WorkspaceLauncher(new FixedTerminalSupport(true), gui2, fallback);

        int exitCode = launcher.run();

        assertEquals(7, exitCode);
        assertTrue(gui2.started);
        assertEquals(0, fallback.calls);
    }

    @Test
    public void usesFallbackWorkspaceWhenTerminalIsUnsupported() throws Exception {
        RecordingWorkspaceRunner gui2 = new RecordingWorkspaceRunner(7);
        RecordingFallback fallback = new RecordingFallback(3);
        WorkspaceLauncher launcher = new WorkspaceLauncher(new FixedTerminalSupport(false), gui2, fallback);

        int exitCode = launcher.run();

        assertEquals(3, exitCode);
        assertEquals(0, gui2.calls);
        assertEquals(1, fallback.calls);
    }

    @Test
    public void supportedLauncherDoesNotUseRawStackedWorkspace() throws Exception {
        RecordingWorkspaceRunner gui2 = new RecordingWorkspaceRunner(0);
        FailingRawWorkspace raw = new FailingRawWorkspace();
        WorkspaceLauncher launcher = WorkspaceLauncher.forGui2(new FixedTerminalSupport(true), gui2, raw);

        int exitCode = launcher.run();

        assertEquals(0, exitCode);
        assertEquals(1, gui2.calls);
        assertEquals(0, raw.calls);
    }

    @Test
    public void rawTerminalWorkspaceSourceIsRemovedFromActiveCodebase() {
        assertFalse(new File("src/main/java/bo/ahosoft/sqlscript/tui/RawTerminalWorkspace.java").exists());
        assertTrue(new File("src/main/java/bo/ahosoft/sqlscript/tui/Gui2TerminalWorkspace.java").exists());
    }

    private static final class FixedTerminalSupport implements WorkspaceLauncher.TerminalSupport {

        private final boolean supported;

        FixedTerminalSupport(boolean supported) {
            this.supported = supported;
        }

        public boolean isSupported() {
            return supported;
        }
    }

    private static final class RecordingWorkspaceRunner implements WorkspaceLauncher.WorkspaceRunner {

        private final int exitCode;
        private int calls;
        private boolean started;

        RecordingWorkspaceRunner(int exitCode) {
            this.exitCode = exitCode;
        }

        public int run() {
            calls++;
            started = true;
            return exitCode;
        }
    }

    private static final class FailingRawWorkspace implements WorkspaceLauncher.WorkspaceRunner {

        private int calls;

        public int run() throws IOException {
            calls++;
            throw new IOException("raw workspace must not be used by the GUI2 launcher");
        }
    }

    private static final class RecordingFallback implements WorkspaceLauncher.WorkspaceRunner {

        private final int exitCode;
        private int calls;

        RecordingFallback(int exitCode) {
            this.exitCode = exitCode;
        }

        public int run() {
            calls++;
            return exitCode;
        }
    }
}
