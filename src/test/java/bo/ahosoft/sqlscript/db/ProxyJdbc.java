package bo.ahosoft.sqlscript.db;

import bo.ahosoft.sqlscript.cli.*;
import bo.ahosoft.sqlscript.config.*;
import bo.ahosoft.sqlscript.db.*;
import bo.ahosoft.sqlscript.domain.*;
import bo.ahosoft.sqlscript.sql.*;
import bo.ahosoft.sqlscript.tui.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

final class ProxyJdbc {

    private ProxyJdbc() {}

    interface StatementHandler {
        ResultSet executeQuery(String sql) throws SQLException;

        boolean execute(String sql) throws SQLException;

        ResultSet resultSet() throws SQLException;
    }

    static Connection connection(final StatementHandler statementHandler) {
        return (Connection) Proxy.newProxyInstance(
            ProxyJdbc.class.getClassLoader(),
            new Class<?>[] { Connection.class },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    if ("createStatement".equals(name)) {
                        return statement(statementHandler);
                    }
                    if ("setAutoCommit".equals(name) || "commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        return null;
                    }
                    if ("isClosed".equals(name)) {
                        return false;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
    }

    static ResultSet resultSet(final String columnName, final String... values) {
        return (ResultSet) Proxy.newProxyInstance(
            ProxyJdbc.class.getClassLoader(),
            new Class<?>[] { ResultSet.class },
            new InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String name = method.getName();
                    if ("next".equals(name)) {
                        index++;
                        return index < values.length;
                    }
                    if ("getString".equals(name) || "getObject".equals(name)) {
                        return values[index];
                    }
                    if ("getMetaData".equals(name)) {
                        return metadata(columnName);
                    }
                    if ("close".equals(name)) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
    }

    private static Statement statement(final StatementHandler handler) {
        return (Statement) Proxy.newProxyInstance(
            ProxyJdbc.class.getClassLoader(),
            new Class<?>[] { Statement.class },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("executeQuery".equals(method.getName())) {
                        return handler.executeQuery((String) args[0]);
                    }
                    if ("execute".equals(method.getName())) {
                        return handler.execute((String) args[0]);
                    }
                    if ("getResultSet".equals(method.getName())) {
                        return handler.resultSet();
                    }
                    if ("getUpdateCount".equals(method.getName())) {
                        return -1;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
    }

    private static ResultSetMetaData metadata(final String columnName) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
            ProxyJdbc.class.getClassLoader(),
            new Class<?>[] { ResultSetMetaData.class },
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("getColumnCount".equals(method.getName())) {
                        return 1;
                    }
                    if ("getColumnLabel".equals(method.getName())) {
                        return columnName;
                    }
                    return defaultValue(method.getReturnType());
                }
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        return null;
    }
}
