package bo.ahosoft.sqlscript.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bo.ahosoft.sqlscript.config.ConnectionRegistry;
import bo.ahosoft.sqlscript.config.ProtectedSecretStore;
import bo.ahosoft.sqlscript.domain.ConnectionConfig;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConnectionTestServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void succeedsAndClosesOpenedConnectionWithoutSavingDraft() throws Exception {
        CloseTrackingConnection connection = new CloseTrackingConnection();
        ConnectionTestService service = new ConnectionTestService(new StubOpener(connection.connection()), sameThreadExecutor());
        ConnectionRegistry registry = registry();

        ConnectionTestResult result = service.test(draft(), 500L);

        assertTrue(result.successful());
        assertEquals(ConnectionTestResult.Status.SUCCESS, result.status());
        assertTrue(connection.closed);
        assertTrue(registry.list().isEmpty());
    }

    @Test
    public void reportsFailureMessageWithoutSavingDraft() throws Exception {
        ConnectionTestService service = new ConnectionTestService(new StubOpener(new SQLException("login failed")), sameThreadExecutor());
        ConnectionRegistry registry = registry();

        ConnectionTestResult result = service.test(draft(), 500L);

        assertEquals(ConnectionTestResult.Status.FAILURE, result.status());
        assertFalse(result.successful());
        assertTrue(result.message().contains("login failed"));
        assertTrue(registry.list().isEmpty());
    }

    @Test
    public void reportsTimeoutWithoutSavingDraft() throws Exception {
        BlockingExecutor executor = new BlockingExecutor();
        ConnectionTestService service = new ConnectionTestService(new StubOpener(new CloseTrackingConnection().connection()), executor);
        ConnectionRegistry registry = registry();

        ConnectionTestResult result = service.test(draft(), 25L);

        assertEquals(ConnectionTestResult.Status.TIMEOUT, result.status());
        assertTrue(result.message().contains("25ms"));
        assertTrue(executor.cancelled);
        assertTrue(registry.list().isEmpty());
    }

    private ConnectionConfig draft() {
        return new ConnectionConfig("jdbc:oracle:thin:@localhost:1521/QA", "qa", "secret");
    }

    private ConnectionRegistry registry() throws Exception {
        File baseDirectory = temporaryFolder.newFolder("connections" + System.nanoTime());
        return new ConnectionRegistry(baseDirectory, new ProtectedSecretStore(new File(baseDirectory, "secrets")));
    }

    private static ConnectionTestService.TaskExecutor sameThreadExecutor() {
        return new ConnectionTestService.TaskExecutor() {
            @Override
            public ConnectionTestService.PendingTask submit(ConnectionTestService.ConnectionTask task) throws Exception {
                try {
                    final Connection connection = task.open();
                    return new ConnectionTestService.PendingTask() {
                        @Override
                        public Connection get(long timeoutMillis) {
                            return connection;
                        }

                        @Override
                        public void cancel() {}
                    };
                } catch (final Exception ex) {
                    return new ConnectionTestService.PendingTask() {
                        @Override
                        public Connection get(long timeoutMillis) throws Exception {
                            throw ex;
                        }

                        @Override
                        public void cancel() {}
                    };
                }
            }
        };
    }

    private static final class BlockingExecutor implements ConnectionTestService.TaskExecutor {

        private boolean cancelled;

        @Override
        public ConnectionTestService.PendingTask submit(ConnectionTestService.ConnectionTask task) {
            return new ConnectionTestService.PendingTask() {
                @Override
                public Connection get(long timeoutMillis) throws Exception {
                    throw ConnectionTestService.timeoutException(timeoutMillis);
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            };
        }
    }

    private static final class StubOpener implements ConnectionTestService.ConnectionOpener {

        private final Connection connection;
        private final SQLException failure;

        private StubOpener(Connection connection) {
            this.connection = connection;
            this.failure = null;
        }

        private StubOpener(SQLException failure) {
            this.connection = null;
            this.failure = failure;
        }

        @Override
        public Connection open(ConnectionConfig config) throws SQLException {
            if (failure != null) {
                throw failure;
            }
            return connection;
        }
    }

    private static final class CloseTrackingConnection {

        private boolean closed;

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                ConnectionTestServiceTest.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("close".equals(method.getName())) {
                            closed = true;
                            return null;
                        }
                        if (method.getReturnType() == Boolean.TYPE) {
                            return false;
                        }
                        if (method.getReturnType() == Integer.TYPE) {
                            return 0;
                        }
                        return null;
                    }
                }
            );
        }
    }
}
