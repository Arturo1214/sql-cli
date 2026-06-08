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

public class WorkspaceCommandTest {

    @Test
    public void parsesNavigationCommandsCaseInsensitively() {
        assertEquals(WorkspaceCommand.Type.HELP, WorkspaceCommand.parse("HELP").type());
        assertEquals(WorkspaceCommand.Type.EXIT, WorkspaceCommand.parse("quit").type());
        assertEquals(WorkspaceCommand.Type.CONNECTIONS, WorkspaceCommand.parse("connections").type());
    }

    @Test
    public void parsesConnectionCommandsWithArguments() {
        WorkspaceCommand use = WorkspaceCommand.parse("use reporting");
        WorkspaceCommand create = WorkspaceCommand.parse("new connection postgresql reporting");

        assertEquals(WorkspaceCommand.Type.USE, use.type());
        assertEquals(Arrays.asList("reporting"), use.arguments());
        assertEquals(WorkspaceCommand.Type.NEW_CONNECTION, create.type());
        assertEquals(Arrays.asList("postgresql", "reporting"), create.arguments());
    }

    @Test
    public void parsesSchemaBufferAndRunCommands() {
        assertEquals(WorkspaceCommand.Type.SCHEMAS, WorkspaceCommand.parse("schemas use public,audit").type());
        assertEquals(Arrays.asList("use", "public,audit"), WorkspaceCommand.parse("schemas use public,audit").arguments());
        assertEquals(WorkspaceCommand.Type.BUFFER, WorkspaceCommand.parse("buffer append select 1").type());
        assertEquals(Arrays.asList("append", "select", "1"), WorkspaceCommand.parse("buffer append select 1").arguments());
        assertEquals(WorkspaceCommand.Type.RUN, WorkspaceCommand.parse("run --force --confirm-risk YES").type());
    }

    @Test
    public void parsesMetadataAndHistoryCommands() {
        assertEquals(WorkspaceCommand.Type.TABLES, WorkspaceCommand.parse("tables user").type());
        assertEquals(WorkspaceCommand.Type.SEARCH, WorkspaceCommand.parse("search customer").type());
        assertEquals(WorkspaceCommand.Type.DESC, WorkspaceCommand.parse("desc users").type());
        assertEquals(WorkspaceCommand.Type.DETAILS, WorkspaceCommand.parse("details users").type());
        assertEquals(WorkspaceCommand.Type.INDEXES, WorkspaceCommand.parse("indexes users").type());
        assertEquals(WorkspaceCommand.Type.CONSTRAINTS, WorkspaceCommand.parse("constraints users").type());
        assertEquals(WorkspaceCommand.Type.FK_IN, WorkspaceCommand.parse("fk-in users").type());
        assertEquals(WorkspaceCommand.Type.FK_OUT, WorkspaceCommand.parse("fk-out users").type());
        assertEquals(WorkspaceCommand.Type.EXPLAIN, WorkspaceCommand.parse("explain select * from users").type());
        assertEquals(WorkspaceCommand.Type.COUNT, WorkspaceCommand.parse("count users").type());
        assertEquals(WorkspaceCommand.Type.SAMPLE, WorkspaceCommand.parse("sample users 5").type());
        assertEquals(WorkspaceCommand.Type.HISTORY, WorkspaceCommand.parse("history").type());
    }

    @Test
    public void preservesRemainderForSqlBufferAndExplainCommands() {
        WorkspaceCommand buffer = WorkspaceCommand.parse("buffer set select * from users where name = 'Ada Lovelace'");
        WorkspaceCommand explain = WorkspaceCommand.parse("explain select * from users where active = true");

        assertEquals("select * from users where name = 'Ada Lovelace'", buffer.argumentTextAfter(1));
        assertEquals("select * from users where active = true", explain.argumentText());
    }

    @Test
    public void treatsBlankInputAsEmptyCommand() {
        assertEquals(WorkspaceCommand.Type.EMPTY, WorkspaceCommand.parse("   ").type());
        assertTrue(WorkspaceCommand.parse(null).arguments().isEmpty());
    }
}
