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
import java.util.List;
import java.util.Properties;

public final class ConfigStore {

    private static final String DEFAULT_FILE_NAME = ".oracle-script-cli.properties";

    private ConfigStore() {}

    public static File defaultConfigFile() {
        return new File(System.getProperty("user.home"), DEFAULT_FILE_NAME);
    }

    public static File profileConfigFile(String profile) {
        if (profile == null || profile.trim().isEmpty() || "default".equals(profile)) {
            return defaultConfigFile();
        }
        if (!profile.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Perfil inválido: " + profile);
        }
        return new File(System.getProperty("user.home"), ".oracle-script-cli-" + profile + ".properties");
    }

    public static List<String> profiles() {
        List<String> profiles = new ArrayList<>();
        File home = new File(System.getProperty("user.home"));
        File[] files = home.listFiles();
        if (files == null) {
            return profiles;
        }
        for (File file : files) {
            String name = file.getName();
            if (DEFAULT_FILE_NAME.equals(name)) {
                profiles.add("default -> " + file.getAbsolutePath());
            } else if (name.startsWith(".oracle-script-cli-") && name.endsWith(".properties")) {
                String profile = name.substring(".oracle-script-cli-".length(), name.length() - ".properties".length());
                profiles.add(profile + " -> " + file.getAbsolutePath());
            }
        }
        return profiles;
    }

    public static ConnectionConfig load(File file) throws IOException {
        if (!file.isFile()) {
            throw new IllegalArgumentException("No existe el archivo de configuración: " + file.getAbsolutePath());
        }

        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(file);
        try {
            properties.load(input);
        } finally {
            input.close();
        }

        return new ConnectionConfig(
            DatabaseType.fromStoredValue(properties.getProperty("type")),
            required(properties, "jdbcUrl"),
            required(properties, "username"),
            required(properties, "password"),
            parseSchemas(properties.getProperty("schemas"))
        );
    }

    public static void save(File file, ConnectionConfig config) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("No se pudo crear el directorio: " + parent.getAbsolutePath());
        }

        Properties properties = new Properties();
        properties.setProperty("type", config.databaseType().name());
        properties.setProperty("jdbcUrl", config.jdbcUrl());
        properties.setProperty("username", config.username());
        properties.setProperty("password", config.password());
        if (config.databaseType() == DatabaseType.POSTGRESQL && !config.schemas().isEmpty()) {
            properties.setProperty("schemas", joinSchemas(config.schemas()));
        }

        FileOutputStream output = new FileOutputStream(file);
        try {
            properties.store(output, "Database Script CLI configuration");
        } finally {
            output.close();
        }
    }

    public static void importLegacyProfile(String name, File legacyConfigFile, ConnectionRegistry registry) throws IOException {
        registry.importLegacy(name, legacyConfigFile);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta la propiedad obligatoria: " + key);
        }
        return value;
    }

    public static List<String> parseSchemas(String value) {
        List<String> schemas = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return schemas;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                schemas.add(part.trim());
            }
        }
        return schemas;
    }

    public static String joinSchemas(List<String> schemas) {
        StringBuilder joined = new StringBuilder();
        for (String schema : schemas) {
            if (schema == null || schema.trim().isEmpty()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(schema.trim());
        }
        return joined.toString();
    }
}
