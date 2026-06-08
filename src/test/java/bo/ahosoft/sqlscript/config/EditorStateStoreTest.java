package bo.ahosoft.sqlscript.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EditorStateStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void restoresSavedBufferAndHistory() throws Exception {
        EditorStateStore store = new EditorStateStore(temporaryFolder.newFile("editor.properties"), 5);

        store.save(new EditorStateStore.EditorState("select * from users", Arrays.asList("select 1 from dual")));

        EditorStateStore.EditorState restored = store.load();
        assertEquals("select * from users", restored.buffer());
        assertEquals(Arrays.asList("select 1 from dual"), restored.history());
    }

    @Test
    public void keepsHistoryBoundedToMostRecentEntries() throws Exception {
        EditorStateStore store = new EditorStateStore(temporaryFolder.newFile("bounded-editor.properties"), 2);

        store.save(new EditorStateStore.EditorState("current", Arrays.asList("first", "second", "third")));

        assertEquals(Arrays.asList("second", "third"), store.load().history());
    }

    @Test
    public void preservesFailedQueryInHistoryForCorrection() throws Exception {
        EditorStateStore store = new EditorStateStore(temporaryFolder.newFile("failed-editor.properties"), 5);

        store.recordHistory("select * from missing_table", true);

        store.saveBuffer("select * from missing_table where id = 1");

        EditorStateStore.EditorState restored = store.load();

        assertEquals("select * from missing_table where id = 1", restored.buffer());
        assertEquals(Arrays.asList("select * from missing_table"), restored.history());
    }

    @Test
    public void storesEditorStateSeparatelyFromExecutionOutput() throws Exception {
        File file = temporaryFolder.newFile("output-separation.properties");
        EditorStateStore store = new EditorStateStore(file, 5);

        store.save(new EditorStateStore.EditorState("select * from users", Arrays.asList("select * from users")));

        String stored = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        assertFalse(stored.contains("RESULT_TABLE"));
        assertFalse(stored.contains("rows returned"));
    }
}
