package bo.ahosoft.sqlscript.domain;

public enum ConnectionEnvironment {
    DEV,
    QA,
    STAGING,
    PROD;

    public static String allowedValues() {
        return "DEV, QA, STAGING, PROD";
    }

    public static ConnectionEnvironment fromStoredValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEV;
        }
        ConnectionEnvironment environment = parse(value);
        return environment == null ? DEV : environment;
    }

    public static ConnectionEnvironment fromInputValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid environment: " + value + ". Allowed values: " + allowedValues());
        }
        ConnectionEnvironment environment = parse(value);
        if (environment != null) {
            return environment;
        }
        throw new IllegalArgumentException("Invalid environment: " + value + ". Allowed values: " + allowedValues());
    }

    private static ConnectionEnvironment parse(String value) {
        String normalized = value.trim().replace('-', '_').toUpperCase();
        for (ConnectionEnvironment environment : values()) {
            if (environment.name().equals(normalized)) {
                return environment;
            }
        }
        return null;
    }

    public boolean isProduction() {
        return this == PROD;
    }
}
