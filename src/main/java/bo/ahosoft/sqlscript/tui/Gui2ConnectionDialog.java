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
        return new Form(this, databaseType);
    }

    public Result submit(Request request) {
        String validationError = validate(request);
        if (validationError != null) {
            return Result.rejected(validationError);
        }
        try {
            ConnectionConfig config = session.createConnectionFromAction(
                trim(request.name()),
                request.databaseType(),
                trim(request.jdbcUrl()),
                trim(request.username()),
                request.password(),
                request.selectedSchemas(),
                request.availableSchemas()
            );
            return Result.created(messages.connectionSaved(trim(request.name())), config);
        } catch (IOException ex) {
            return Result.rejected(ex.getMessage());
        }
    }

    public Result cancel() {
        return Result.rejected(messages.connectionCancelled());
    }

    public static final class Form {

        private static final List<String> FIELD_LABELS = Collections.unmodifiableList(
            Arrays.asList("Name", "JDBC URL", "Username", "Password", "Schemas")
        );

        private final Gui2ConnectionDialog dialog;
        private final DatabaseType databaseType;
        private String name;
        private String jdbcUrl;
        private String username;
        private String password;
        private List<String> selectedSchemas = Collections.emptyList();
        private List<String> availableSchemas = Collections.emptyList();
        private String feedback;

        private Form(Gui2ConnectionDialog dialog, DatabaseType databaseType) {
            this.dialog = dialog;
            this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
            this.feedback = dialog.messages.connectionWizard(this.databaseType);
        }

        public DatabaseType databaseType() {
            return databaseType;
        }

        public List<String> fieldLabels() {
            return FIELD_LABELS;
        }

        public String feedback() {
            return feedback;
        }

        public Form name(String value) {
            this.name = value;
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
            return this;
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
            Result result = dialog.submit(new Request(databaseType, name, jdbcUrl, username, password, selectedSchemas, availableSchemas));
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
        if (request.password() == null) {
            return messages.passwordRequired();
        }
        return null;
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

        private final DatabaseType databaseType;
        private final String name;
        private final String jdbcUrl;
        private final String username;
        private final String password;
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
            this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
            this.name = name;
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.selectedSchemas = selectedSchemas == null ? Collections.<String>emptyList() : selectedSchemas;
            this.availableSchemas = availableSchemas == null ? Collections.<String>emptyList() : availableSchemas;
        }

        public DatabaseType databaseType() {
            return databaseType;
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
            return password;
        }

        public List<String> selectedSchemas() {
            return selectedSchemas;
        }

        public List<String> availableSchemas() {
            return availableSchemas;
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
