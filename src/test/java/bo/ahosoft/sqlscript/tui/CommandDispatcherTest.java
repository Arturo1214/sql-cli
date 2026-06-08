package bo.ahosoft.sqlscript.tui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import org.junit.Test;

public class CommandDispatcherTest {

    @Test
    public void recognizesExistingCommands() {
        assertTrue(CommandDispatcher.isCommand("init"));
        assertTrue(CommandDispatcher.isCommand("exec"));
        assertTrue(CommandDispatcher.isCommand("tables"));
        assertTrue(CommandDispatcher.isCommand("history"));
        assertTrue(CommandDispatcher.isCommand("workspace"));
        assertTrue(CommandDispatcher.isCommand("run-current"));
        assertTrue(CommandDispatcher.isCommand("connections"));
        assertTrue(CommandDispatcher.isCommand("validate"));
    }

    @Test
    public void recognizesHelpAliasesAsCommands() {
        assertTrue(CommandDispatcher.isCommand("help"));
        assertTrue(CommandDispatcher.isCommand("--help"));
        assertTrue(CommandDispatcher.isCommand("-h"));
    }

    @Test
    public void rejectsUnknownCommands() {
        assertFalse(CommandDispatcher.isCommand("unknown"));
    }
}
