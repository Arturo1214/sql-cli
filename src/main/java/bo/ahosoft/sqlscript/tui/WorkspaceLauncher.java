package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.IOException;

public final class WorkspaceLauncher {

    private final TerminalSupport terminalSupport;
    private final WorkspaceRunner gui2Workspace;
    private final WorkspaceRunner fallbackWorkspace;

    public WorkspaceLauncher(TerminalSupport terminalSupport, WorkspaceRunner gui2Workspace, WorkspaceRunner fallbackWorkspace) {
        this.terminalSupport = terminalSupport;
        this.gui2Workspace = gui2Workspace;
        this.fallbackWorkspace = fallbackWorkspace;
    }

    public static WorkspaceLauncher forGui2(
        TerminalSupport terminalSupport,
        WorkspaceRunner gui2Workspace,
        WorkspaceRunner fallbackWorkspace
    ) {
        return new WorkspaceLauncher(terminalSupport, gui2Workspace, fallbackWorkspace);
    }

    public int run() throws IOException {
        return terminalSupport.isSupported() ? gui2Workspace.run() : fallbackWorkspace.run();
    }

    public interface TerminalSupport {
        boolean isSupported();
    }

    public interface WorkspaceRunner {
        int run() throws IOException;
    }
}
