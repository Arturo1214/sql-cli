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
        requireSafe(script, ConnectionEnvironment.DEV, null, force, false, confirmRisk);
    }

    public static void requireSafe(
        String script,
        ConnectionEnvironment environment,
        String connectionName,
        boolean force,
        boolean unsafe,
        String confirmRisk
    ) {
        if (!isDestructive(script)) {
            return;
        }
        ConnectionEnvironment effectiveEnvironment = environment == null ? ConnectionEnvironment.DEV : environment;
        String confirmationTarget = effectiveEnvironment.isProduction() ? safeConnectionName(connectionName) : REQUIRED_CONFIRMATION;
        if (unsafe) {
            if (!effectiveEnvironment.isProduction() || confirmationTarget.equals(confirmRisk)) {
                return;
            }
            throw new IllegalArgumentException(
                "Safety mode blocked a dangerous SQL statement in PROD. Re-run with --unsafe --confirm-risk " +
                confirmationTarget +
                " only if you intend to continue."
            );
        }
        if (!force) {
            throw new IllegalArgumentException(
                "Safety mode blocked a dangerous SQL statement. Re-run with --unsafe for non-PROD, or --unsafe --confirm-risk " +
                confirmationTarget +
                " for PROD, only if you intend to continue."
            );
        }
        if (!confirmationTarget.equals(confirmRisk)) {
            throw new IllegalArgumentException("Dangerous SQL requires typed confirmation: --confirm-risk " + confirmationTarget);
        }
    }

    public static boolean isAllowedByDefault(String script) {
        return !isDestructive(script);
    }

    public static boolean isDestructive(String script) {
        if (script == null) {
            return false;
        }
        String normalized = stripComments(script).trim().toLowerCase();
        return normalized.matches("(?s).*(^|[;\\s])(update|delete|insert|merge|drop|truncate|alter|create|grant|revoke)\\s+.*");
    }

    public static String destructiveOperation(String script) {
        if (script == null) {
            return "SQL";
        }
        String normalized = stripComments(script).trim().toLowerCase();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "(?s)(^|[;\\s])(update|delete|insert|merge|drop|truncate|alter|create|grant|revoke)\\s+.*"
        ).matcher(normalized);
        return matcher.find() ? matcher.group(2).toUpperCase(java.util.Locale.ROOT) : "SQL";
    }

    public static String requiredConfirmation(ConnectionEnvironment environment, String connectionName) {
        ConnectionEnvironment effectiveEnvironment = environment == null ? ConnectionEnvironment.DEV : environment;
        return effectiveEnvironment.isProduction() ? safeConnectionName(connectionName) : "RUN";
    }

    private static String safeConnectionName(String connectionName) {
        return connectionName == null || connectionName.trim().isEmpty() ? REQUIRED_CONFIRMATION : connectionName.trim();
    }

    private static String stripComments(String script) {
        return script.replaceAll("(?m)--.*$", " ").replaceAll("(?s)/\\*.*?\\*/", " ");
    }
}
