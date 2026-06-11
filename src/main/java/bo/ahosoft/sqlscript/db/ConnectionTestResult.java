package bo.ahosoft.sqlscript.db;

public final class ConnectionTestResult {

    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT,
    }

    private final Status status;
    private final String message;

    private ConnectionTestResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static ConnectionTestResult success() {
        return new ConnectionTestResult(Status.SUCCESS, "Connection test succeeded");
    }

    public static ConnectionTestResult failure(String message) {
        return new ConnectionTestResult(Status.FAILURE, cleanMessage(message, "Connection test failed"));
    }

    public static ConnectionTestResult timeout(long timeoutMillis) {
        return new ConnectionTestResult(Status.TIMEOUT, "Connection test timed out after " + timeoutMillis + "ms");
    }

    public Status status() {
        return status;
    }

    public String message() {
        return message;
    }

    public boolean successful() {
        return status == Status.SUCCESS;
    }

    private static String cleanMessage(String message, String fallback) {
        return message == null || message.trim().isEmpty() ? fallback : message;
    }
}
