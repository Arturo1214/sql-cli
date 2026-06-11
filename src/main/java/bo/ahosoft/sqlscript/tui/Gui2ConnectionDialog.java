package bo.ahosoft.sqlscript.tui;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Gui2ConnectionDialog {

    private final InteractiveWorkspace.Session session;
    private final TuiMessages messages;

    public Gui2ConnectionDialog(InteractiveWorkspace.Session session) {
        this(session, new TuiMessages(TuiLanguage.ENGLISH));
    }

    public Gui2ConnectionDialog(InteractiveWorkspace.Session session, TuiMessages messages) {
        this.session = session;
        this.messages = messages == null ? new TuiMessages(TuiLanguage.ENGLISH) : messages;
    }

    public Form open(DatabaseType databaseType) {
        return new Form(this, Mode.CREATE, null, databaseType, null);
    }

    public Form openEdit(String name, ConnectionConfig existing) {
        if (existing == null) {
            return new Form(this, Mode.EDIT, name, DatabaseType.ORACLE, null);
        }
        return new Form(this, Mode.EDIT, name, existing.databaseType(), existing);
    }

    public Result submit(Request request) {
        String validationError = validate(request);
        if (validationError != null) {
            return Result.rejected(validationError);
        }
        try {
            if (request.mode() == Mode.EDIT) {
                ConnectionConfig config = toConfig(request);
                ConnectionConfig updated = session.editConnectionFromAction(request.oldName(), trim(request.name()), config);
                return Result.created("Connection updated: " + trim(request.name()), updated);
            }
            ConnectionConfig config = session.createConnectionFromAction(
                trim(request.name()),
                request.databaseType(),
                request.environment(),
                trim(request.jdbcUrl()),
                trim(request.username()),
                request.passwordDraft().resolve(null),
                request.selectedSchemas(),
                request.availableSchemas()
            );
            return Result.created(messages.connectionSaved(trim(request.name())), config);
        } catch (IOException ex) {
            return Result.rejected(ex.getMessage());
        }
    }

    public Result test(Request request, long timeoutMillis) {
        String validationError = validate(request);
        if (validationError != null) {
            return Result.rejected(validationError);
        }
        ConnectionTestResult result = session.testConnectionFromAction(toConfig(request), timeoutMillis);
        return Result.rejected(result.message());
    }

    public Result cancel() {
        return Result.rejected(messages.connectionCancelled());
    }

    public static final class Form {

        private static final List<String> FIELD_LABELS = Collections.unmodifiableList(
            Arrays.asList("Name", "Environment", "JDBC URL", "Username", "Password", "Schemas")
        );

        private final Gui2ConnectionDialog dialog;
        private final Mode mode;
        private final String oldName;
        private final DatabaseType databaseType;
        private ConnectionEnvironment environment = ConnectionEnvironment.DEV;
        private String name;
        private String jdbcUrl;
        private String username;
        private String password;
        private String existingPassword;
        private boolean revealPassword;
        private List<String> selectedSchemas = Collections.emptyList();
        private List<String> availableSchemas = Collections.emptyList();
        private String feedback;

        private Form(Gui2ConnectionDialog dialog, Mode mode, String oldName, DatabaseType databaseType, ConnectionConfig existing) {
            this.dialog = dialog;
            this.mode = mode == null ? Mode.CREATE : mode;
            this.oldName = oldName;
            this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
            this.feedback = dialog.messages.connectionWizard(this.databaseType);
            if (existing != null) {
                this.name = oldName;
                this.environment = existing.environment();
                this.jdbcUrl = existing.jdbcUrl();
                this.username = existing.username();
                this.existingPassword = existing.password();
                this.selectedSchemas = existing.schemas();
                this.availableSchemas = existing.schemas();
            }
        }

        public Mode mode() {
            return mode;
        }

        public DatabaseType databaseType() {
            return databaseType;
        }

        public ConnectionEnvironment environment() {
            return environment;
        }

        public List<String> fieldLabels() {
            return FIELD_LABELS;
        }

        public String feedback() {
            return feedback;
        }

        public String name() {
            return name;
        }

        public String jdbcUrl() {
            return jdbcUrl;
        }

        public String username() {
            return username;
        }

        public List<String> selectedSchemas() {
            return selectedSchemas;
        }

        public Form name(String value) {
            this.name = value;
            return this;
        }

        public Form environment(ConnectionEnvironment value) {
            this.environment = value == null ? ConnectionEnvironment.DEV : value;
            return this;
        }

        public Form jdbcUrl(String value) {
            this.jdbcUrl = value;
            return this;
        }

        public Form username(String value) {
            this.username = value;
            return this;
        }

        public Form password(String value) {
            this.password = value;
            this.revealPassword = false;
            return this;
        }

        public Form togglePasswordReveal() {
            this.revealPassword = !this.revealPassword;
            return this;
        }

        public PasswordDraft passwordDraft() {
            if (mode == Mode.EDIT && isBlank(password)) {
                return PasswordDraft.preserveExisting();
            }
            return PasswordDraft.replaceWith(password);
        }

        public String passwordDisplayValue() {
            PasswordDraft draft = passwordDraft();
            if (draft.preservesExisting()) {
                return "";
            }
            String value = draft.resolve(existingPassword);
            if (value == null) {
                return "";
            }
            if (revealPassword) {
                return value;
            }
            char[] masked = new char[value.length()];
            Arrays.fill(masked, '*');
            return new String(masked);
        }

        public Form selectedSchemas(List<String> values) {
            this.selectedSchemas = values == null ? Collections.<String>emptyList() : values;
            return this;
        }

        public Form availableSchemas(List<String> values) {
            this.availableSchemas = values == null ? Collections.<String>emptyList() : values;
            return this;
        }

        public Result save() {
            Result result = dialog.submit(request());
            feedback = result.message();
            return result;
        }

        public Result testConnection(long timeoutMillis) {
            Result result = dialog.test(request(), timeoutMillis);
            feedback = result.message();
            return result;
        }

        public Result cancel() {
            Result result = dialog.cancel();
            feedback = result.message();
            return result;
        }

        public Result back() {
            return cancel();
        }

        private Request request() {
            return new Request(
                mode,
                oldName,
                databaseType,
                environment,
                name,
                jdbcUrl,
                username,
                passwordDraft(),
                existingPassword,
                selectedSchemas,
                availableSchemas
            );
        }
    }

    private String validate(Request request) {
        if (request == null) {
            return messages.connectionRequestRequired();
        }
        if (isBlank(request.name())) {
            return messages.connectionNameRequired();
        }
        if (isBlank(request.jdbcUrl())) {
            return messages.jdbcUrlRequired();
        }
        if (!request.databaseType().acceptsJdbcUrl(request.jdbcUrl())) {
            return messages.jdbcUrlDoesNotMatch(request.databaseType());
        }
        if (isBlank(request.username())) {
            return messages.usernameRequired();
        }
        if (request.mode() == Mode.CREATE && request.passwordDraft().resolve(null) == null) {
            return messages.passwordRequired();
        }
        return null;
    }

    private ConnectionConfig toConfig(Request request) {
        return new ConnectionConfig(
            request.databaseType(),
            request.environment(),
            trim(request.jdbcUrl()),
            trim(request.username()),
            request.passwordDraft().resolve(request.existingPassword()),
            resolveSchemas(request.databaseType(), request.selectedSchemas(), request.availableSchemas())
        );
    }

    private static List<String> resolveSchemas(DatabaseType databaseType, List<String> selectedSchemas, List<String> availableSchemas) {
        List<String> schemas = selectedSchemas == null ? Collections.<String>emptyList() : selectedSchemas;
        if (
            databaseType == DatabaseType.POSTGRESQL && schemas.isEmpty() && availableSchemas != null && availableSchemas.contains("public")
        ) {
            return Arrays.asList("public");
        }
        return schemas;
    }

    private static String databaseTypeLabel(DatabaseType databaseType) {
        if (databaseType == DatabaseType.POSTGRESQL) {
            return "PostgreSQL";
        }
        return "Oracle";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static final class Request {

        private final Mode mode;
        private final String oldName;
        private final DatabaseType databaseType;
        private final ConnectionEnvironment environment;
        private final String name;
        private final String jdbcUrl;
        private final String username;
        private final PasswordDraft passwordDraft;
        private final String existingPassword;
        private final List<String> selectedSchemas;
        private final List<String> availableSchemas;

        public Request(
            DatabaseType databaseType,
            String name,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) {
            this(databaseType, ConnectionEnvironment.DEV, name, jdbcUrl, username, password, selectedSchemas, availableSchemas);
        }

        public Request(
            DatabaseType databaseType,
            ConnectionEnvironment environment,
            String name,
            String jdbcUrl,
            String username,
            String password,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) {
            this(
                Mode.CREATE,
                null,
                databaseType,
                environment,
                name,
                jdbcUrl,
                username,
                PasswordDraft.replaceWith(password),
                null,
                selectedSchemas,
                availableSchemas
            );
        }

        public Request(
            Mode mode,
            String oldName,
            DatabaseType databaseType,
            ConnectionEnvironment environment,
            String name,
            String jdbcUrl,
            String username,
            PasswordDraft passwordDraft,
            String existingPassword,
            List<String> selectedSchemas,
            List<String> availableSchemas
        ) {
            this.mode = mode == null ? Mode.CREATE : mode;
            this.oldName = oldName;
            this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
            this.environment = environment == null ? ConnectionEnvironment.DEV : environment;
            this.name = name;
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.passwordDraft = passwordDraft == null ? PasswordDraft.replaceWith(null) : passwordDraft;
            this.existingPassword = existingPassword;
            this.selectedSchemas = selectedSchemas == null ? Collections.<String>emptyList() : selectedSchemas;
            this.availableSchemas = availableSchemas == null ? Collections.<String>emptyList() : availableSchemas;
        }

        public Mode mode() {
            return mode;
        }

        public String oldName() {
            return oldName;
        }

        public DatabaseType databaseType() {
            return databaseType;
        }

        public ConnectionEnvironment environment() {
            return environment;
        }

        public String name() {
            return name;
        }

        public String jdbcUrl() {
            return jdbcUrl;
        }

        public String username() {
            return username;
        }

        public String password() {
            return passwordDraft.resolve(existingPassword);
        }

        public PasswordDraft passwordDraft() {
            return passwordDraft;
        }

        public String existingPassword() {
            return existingPassword;
        }

        public List<String> selectedSchemas() {
            return selectedSchemas;
        }

        public List<String> availableSchemas() {
            return availableSchemas;
        }
    }

    public enum Mode {
        CREATE,
        EDIT,
    }

    public static final class PasswordDraft {

        private final boolean preserveExisting;
        private final String value;

        private PasswordDraft(boolean preserveExisting, String value) {
            this.preserveExisting = preserveExisting;
            this.value = value;
        }

        public static PasswordDraft preserveExisting() {
            return new PasswordDraft(true, null);
        }

        public static PasswordDraft replaceWith(String value) {
            return new PasswordDraft(false, value);
        }

        public boolean preservesExisting() {
            return preserveExisting;
        }

        public String resolve(String existingPassword) {
            return preserveExisting ? existingPassword : value;
        }
    }

    public static final class Result {

        private final boolean created;
        private final String message;
        private final ConnectionConfig config;

        private Result(boolean created, String message, ConnectionConfig config) {
            this.created = created;
            this.message = message;
            this.config = config;
        }

        public static Result created(String message, ConnectionConfig config) {
            return new Result(true, message, config);
        }

        public static Result rejected(String message) {
            return new Result(false, message, null);
        }

        public boolean created() {
            return created;
        }

        public String message() {
            return message;
        }

        public ConnectionConfig config() {
            return config;
        }
    }
}
