package bo.ahosoft.sqlscript.config;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class EditorStateStore {

    private final File file;
    private final int maxHistoryEntries;

    public EditorStateStore(File file, int maxHistoryEntries) {
        if (maxHistoryEntries < 1) {
            throw new IllegalArgumentException("maxHistoryEntries must be positive");
        }
        this.file = file;
        this.maxHistoryEntries = maxHistoryEntries;
    }

    public void save(EditorState state) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create editor state directory: " + parent.getAbsolutePath());
        }
        List<String> history = bounded(state.history());
        Properties properties = new Properties();
        properties.setProperty("buffer", encode(state.buffer()));
        properties.setProperty("history.count", String.valueOf(history.size()));
        for (int i = 0; i < history.size(); i++) {
            properties.setProperty("history." + i, encode(history.get(i)));
        }
        FileOutputStream output = new FileOutputStream(file);
        try {
            properties.store(output, "Database Script CLI editor state");
        } finally {
            output.close();
        }
    }

    public EditorState load() throws IOException {
        if (!file.isFile()) {
            return new EditorState("", Collections.<String>emptyList());
        }
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(file);
        try {
            properties.load(input);
        } finally {
            input.close();
        }
        String buffer = decode(properties.getProperty("buffer", ""));
        int count = parseCount(properties.getProperty("history.count"));
        List<String> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = properties.getProperty("history." + i);
            if (value != null) {
                history.add(decode(value));
            }
        }
        return new EditorState(buffer, bounded(history));
    }

    public void saveBuffer(String buffer) throws IOException {
        EditorState current = load();
        save(new EditorState(buffer, current.history()));
    }

    public void recordHistory(String sql, boolean failed) throws IOException {
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        EditorState current = load();
        List<String> history = new ArrayList<>(current.history());
        history.add(sql);
        save(new EditorState(current.buffer(), history));
    }

    private List<String> bounded(List<String> history) {
        int from = Math.max(0, history.size() - maxHistoryEntries);
        return new ArrayList<>(history.subList(from, history.size()));
    }

    private static int parseCount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static final class EditorState {

        private final String buffer;
        private final List<String> history;

        public EditorState(String buffer, List<String> history) {
            this.buffer = buffer == null ? "" : buffer;
            this.history = Collections.unmodifiableList(new ArrayList<>(history));
        }

        public String buffer() {
            return buffer;
        }

        public List<String> history() {
            return history;
        }
    }
}
