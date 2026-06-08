package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CommandDispatcher {

    private static final Set<String> COMMANDS = new HashSet<>(
        Arrays.asList(
            "init",
            "profiles",
            "connections",
            "validate",
            "workspace",
            "run-current",
            "history",
            "exec",
            "export",
            "tables",
            "search",
            "sample",
            "desc",
            "describe",
            "detail",
            "details",
            "indexes",
            "constraints",
            "fk-in",
            "fk-out",
            "explain",
            "count"
        )
    );

    private CommandDispatcher() {}

    public static boolean isHelp(String value) {
        return "help".equals(value) || "--help".equals(value) || "-h".equals(value);
    }

    public static boolean isCommand(String value) {
        return COMMANDS.contains(value) || isHelp(value);
    }
}
