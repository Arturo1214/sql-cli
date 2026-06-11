package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.domain.ConnectionConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ConnectionTestService {

    private static final long DEFAULT_TIMEOUT_MILLIS = 5000L;

    private final ConnectionOpener opener;
    private final TaskExecutor executor;

    public ConnectionTestService() {
        this(new JdbcConnectionOpener(), new FutureTaskExecutor());
    }

    public ConnectionTestService(ConnectionOpener opener, TaskExecutor executor) {
        this.opener = opener == null ? new JdbcConnectionOpener() : opener;
        this.executor = executor == null ? new FutureTaskExecutor() : executor;
    }

    public ConnectionTestResult test(ConnectionConfig draft) {
        return test(draft, DEFAULT_TIMEOUT_MILLIS);
    }

    public ConnectionTestResult test(final ConnectionConfig draft, long timeoutMillis) {
        PendingTask pendingTask = null;
        try {
            pendingTask = executor.submit(
                new ConnectionTask() {
                    @Override
                    public Connection open() throws Exception {
                        return opener.open(draft);
                    }
                }
            );
            Connection connection = pendingTask.get(timeoutMillis);
            closeQuietly(connection);
            return ConnectionTestResult.success();
        } catch (TimeoutException ex) {
            if (pendingTask != null) {
                pendingTask.cancel();
            }
            return ConnectionTestResult.timeout(timeoutMillis);
        } catch (Exception ex) {
            return ConnectionTestResult.failure(rootMessage(ex));
        }
    }

    public static TimeoutException timeoutException(long timeoutMillis) {
        return new TimeoutException("Timed out after " + timeoutMillis + "ms");
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // A failed close should not turn a successful open into a failed test.
        }
    }

    private static String rootMessage(Exception ex) {
        Throwable current = ex;
        if (ex instanceof ExecutionException && ex.getCause() != null) {
            current = ex.getCause();
        }
        return current.getMessage();
    }

    public interface ConnectionOpener {
        Connection open(ConnectionConfig config) throws SQLException;
    }

    public interface ConnectionTask {
        Connection open() throws Exception;
    }

    public interface PendingTask {
        Connection get(long timeoutMillis) throws Exception;

        void cancel();
    }

    public interface TaskExecutor {
        PendingTask submit(ConnectionTask task) throws Exception;
    }

    private static final class JdbcConnectionOpener implements ConnectionOpener {

        private final JdbcConnectionFactory factory = new JdbcConnectionFactory();

        @Override
        public Connection open(ConnectionConfig config) throws SQLException {
            return factory.open(config);
        }
    }

    private static final class FutureTaskExecutor implements TaskExecutor {

        @Override
        public PendingTask submit(final ConnectionTask task) {
            final ExecutorService service = Executors.newSingleThreadExecutor();
            final Future<Connection> future = service.submit(
                new Callable<Connection>() {
                    @Override
                    public Connection call() throws Exception {
                        return task.open();
                    }
                }
            );
            return new PendingTask() {
                @Override
                public Connection get(long timeoutMillis) throws Exception {
                    try {
                        return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                    } finally {
                        service.shutdownNow();
                    }
                }

                @Override
                public void cancel() {
                    future.cancel(true);
                    service.shutdownNow();
                }
            };
        }
    }
}
