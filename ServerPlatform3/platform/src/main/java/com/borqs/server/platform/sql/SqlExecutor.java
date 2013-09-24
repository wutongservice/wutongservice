package com.borqs.server.platform.sql;


import com.atomikos.icatch.jta.UserTransactionManager;
import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.data.Values;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.ObjectHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SqlExecutor {
    private static final Logger L = Logger.get(SqlExecutor.class);

    private ConnectionFactory connectionFactory;


    public SqlExecutor() {
        this(null);
    }

    public SqlExecutor(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public <T> T openConnection(String db, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db}, handler);
    }

    public <T> T openConnections(String db1, String db2, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db1, db2}, handler);
    }

    public <T> T openConnections(String db1, String db2, String db3, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db1, db2, db3}, handler);
    }

    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public <T> T openConnections(String[] dbs, ConnectionsHandler<T> handler) {
        Validate.notEmpty(dbs);
        Validate.notNull(handler);
        try {
            T r = null;

            Connection[] conns = new Connection[dbs.length];
            if (isSameDb(dbs)) {
                String db = getFirstDb(dbs);
                if (db != null) {
                    Connection conn = connectionFactory.getConnection(db, false);
                    conn.setAutoCommit(false);
                    for (int i = 0; i < conns.length; i++)
                        conns[i] = dbs[i] != null ? conn : null;

                    try {
                        r = handler.handle(conns);
                        conn.commit();
                    } catch (Throwable t) {
                        conn.rollback();
                        throw t;
                    } finally {
                        closeQuietly(conn);
                    }
                } else {
                    for (int i = 0; i < conns.length; i++)
                        conns[i] = null;

                    handler.handle(conns);
                }
            } else {
                HashMap<String, Connection> connMap = new HashMap<String, Connection>();
                for (int i = 0; i < conns.length; i++) {
                    String db = dbs[i];
                    if (db == null) {
                        conns[i] = null;
                    } else {
                        if (connMap.containsKey(db)) {
                            conns[i] = connMap.get(db);
                        } else {
                            Connection conn = connectionFactory.getConnection(db, true);
                            conn.setAutoCommit(false);
                            connMap.put(db, conn);
                            conns[i] = conn;
                        }
                    }
                }

                UserTransactionManager ut = new UserTransactionManager();
                ut.init();
                try {
                    ut.begin();
                    r = handler.handle(conns);
                    ut.commit();
                } catch (Throwable t) {
                    ut.rollback();
                    throw t;
                } finally {
                    for (Connection conn : connMap.values())
                        closeQuietly(conn);

                    ut.close();
                }
            }
            return r;
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } catch (Throwable t) {
            throw ServerException.wrap(E.SQL, t);
        }
    }

    private static boolean isSameDb(String[] dbs) {
        String db = null;
        for (String dbi : dbs) {
            if (dbi != null) {
                if (db == null) {
                    db = dbi;
                } else {
                    if (!StringUtils.equals(dbi, db))
                        return false;
                }
            }
        }
        return true;
    }

    private static String getFirstDb(String[] dbs) {
        for (String db : dbs) {
            if (db != null)
                return db;
        }
        return null;
    }

    private static int executeUpdate0(Context ctx, Statement stmt, String sql, boolean autoGeneratedKeys) throws SQLException {
        if (L.isDebugEnabled())
            L.debug(ctx, sql);
        return stmt.executeUpdate(sql, autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
    }

    private static ResultSet executeQuery0(Context ctx, Statement stmt, String sql) throws SQLException {
        if (L.isDebugEnabled())
            L.debug(ctx, sql);
        return stmt.executeQuery(sql);
    }

    public static Record bindRecord(ResultSet rs, Record rec) {
        try {
            Record rec0 = rec != null ? rec : new Record();
            ResultSetMetaData meta = rs.getMetaData();
            ArrayList<String> cols = new ArrayList<String>();
            for (int i = 0; i < meta.getColumnCount(); i++)
                cols.add(meta.getColumnLabel(i + 1));

            for (String col : cols) {
                Object o = rs.getObject(col);
                rec0.put(col, o);
            }
            return rec0;
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        }
    }

    public static long executeUpdate(Context ctx, Connection conn, List<?> sqls) {
        Validate.notNull(conn);
        if (CollectionUtils.isEmpty(sqls))
            return 0;

        Statement stmt = null;
        try {
            long n = 0;
            stmt = conn.createStatement();
            for (Object sql : sqls) {
                long nn = executeUpdate0(ctx, stmt, ObjectUtils.toString(sql), false);
                n += nn;
                if (sql instanceof Sql.Entry)
                    ((Sql.Entry) sql).count = nn;
            }

            return n;
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static long executeUpdate(Context ctx, Connection conn, Object sql) {
        return executeUpdate(ctx, conn, sql, null);
    }

    public static long executeUpdate(Context ctx, Connection conn, Object sql, ObjectHolder generatedKey) {
        Validate.notNull(conn);
        Validate.notNull(sql);

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            long n = executeUpdate0(ctx, stmt, ObjectUtils.toString(sql), generatedKey != null);
            if (n > 0 && generatedKey != null) {
                ResultSet rs = stmt.getGeneratedKeys();
                try {
                    rs.next();
                    generatedKey.value = rs.getObject(1);
                } finally {
                    rs.close();
                }
            }
            return n;
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static RecordSet executeRecords(Context ctx, Connection conn, Object sql, RecordSet recs) {
        Validate.notNull(conn);
        Validate.notNull(sql);

        if (recs == null)
            recs = new RecordSet();

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                while (rs.next()) {
                    Record rec = bindRecord(rs, null);
                    recs.add(rec);
                }
                return recs;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static Record executeRecord(Context ctx, Connection conn, Object sql, Record rec) {
        Validate.notNull(conn);
        Validate.notNull(sql);

        if (rec == null)
            rec = new Record();

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                if (rs.next()) {
                    return bindRecord(rs, rec);
                } else {
                    return new Record();
                }
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static long executeRecordCustom(Context ctx, Connection conn, Object sql, RecordHandler handler) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                long n = 0;
                while (rs.next()) {
                    Record rec = bindRecord(rs, null);
                    handler.handle(rec);
                    n++;
                }
                return n;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }


    public static Object executeObject(Context ctx, Connection conn, Object sql) {
        Record rec = executeRecord(ctx, conn, sql, null);
        return rec.isEmpty() ? null : rec.values().iterator().next();
    }

    public static long executeInt(Context ctx, Connection conn, Object sql, long def) {
        Object o = executeObject(ctx, conn, sql);
        return o == null ? def : Values.toInt(o);
    }

    public static String executeString(Context ctx, Connection conn, Object sql, String def) {
        Object o = executeObject(ctx, conn, sql);
        return o == null ? def : Values.toString(o);
    }

    public static <T> List<T> executeList(Context ctx, Connection conn, Object sql, List<T> reuse, ResultSetReader<T> reader) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                if (reuse == null)
                    reuse = new ArrayList<T>();
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                while (rs.next()) {
                    reuse.add(reader.read(rs, null));
                }
                return reuse;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static <T> T executeFirst(Context ctx, Connection conn, Object sql, ResultSetReader<T> reader) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                if (rs.next()) {
                    return reader.read(rs, null);
                }
                return null;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static void executeCustom(Context ctx, Connection conn, Object sql, ResultSetHandler handler) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0(ctx, stmt, ObjectUtils.toString(sql));
                handler.handle(rs);
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static void executeSource(ConnectionFactory connFactory, String db, final String sql) {
        Connection conn = null;
        try {
            conn = connFactory.getConnection(db, false);
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                stmt.execute(sql);
            } catch (SQLException e) {
                throw new ServerException(E.SQL, e);
            } finally {
                closeQuietly(stmt);
            }
        } finally {
            closeQuietly(conn);
        }

    }

    public static void executeSource(String db, String sql) {
        if (StringUtils.isBlank(sql))
            return;

        ConnectionFactory cf = new SimpleConnectionFactory();
        try {
            cf.init();
            executeSource(cf, db, sql);
        } finally {
            cf.destroy();
        }
    }
}
