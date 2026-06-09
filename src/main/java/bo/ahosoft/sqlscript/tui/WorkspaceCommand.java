package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class WorkspaceCommand {

    public enum Type {
        EMPTY,
        HELP,
        EXIT,
        CONNECTIONS,
        USE,
        NEW_CONNECTION,
        SCHEMAS,
        BUFFER,
        RUN,
        TABLES,
        SEARCH,
        DESC,
        DETAILS,
        INDEXES,
        CONSTRAINTS,
        FK_IN,
        FK_OUT,
        EXPLAIN,
        COUNT,
        SAMPLE,
        HISTORY,
        LIB_SAVE,
        LIB_LIST,
        LIB_SEARCH,
        LIB_LOAD,
        LIB_DELETE,
        LIB_FAVORITE,
        LIB_UNFAVORITE,
        UNKNOWN,
    }

    private final Type type;
    private final List<String> arguments;
    private final String argumentText;

    private WorkspaceCommand(Type type, List<String> arguments) {
        this(type, arguments, join(arguments));
    }

    private WorkspaceCommand(Type type, List<String> arguments, String argumentText) {
        this.type = type;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        this.argumentText = argumentText == null ? "" : argumentText;
    }

    public static WorkspaceCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new WorkspaceCommand(Type.EMPTY, Collections.<String>emptyList());
        }
        String trimmed = input.trim();
        List<String> tokens = new ArrayList<>(Arrays.asList(trimmed.split("\\s+")));
        String command = tokens.remove(0).toLowerCase(Locale.ROOT);
        String argumentText = trimmed.length() == command.length() ? "" : trimmed.substring(command.length()).trim();
        if ("help".equals(command)) {
            return new WorkspaceCommand(Type.HELP, tokens, argumentText);
        }
        if ("exit".equals(command) || "quit".equals(command)) {
            return new WorkspaceCommand(Type.EXIT, tokens, argumentText);
        }
        if ("connections".equals(command)) {
            return new WorkspaceCommand(Type.CONNECTIONS, tokens, argumentText);
        }
        if ("use".equals(command)) {
            return new WorkspaceCommand(Type.USE, tokens, argumentText);
        }
        if ("new".equals(command) && !tokens.isEmpty() && "connection".equals(tokens.get(0).toLowerCase(Locale.ROOT))) {
            tokens.remove(0);
            return new WorkspaceCommand(Type.NEW_CONNECTION, tokens, join(tokens));
        }
        if ("schemas".equals(command)) {
            return new WorkspaceCommand(Type.SCHEMAS, tokens, argumentText);
        }
        if ("buffer".equals(command) || "editor".equals(command)) {
            return new WorkspaceCommand(Type.BUFFER, tokens, argumentText);
        }
        if ("run".equals(command)) {
            return new WorkspaceCommand(Type.RUN, tokens, argumentText);
        }
        if ("tables".equals(command)) {
            return new WorkspaceCommand(Type.TABLES, tokens, argumentText);
        }
        if ("search".equals(command)) {
            return new WorkspaceCommand(Type.SEARCH, tokens, argumentText);
        }
        if ("desc".equals(command) || "describe".equals(command)) {
            return new WorkspaceCommand(Type.DESC, tokens, argumentText);
        }
        if ("detail".equals(command) || "details".equals(command)) {
            return new WorkspaceCommand(Type.DETAILS, tokens, argumentText);
        }
        if ("indexes".equals(command)) {
            return new WorkspaceCommand(Type.INDEXES, tokens, argumentText);
        }
        if ("constraints".equals(command)) {
            return new WorkspaceCommand(Type.CONSTRAINTS, tokens, argumentText);
        }
        if ("fk-in".equals(command)) {
            return new WorkspaceCommand(Type.FK_IN, tokens, argumentText);
        }
        if ("fk-out".equals(command)) {
            return new WorkspaceCommand(Type.FK_OUT, tokens, argumentText);
        }
        if ("explain".equals(command)) {
            return new WorkspaceCommand(Type.EXPLAIN, tokens, argumentText);
        }
        if ("count".equals(command)) {
            return new WorkspaceCommand(Type.COUNT, tokens, argumentText);
        }
        if ("sample".equals(command)) {
            return new WorkspaceCommand(Type.SAMPLE, tokens, argumentText);
        }
        if ("history".equals(command)) {
            return new WorkspaceCommand(Type.HISTORY, tokens, argumentText);
        }
        if ("lib".equals(command) || "library".equals(command)) {
            return parseLibraryCommand(tokens, argumentText);
        }
        return new WorkspaceCommand(Type.UNKNOWN, tokens, argumentText);
    }

    public Type type() {
        return type;
    }

    public List<String> arguments() {
        return arguments;
    }

    public String argumentText() {
        return argumentText;
    }

    public String argumentTextAfter(int argumentCount) {
        if (argumentCount >= arguments.size()) {
            return "";
        }
        return join(arguments.subList(argumentCount, arguments.size()));
    }

    private static String join(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(value);
        }
        return joined.toString();
    }

    private static WorkspaceCommand parseLibraryCommand(List<String> tokens, String argumentText) {
        if (tokens.isEmpty()) {
            return new WorkspaceCommand(Type.UNKNOWN, tokens, argumentText);
        }
        String subcommand = tokens.get(0).toLowerCase(Locale.ROOT);
        List<String> arguments = new ArrayList<>(tokens.subList(1, tokens.size()));
        String remaining = join(arguments);
        if ("save".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_SAVE, arguments, remaining);
        }
        if ("list".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_LIST, arguments, remaining);
        }
        if ("search".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_SEARCH, arguments, remaining);
        }
        if ("load".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_LOAD, arguments, remaining);
        }
        if ("delete".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_DELETE, arguments, remaining);
        }
        if ("favorite".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_FAVORITE, arguments, remaining);
        }
        if ("unfavorite".equals(subcommand)) {
            return new WorkspaceCommand(Type.LIB_UNFAVORITE, arguments, remaining);
        }
        return new WorkspaceCommand(Type.UNKNOWN, tokens, argumentText);
    }
}
