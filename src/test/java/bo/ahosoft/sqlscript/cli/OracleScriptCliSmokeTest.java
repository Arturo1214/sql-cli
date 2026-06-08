package bo.ahosoft.sqlscript.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.Test;

public class OracleScriptCliSmokeTest {

    @Test
    public void helpListsWorkspaceCommands() throws Exception {
        ProcessResult result = runCli("--help");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output.contains("workspace"));
        assertTrue(result.output.contains("run-current"));
        assertTrue(result.output.contains("connections"));
        assertTrue(result.output.contains("validate"));
    }

    @Test
    public void helpMentionsWorkspaceShortcutsAndConnectionForm() throws Exception {
        ProcessResult result = runCli("--help");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output.contains("GUI2 split-pane workspace"));
        assertTrue(result.output.contains("Ctrl+R runs the visible SQL buffer"));
        assertTrue(result.output.contains("Enter activates the selected connection or explorer action"));
        assertTrue(result.output.contains("New Oracle/New PostgreSQL opens the connection form"));
    }

    @Test
    public void noArgsLaunchesWorkspaceAndExitsFromStdin() throws Exception {
        RecordingWorkspaceRunner workspace = new RecordingWorkspaceRunner(0);

        OracleScriptCli.run(new String[0], workspace);

        assertEquals(1, workspace.calls);
    }

    @Test
    public void noArgsUsesCompactFallbackWithoutAnsiWhenAnsiIsNotAvailable() throws Exception {
        ProcessResult result = runCliWithInput("exit\n");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output.contains("Interactive Workspace (Compact)"));
        assertTrue(result.output.contains("Connections:"));
        assertTrue(result.output.contains("SQL Buffer:"));
        assertTrue(result.output.contains("Results:"));
        assertFalse(result.output.contains("\u001B[2J\u001B[H"));
    }

    @Test
    public void workspaceAliasLaunchesWorkspaceAndExitsFromStdin() throws Exception {
        RecordingWorkspaceRunner workspace = new RecordingWorkspaceRunner(0);

        OracleScriptCli.run(new String[] { "workspace" }, workspace);

        assertEquals(1, workspace.calls);
    }

    @Test
    public void explicitCommandsBypassInteractiveWorkspaceLauncher() throws Exception {
        RecordingWorkspaceRunner workspace = new RecordingWorkspaceRunner(0);
        String buffer = "select * from users;\nselect * from orders;";

        OracleScriptCli.run(
            new String[] { "run-current", "--buffer", buffer, "--cursor", String.valueOf(buffer.indexOf("orders")), "--dry-run" },
            workspace
        );

        assertEquals(0, workspace.calls);
    }

    @Test
    public void runCurrentKeepsOneShotWorkspaceDryRunForScripts() throws Exception {
        String buffer = "select * from users;\nselect * from orders;";
        ProcessResult result = runCli("run-current", "--buffer", buffer, "--cursor", String.valueOf(buffer.indexOf("orders")), "--dry-run");

        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output.contains("Selected statement:"));
        assertTrue(result.output.contains("select * from orders"));
        assertFalse(result.output.contains("select * from users"));
        assertFalse(result.output.contains("Interactive Workspace"));
        assertFalse(result.output.contains("SQL Buffer"));
    }

    @Test
    public void packageDeclaresAcceptedLanternaTerminalDependencyOnly() throws Exception {
        String pom = readAll(new FileInputStream(new File("pom.xml"))).toLowerCase();

        assertTrue(pom.contains("com.googlecode.lanterna"));
        Class.forName("com.googlecode.lanterna.input.KeyStroke");
        assertFalse(pom.contains("jline"));
        assertFalse(pom.contains("curses"));
        assertFalse(pom.contains("terminal-ui"));
    }

    @Test
    public void workspaceOneShotOptionsPointScriptsToRunCurrent() throws Exception {
        String buffer = "select * from users;\nselect * from orders;";
        ProcessResult result = runCli("workspace", "--buffer", buffer, "--cursor", String.valueOf(buffer.indexOf("orders")), "--dry-run");

        assertEquals(result.output, 2, result.exitCode);
        assertTrue(result.output.contains("Use run-current for one-shot workspace execution"));
        assertTrue(result.output.contains("run-current --buffer"));
        assertFalse(result.output.contains("Selected statement:"));
        assertFalse(result.output.contains("select * from users"));
    }

    @Test
    public void workspaceDryRunRequiresTypedConfirmationForDestructiveStatement() throws Exception {
        ProcessResult blocked = runCli("run-current", "--buffer", "delete from users", "--cursor", "0", "--dry-run", "--force");
        ProcessResult confirmed = runCli(
            "run-current",
            "--buffer",
            "delete from users",
            "--cursor",
            "0",
            "--dry-run",
            "--force",
            "--confirm-risk",
            "YES"
        );

        assertEquals(blocked.output, 2, blocked.exitCode);
        assertTrue(blocked.output.contains("--confirm-risk YES"));
        assertEquals(confirmed.output, 0, confirmed.exitCode);
        assertTrue(confirmed.output.contains("delete from users"));
    }

    @Test
    public void postgreSqlJdbcDriverIsAvailableForPackagedRuntime() throws Exception {
        Class.forName("org.postgresql.Driver");
    }

    @Test
    public void initPersistsPostgreSqlTypeAndSchemasFromFlags() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-postgres", ".properties");
        configFile.delete();

        ProcessResult result = runCli(
            "init",
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            "--type",
            "postgresql",
            "--schemas",
            "app,audit",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(result.output, 0, result.exitCode);
        Properties properties = load(configFile);
        assertEquals("POSTGRESQL", properties.getProperty("type"));
        assertEquals("app,audit", properties.getProperty("schemas"));
        assertTrue(result.output.contains("POSTGRESQL"));
    }

    @Test
    public void missingPostgreSqlDriverFailsClearly() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-missing-postgres", ".properties");
        configFile.delete();

        ProcessResult result = runCli(
            Arrays.<String>asList(),
            classpathWithoutPostgreSqlDriver(),
            "init",
            "jdbc:postgresql://localhost:5432/app",
            "pg_user",
            "pg-secret",
            "--type",
            "postgresql",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(result.output, 2, result.exitCode);
        assertTrue(result.output.contains("No suitable driver"));
        assertFalse(result.output.contains("pg-secret"));
    }

    @Test
    public void initWithoutPostgreSqlSchemasPersistsPublic() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-postgres-public", ".properties");
        configFile.delete();

        ProcessResult result = runCli(
            Arrays.asList("-DoracleScriptCli.testDriver=" + StubPostgresDriver.class.getName()),
            System.getProperty("java.class.path"),
            "init",
            StubPostgresDriver.URL,
            "pg_user",
            "pg-secret",
            "--type",
            "postgresql",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(result.output, 0, result.exitCode);
        Properties properties = load(configFile);
        assertEquals("POSTGRESQL", properties.getProperty("type"));
        assertEquals("public", properties.getProperty("schemas"));
        assertTrue(result.output.contains("Schemas: public"));
        assertFalse(result.output.contains("pg-secret"));
    }

    @Test
    public void initRejectsMismatchedTypeAndJdbcUrlClearly() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-invalid", ".properties");
        configFile.delete();

        ProcessResult result = runCli(
            "init",
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "pg_user",
            "pg-secret",
            "--type",
            "postgresql",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(result.output, 2, result.exitCode);
        assertTrue(result.output.contains("JDBC URL does not match database type: POSTGRESQL"));
        assertFalse(result.output.contains("pg-secret"));
    }

    @Test
    public void initDefaultsToOracleWhenTypeFlagIsOmitted() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-oracle", ".properties");
        configFile.delete();

        ProcessResult result = runCli(
            "init",
            "jdbc:oracle:thin:@localhost:1521/XEPDB1",
            "ora_user",
            "ora-secret",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(result.output, 0, result.exitCode);
        Properties properties = load(configFile);
        assertEquals("ORACLE", properties.getProperty("type"));
        assertFalse(properties.containsKey("schemas"));
    }

    @Test
    public void workspaceDryRunKeepsDestructiveConfirmationForPostgreSqlProfiles() throws Exception {
        File configFile = File.createTempFile("oracle-script-cli-workspace-postgres", ".properties");
        ConfigStore.save(
            configFile,
            new ConnectionConfig(
                DatabaseType.POSTGRESQL,
                "jdbc:postgresql://localhost:5432/app",
                "pg_user",
                "pg-secret",
                Arrays.asList("public")
            )
        );

        ProcessResult blocked = runCli(
            "run-current",
            "--buffer",
            "delete from customers",
            "--dry-run",
            "--force",
            "--config",
            configFile.getAbsolutePath()
        );
        ProcessResult confirmed = runCli(
            "run-current",
            "--buffer",
            "delete from customers",
            "--dry-run",
            "--force",
            "--confirm-risk",
            "YES",
            "--config",
            configFile.getAbsolutePath()
        );

        assertEquals(blocked.output, 2, blocked.exitCode);
        assertTrue(blocked.output.contains("--confirm-risk YES"));
        assertEquals(confirmed.output, 0, confirmed.exitCode);
        assertTrue(confirmed.output.contains("delete from customers"));
        assertFalse(confirmed.output.contains("pg-secret"));
    }

    private static Properties load(File file) throws Exception {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(file);
        try {
            properties.load(input);
        } finally {
            input.close();
        }
        return properties;
    }

    private static ProcessResult runCli(String... args) throws Exception {
        return runCli(Arrays.<String>asList(), System.getProperty("java.class.path"), args);
    }

    private static ProcessResult runCliWithInput(String stdin, String... args) throws Exception {
        return runCliWithOptionalInput(Arrays.<String>asList(), System.getProperty("java.class.path"), stdin, args);
    }

    private static ProcessResult runCli(List<String> jvmArgs, String classpath, String... args) throws Exception {
        return runCliWithOptionalInput(jvmArgs, classpath, null, args);
    }

    private static ProcessResult runCliWithOptionalInput(List<String> jvmArgs, String classpath, String stdin, String... args)
        throws Exception {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + "/bin/java");
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(classpath);
        command.add("bo.ahosoft.sqlscript.cli.OracleScriptCli");
        command.addAll(Arrays.asList(args));

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        if (stdin != null) {
            process.getOutputStream().write(stdin.getBytes("UTF-8"));
        }
        process.getOutputStream().close();
        String output = readAll(process.getInputStream());
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, output);
    }

    private static String classpathWithoutPostgreSqlDriver() {
        String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
        StringBuilder filtered = new StringBuilder();
        for (String entry : entries) {
            if (isPostgreSqlDriverClasspathEntry(entry)) {
                continue;
            }
            if (filtered.length() > 0) {
                filtered.append(File.pathSeparator);
            }
            filtered.append(entry);
        }
        return filtered.toString();
    }

    private static boolean isPostgreSqlDriverClasspathEntry(String entry) {
        String normalized = entry.replace('\\', '/');
        return normalized.contains("/org/postgresql/") || normalized.contains("postgresql-");
    }

    private static String readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), "UTF-8");
    }

    private static final class ProcessResult {

        private final int exitCode;
        private final String output;

        private ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private static final class RecordingWorkspaceRunner implements WorkspaceLauncher.WorkspaceRunner {

        private final int exitCode;
        private int calls;

        RecordingWorkspaceRunner(int exitCode) {
            this.exitCode = exitCode;
        }

        public int run() {
            calls++;
            return exitCode;
        }
    }
}
