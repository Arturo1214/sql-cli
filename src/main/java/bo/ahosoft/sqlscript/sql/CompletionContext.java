package bo.ahosoft.sqlscript.sql;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class CompletionContext {

    private final String beforeCursor;
    private final String prefix;
    private final int replacementStart;
    private final int replacementEnd;

    private CompletionContext(String beforeCursor, String prefix, int replacementStart, int replacementEnd) {
        this.beforeCursor = beforeCursor;
        this.prefix = prefix;
        this.replacementStart = replacementStart;
        this.replacementEnd = replacementEnd;
    }

    public static CompletionContext from(String text, int cursorOffset) {
        String safeText = text == null ? "" : text;
        int safeCursor = Math.max(0, Math.min(cursorOffset, safeText.length()));
        int start = safeCursor;
        while (start > 0 && isIdentifierPart(safeText.charAt(start - 1))) {
            start--;
        }
        return new CompletionContext(safeText.substring(0, safeCursor), safeText.substring(start, safeCursor), start, safeCursor);
    }

    public String prefix() {
        return prefix;
    }

    public int replacementStart() {
        return replacementStart;
    }

    public int replacementEnd() {
        return replacementEnd;
    }

    public boolean atTableReferencePosition() {
        String leading = beforeCursor.substring(0, replacementStart).toLowerCase();
        return leading.matches("(?s).*\\b(from|join)\\s+$");
    }

    public boolean afterSelectKeyword() {
        String leading = beforeCursor.substring(0, replacementStart).toLowerCase();
        return leading.matches("(?s).*\\bselect\\s+$");
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$' || value == '#';
    }
}
