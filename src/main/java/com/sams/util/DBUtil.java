package com.sams.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiny JDBC connection pool.
 * <ul>
 *   <li>At most {@code db.pool.size} connections are open at the same time.</li>
 *   <li>{@link #getConnection()} returns a proxy - calling close() on it gives the
 *       real connection back to the pool instead of closing it.</li>
 *   <li>Idle connections are validated before being handed out again.</li>
 * </ul>
 */
public final class DBUtil {
    private static final ConcurrentLinkedDeque<Connection> IDLE = new ConcurrentLinkedDeque<>();
    private static Semaphore permits;
    private static String url, user, password;
    private static volatile boolean ready;

    private DBUtil() { }

    public static synchronized void init() {
        if (ready) return;
        String driver = Config.get("db.driver", "");
        url = Config.get("db.url", "");
        user = Config.get("db.user", "");
        password = Config.get("db.password", "");
        permits = new Semaphore(Config.getInt("db.pool.size", 10));
        try {
            if (!driver.isEmpty()) Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + driver, e);
        }
        ready = true;
    }

    public static Connection getConnection() throws SQLException {
        if (!ready) init();
        try {
            if (!permits.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new SQLException("Database connection pool exhausted");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a connection", e);
        }
        Connection real = null;
        try {
            while ((real = IDLE.pollFirst()) != null) {
                if (real.isValid(2)) break;
                closeQuietly(real);
                real = null;
            }
            if (real == null) real = DriverManager.getConnection(url, user, password);
            return wrap(real);
        } catch (SQLException | RuntimeException e) {
            closeQuietly(real);
            permits.release();
            throw e;
        }
    }

    private static Connection wrap(final Connection real) {
        final AtomicBoolean closed = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(
                DBUtil.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("close".equals(name)) {
                        if (closed.compareAndSet(false, true)) release(real);
                        return null;
                    }
                    if ("isClosed".equals(name)) return closed.get();
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void release(Connection real) {
        try {
            if (!real.isClosed()) {
                if (!real.getAutoCommit()) {          // roll back anything left open
                    real.rollback();
                    real.setAutoCommit(true);
                }
                IDLE.offerFirst(real);
            }
        } catch (SQLException e) {
            closeQuietly(real);
        } finally {
            permits.release();
        }
    }

    public static synchronized void shutdown() {
        Connection c;
        while ((c = IDLE.pollFirst()) != null) closeQuietly(c);
        ready = false;
    }

    private static void closeQuietly(Connection c) {
        if (c == null) return;
        try { c.close(); } catch (SQLException ignored) { }
    }
}
