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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class ConnectionRegistry {

    private final File directory;
    private final ProtectedSecretStore secretStore;

    public ConnectionRegistry(File directory, ProtectedSecretStore secretStore) {
        this.directory = directory;
        this.secretStore = secretStore;
    }

    public void save(String name, ConnectionConfig config) throws IOException {
        validateName(name);
        validate(config);
        ensureDirectoryExists();

        Properties properties = toProperties(config);
        FileOutputStream output = new FileOutputStream(connectionFile(name));
        try {
            properties.store(output, "Database Script CLI connection profile");
        } finally {
            output.close();
        }
    }

    public boolean exists(String name) {
        validateName(name);
        return connectionFile(name).isFile();
    }

    public void update(String oldName, String newName, ConnectionConfig config) throws IOException {
        validateName(oldName);
        validateName(newName);
        validate(config);
        File oldFile = connectionFile(oldName);
        File newFile = connectionFile(newName);
        if (!oldFile.isFile()) {
            throw new IllegalArgumentException("Connection profile does not exist: " + oldName);
        }
        if (!oldName.equals(newName) && newFile.isFile()) {
            throw new IllegalArgumentException("Connection profile already exists: " + newName);
        }
        save(newName, config);
        if (!oldName.equals(newName) && oldFile.isFile() && !oldFile.delete()) {
            throw new IOException("Could not delete old connection profile: " + oldName);
        }
    }

    public boolean delete(String name) {
        validateName(name);
        File file = connectionFile(name);
        return file.isFile() && file.delete();
    }

    private void ensureDirectoryExists() throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create connection directory: " + directory.getAbsolutePath());
        }
    }

    private Properties toProperties(ConnectionConfig config) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("type", config.databaseType().name());
        properties.setProperty("environment", config.environment().name());
        properties.setProperty("jdbcUrl", config.jdbcUrl());
        properties.setProperty("username", config.username());
        properties.setProperty("password", secretStore.protect(config.password()));
        if (config.databaseType() == DatabaseType.POSTGRESQL && !config.schemas().isEmpty()) {
            properties.setProperty("schemas", ConfigStore.joinSchemas(config.schemas()));
        }
        return properties;
    }

    public ConnectionConfig load(String name) throws IOException {
        validateName(name);
        File file = connectionFile(name);
        if (!file.isFile()) {
            throw new IllegalArgumentException("Connection profile does not exist: " + name);
        }
        Properties properties = loadProperties(file);
        String protectedPassword = required(properties, "password");
        String password = secretStore.isProtectedValue(protectedPassword) ? secretStore.reveal(protectedPassword) : protectedPassword;
        return new ConnectionConfig(
            DatabaseType.fromStoredValue(properties.getProperty("type")),
            ConnectionEnvironment.fromStoredValue(properties.getProperty("environment")),
            required(properties, "jdbcUrl"),
            required(properties, "username"),
            password,
            ConfigStore.parseSchemas(properties.getProperty("schemas"))
        );
    }

    public List<ConnectionSummary> list() throws IOException {
        List<ConnectionSummary> summaries = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return summaries;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().endsWith(".properties")) {
                continue;
            }
            Properties properties = loadProperties(file);
            String name = file.getName().substring(0, file.getName().length() - ".properties".length());
            summaries.add(
                new ConnectionSummary(
                    name,
                    DatabaseType.fromStoredValue(properties.getProperty("type")),
                    ConnectionEnvironment.fromStoredValue(properties.getProperty("environment")),
                    required(properties, "jdbcUrl"),
                    required(properties, "username"),
                    ConfigStore.parseSchemas(properties.getProperty("schemas"))
                )
            );
        }
        Collections.sort(summaries);
        return summaries;
    }

    public void importLegacy(String name, File legacyConfigFile) throws IOException {
        save(name, ConfigStore.load(legacyConfigFile));
    }

    public void validate(ConnectionConfig config) {
        requireValue(config.jdbcUrl(), "jdbcUrl");
        requireValue(config.username(), "username");
        requireValue(config.password(), "password");
        if (!config.databaseType().acceptsJdbcUrl(config.jdbcUrl())) {
            throw new IllegalArgumentException("JDBC URL does not match database type: " + config.databaseType());
        }
    }

    private File connectionFile(String name) {
        return new File(directory, name + ".properties");
    }

    private static Properties loadProperties(File file) throws IOException {
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(file);
        try {
            properties.load(input);
        } finally {
            input.close();
        }
        return properties;
    }

    private static void validateName(String name) {
        requireValue(name, "name");
        if (!name.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid connection profile name: " + name);
        }
    }

    private static String required(Properties properties, String key) {
        return requireValue(properties.getProperty(key), key);
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required connection field is missing: " + field);
        }
        return value;
    }

    public static final class ConnectionSummary implements Comparable<ConnectionSummary> {

        private final String name;
        private final DatabaseType databaseType;
        private final ConnectionEnvironment environment;
        private final String jdbcUrl;
        private final String username;
        private final List<String> schemas;

        public ConnectionSummary(String name, String jdbcUrl, String username) {
            this(name, DatabaseType.ORACLE, ConnectionEnvironment.DEV, jdbcUrl, username, Collections.<String>emptyList());
        }

        public ConnectionSummary(String name, DatabaseType databaseType, String jdbcUrl, String username, List<String> schemas) {
            this(name, databaseType, ConnectionEnvironment.DEV, jdbcUrl, username, schemas);
        }

        public ConnectionSummary(
            String name,
            DatabaseType databaseType,
            ConnectionEnvironment environment,
            String jdbcUrl,
            String username,
            List<String> schemas
        ) {
            this.name = name;
            this.databaseType = databaseType == null ? DatabaseType.ORACLE : databaseType;
            this.environment = environment == null ? ConnectionEnvironment.DEV : environment;
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.schemas = Collections.unmodifiableList(new ArrayList<>(schemas));
        }

        public String name() {
            return name;
        }

        public String jdbcUrl() {
            return jdbcUrl;
        }

        public DatabaseType databaseType() {
            return databaseType;
        }

        public ConnectionEnvironment environment() {
            return environment;
        }

        public String username() {
            return username;
        }

        public List<String> schemas() {
            return schemas;
        }

        @Override
        public int compareTo(ConnectionSummary other) {
            return name.compareTo(other.name);
        }

        @Override
        public String toString() {
            String schemaSummary = schemas.isEmpty() ? "" : ", schemas=" + ConfigStore.joinSchemas(schemas);
            return name + " -> " + jdbcUrl + " (" + username + ", " + databaseType + ", " + environment + schemaSummary + ")";
        }
    }
}
