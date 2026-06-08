package bo.ahosoft.sqlscript.cli;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;

public final class SafetyGuard {

    private static final String REQUIRED_CONFIRMATION = "YES";

    private SafetyGuard() {}

    public static void requireSafe(String script, boolean force, String confirmRisk) {
        if (!isDestructive(script)) {
            return;
        }
        if (!force) {
            throw new IllegalArgumentException(
                "Safe mode blocked a destructive SQL statement. Re-run with --force and --confirm-risk YES only if you intend to continue."
            );
        }
        if (!REQUIRED_CONFIRMATION.equals(confirmRisk)) {
            throw new IllegalArgumentException("Destructive SQL requires typed confirmation: --confirm-risk YES");
        }
    }

    public static boolean isDestructive(String script) {
        if (script == null) {
            return false;
        }
        String normalized = stripComments(script).trim().toLowerCase();
        return normalized.matches("(?s).*(^|[;\\s])(update|delete|insert|merge|drop|truncate|alter|create|grant|revoke)\\s+.*");
    }

    private static String stripComments(String script) {
        return script.replaceAll("(?m)--.*$", " ").replaceAll("(?s)/\\*.*?\\*/", " ");
    }
}
