package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.List;

public interface SqlEditorComponent {
    String text();

    int cursorOffset();

    void insertText(String text);

    void moveCursor(int delta);

    void deleteBeforeCursor();

    String currentStatement();

    String executionReadinessMessage();

    List<String> renderLines(int width, int height);

    SqlRenderModel renderModel(int width, int height);

    List<CompletionCandidate> completionCandidates();

    List<String> decorations();
}
