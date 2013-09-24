package com.borqs.server.base.sql;


import com.atomikos.icatch.jta.UserTransactionManager;
import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Values;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.ObjectHolder;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.sql.*;
import java.util.*;

public class SQLExecutor {
    private static final Logger L = Logger.getLogger(SQLExecutor.class);

    private final ConnectionFactory connectionFactory;
    private final List<String> dbs;


    public SQLExecutor(ConnectionFactory connectionFactory, String dbs) {
        this.connectionFactory = connectionFactory;
        this.dbs = Arrays.asList(StringUtils.split(dbs, ","));
        Validate.isTrue(!dbs.isEmpty());
    }

    private static int executeUpdate0(Statement stmt, String sql, ObjectHolder<Long> genKeyHolder) throws SQLException {
        if (L.isTraceEnabled())
            L.traceSql(null, sql);
        int n = 0;
        try {
            n = stmt.executeUpdate(sql, genKeyHolder != null ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
            if (genKeyHolder != null) {
                ResultSet genKeysRs = stmt.getGeneratedKeys();
                try {
                    if (genKeysRs.next()) {
                        genKeyHolder.value = Values.toInt(genKeysRs.getLong(1));
                    }
                } finally {
                    genKeysRs.close();
                }
            }
        }
        catch (SQLException se)
        {
            L.trace(null, "sql exception: " + se.getMessage());
        }
        return n;
    }

    private static ResultSet executeQuery0(Statement stmt, String sql) throws SQLException {
        if (L.isTraceEnabled())
            L.traceSql(null, sql);
        return stmt.executeQuery(sql);
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public String getDbs() {
        return StringUtils.join(dbs, ",");
    }

    private String getRandomDb() {
        Random rand = new Random();
        int index = rand.nextInt(dbs.size());
        return dbs.get(index);
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
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        }
    }

    public static void executeStatement(ConnectionFactory cf, String db, SQLStatementHandler handler) {
        Validate.notNull(cf);
        Validate.notNull(db);
        Validate.notNull(handler);

        try {
            Connection conn = null;
            try {
                conn = cf.getConnection(db);
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    handler.handle(stmt);
                } finally {
                    if (stmt != null)
                        stmt.close();
                }
            } finally {
                if (conn != null)
                    conn.close();
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        }
    }


    public static void executeStatements(CfDb cfdb1, CfDb cfdb2, SQLStatementsHandler handler) {
        executeStatements(new CfDb[]{cfdb1, cfdb2}, handler);
    }

    public static void executeStatements(CfDb cfdb1, CfDb cfdb2, CfDb cfdb3, SQLStatementsHandler handler) {
        executeStatements(new CfDb[]{cfdb1, cfdb2, cfdb3}, handler);
    }

    public static void executeStatements(CfDb[] cfdbs, SQLStatementsHandler handler) {
        try {
            Validate.notNull(cfdbs);
            Validate.notNull(handler);
            if (cfdbs.length == 0) {
                handler.handle(new Statement[0]);
                return;
            }

            Connection[] conns = new Connection[cfdbs.length];
            try {
                for (int i = 0; i < conns.length; i++)
                    conns[i] = cfdbs[i].getConnection();
                Statement[] stmts = new Statement[conns.length];
                try {
                    for (int i = 0; i < conns.length; i++)
                        stmts[i] = conns[i].createStatement();

                    handler.handle(stmts);
                } finally {
                    for (int i = stmts.length - 1; i >= 0; i--) {
                        if (stmts[i] != null)
                            stmts[i].close();
                    }
                }
            } finally {
                for (int i = conns.length - 1; i >= 0; i--) {
                    if (conns[i] != null)
                        conns[i].close();
                }
            }

        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        }
    }

    public static long executeUpdate(ConnectionFactory cf, String db, List<String> sqls, final ObjectHolder<Long> genKeyHolder) {
        Validate.notNull(sqls);
        if (sqls.isEmpty())
            return 0L;

        final List<String> sqls0 = sqls;
        final ObjectHolder<Long> effected = new ObjectHolder<Long>(0L);
        executeStatement(cf, db, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                for (String sql : sqls0) {
                    int n = executeUpdate0(stmt, sql, genKeyHolder);
                    effected.value += n;
                }
            }
        });
        return effected.value;
    }

    public static long executeUpdate(ConnectionFactory cf, String db, String sql, ObjectHolder<Long> genKeyHolder) {
        return executeUpdate(cf, db, Arrays.asList(sql), genKeyHolder);
    }

    public static long executeUpdate(Statement stmt, String sql, ObjectHolder<Long> genKeyHolder) throws SQLException {
        return executeUpdate0(stmt, sql, genKeyHolder);
    }

    public static long executeUpdate(ConnectionFactory cf, String db, List<String> sqls) {
        return executeUpdate(cf, db, sqls, null);
    }

    public static long executeUpdate(ConnectionFactory cf, String db, String sql) {
        return executeUpdate(cf, db, Arrays.asList(sql));
    }

    public static long executeUpdate(Statement stmt, String sql) throws SQLException {
        return executeUpdate0(stmt, sql, null);
    }

    public static long executeUpdate(Statement stmt, List<String> sqls) throws SQLException {
        long n = 0;
        for (String sql : sqls) {
            n += executeUpdate0(stmt, sql, null);
        }
        return n;
    }

    public static long executeIntScalar(Statement stmt, String sql, long def) throws SQLException {
        Object o = executeScalar(stmt, sql);
        if (o == null)
            return def;

        try {
            return Values.toInt(o);
        } catch (Exception e) {
            return def;
        }
    }

    public static Object executeScalar(Statement stmt, String sql) throws SQLException {
        ResultSet rs = null;
        try {
            rs = executeQuery0(stmt, sql);
            if (rs.next())
                return rs.getObject(1);
            else
                return null;
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    public static long executeIntScalar(ConnectionFactory cf, String db, String sql, long def) throws SQLException {
        Object o = executeScalar(cf, db, sql);
        if (o == null)
            return def;

        try {
            return Values.toInt(o);
        } catch (Exception e) {
            return def;
        }
    }

    public static Object executeScalar(ConnectionFactory cf, String db, String sql) {
        final String sql0 = sql;
        final ObjectHolder<Object> r = new ObjectHolder<Object>(null);
        executeStatement(cf, db, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                r.value = executeScalar(stmt, sql0);
            }
        });
        return r.value;
    }

    public static Record executeRecord(Statement stmt, String sql, Record rec) throws SQLException {
        if (rec == null)
            rec = new Record();

        ResultSet rs = null;
        try {
            rs = executeQuery0(stmt, sql);
            if (rs.next())
                bindRecord(rs, rec);

            return rec;
        } finally {
            if (rs != null)
                rs.close();
        }
    }
    public static Record executeRecord( Connection conn, Object sql, Record rec) {
        Validate.notNull(conn);
        Validate.notNull(sql);

        if (rec == null)
            rec = new Record();

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0( stmt, ObjectUtils.toString(sql, ""));
                if (rs.next()) {
                    return bindRecord(rs, rec);
                } else {
                    return new Record();
                }
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } finally {
            closeQuietly(stmt);
        }
    }
    public static Record executeRecord(ConnectionFactory cf, String db, String sql, Record rec) {
        final String sql0 = sql;
        final Record rec0 = rec != null ? rec : new Record();
        executeStatement(cf, db, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                executeRecord(stmt, sql0, rec0);
            }
        });
        return rec0;
    }

    public static RecordSet executeRecordSet(Statement stmt, String sql, RecordSet recs) throws SQLException {
        if (recs == null)
            recs = new RecordSet();

        ResultSet rs = null;
        try {
            rs = executeQuery0(stmt, sql);
            while (rs.next()) {
                Record rec = bindRecord(rs, null);
                recs.add(rec);
            }
            return recs;
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    public static RecordSet executeRecordSet(ConnectionFactory cf, String db, String sql, RecordSet recs) {
        final String sql0 = sql;
        final RecordSet recs0 = recs != null ? recs : new RecordSet();
        executeStatement(cf, db, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                executeRecordSet(stmt, sql0, recs0);
            }
        });
        return recs0;
    }

    public static void executeRecordHandler(Statement stmt, String sql, RecordHandler handler) throws SQLException {
        ResultSet rs = null;
        try {
            rs = executeQuery0(stmt, sql);
            while (rs.next()) {
                Record rec = bindRecord(rs, null);
                handler.handle(rec);
            }
        } finally {
            if (rs != null)
                rs.close();
        }
    }

    public static void executeRecordHandler(ConnectionFactory cf, String db, String sql, RecordHandler handler) {
        final String sql0 = sql;
        final RecordHandler handler0 = handler;
        executeStatement(cf, db, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                executeRecordHandler(stmt, sql0, handler0);
            }
        });
    }

    public long executeUpdate(List<String> sqls) {
        Validate.notNull(sqls);

        long effected = 0;
        ArrayList<ServerException> errors = new ArrayList<ServerException>();
        for (String db : dbs) {
            try {
                effected = executeUpdate(connectionFactory, db, sqls);
                errors.add(null);
            } catch (ServerException e) {
                errors.add(e);
            }
        }

        if (!errors.isEmpty() && !errors.contains(null))
            throw errors.get(0);

        return effected;
    }

    public long executeUpdate(String sql) {
        return executeUpdate(Arrays.asList(sql));
    }

    public long executeUpdate(String sql, ObjectHolder<Long> genKeyHolder) {
        long effected = 0;
        for (String db : dbs) {
            effected = executeUpdate(connectionFactory, db, sql, genKeyHolder);
        }
        return effected;
    }

    public void executeStatementAll(SQLStatementHandler handler) {
        Validate.notNull(handler);

        ArrayList<ServerException> errors = new ArrayList<ServerException>();
        for (String db : dbs) {
            try {
                executeStatement(connectionFactory, db, handler);
                errors.add(null);
            } catch (ServerException e) {
                errors.add(e);
            }
        }

        if (!errors.isEmpty() && !errors.contains(null))
            throw errors.get(0);
    }

    public void executeStatementRandom(SQLStatementHandler handler) {
        String db = getRandomDb();
        executeStatement(connectionFactory, db, handler);
    }

    public Object executeScalar(String sql) {
        String db = getRandomDb();
        return executeScalar(connectionFactory, db, sql);
    }

    public long executeIntScalar(String sql, long def) {
        Object v = executeScalar(sql);
        return v != null ? Values.toInt(v) : def;
    }

    public Record executeRecord(String sql, Record rec) {
        String db = getRandomDb();
        return executeRecord(connectionFactory, db, sql, rec);
    }

    public RecordSet executeRecordSet(String sql, RecordSet recs) {
        String db = getRandomDb();
        return executeRecordSet(connectionFactory, db, sql, recs);
    }

    public void executeRecordHandler(String sql, RecordHandler handler) {
        String db = getRandomDb();
        executeRecordHandler(connectionFactory, db, sql, handler);
    }


    public static <T> T executeFirst( Connection conn, Object sql, ResultSetReader<T> reader) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
             ResultSet rs = null;
            try {
                rs = executeQuery0(stmt, ObjectUtils.toString(sql, ""));
                if (rs.next()) {
                    return reader.read(rs, null);
                }
                return null;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } finally {
            closeQuietly(stmt);
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
    public static <T> List<T> executeList( Connection conn, Object sql, List<T> reuse, ResultSetReader<T> reader) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                if (reuse == null)
                    reuse = new ArrayList<T>();
                rs = executeQuery0( stmt, ObjectUtils.toString(sql, ""));
                while (rs.next()) {
                    reuse.add(reader.read(rs, null));
                }
                return reuse;
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static void executeCustom( Connection conn, Object sql, ResultSetHandler handler) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try {
                rs = executeQuery0( stmt, ObjectUtils.toString(sql, ""));
                handler.handle(rs);
            } finally {
                closeQuietly(rs);
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } finally {
            closeQuietly(stmt);
        }
    }

    public static String executeString( Connection conn, Object sql, String def) {
        Object o = executeObject( conn, sql);
        return o == null ? def : Values.toString(o);
    }

    public static Object executeObject( Connection conn, Object sql) {
        Record rec = executeRecord(conn, sql, null);
        return rec.isEmpty() ? null : rec.values().iterator().next();
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
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } catch (Throwable t) {
            throw ServerException.wrap(BaseErrors.PLATFORM_SQL_ERROR, t);
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
    public <T> T openConnection(String db, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db}, handler);
    }

    public <T> T openConnections(String db1, String db2, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db1, db2}, handler);
    }

    public <T> T openConnections(String db1, String db2, String db3, ConnectionsHandler<T> handler) {
        return openConnections(new String[]{db1, db2, db3}, handler);
    }

    public static long executeInt( Connection conn, Object sql, long def) {
        Object o = executeObject(conn, sql);
        return o == null ? def : Values.toInt(o);
    }
    public static long executeUpdate(Connection conn, Object sql, ObjectHolder generatedKey) {
        Validate.notNull(conn);
        Validate.notNull(sql);

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            long n = executeUpdate0(stmt, ObjectUtils.toString(sql, ""), generatedKey != null);
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
            throw new ServerException(BaseErrors.PLATFORM_SQL_ERROR, e);
        } finally {
            closeQuietly(stmt);
        }
    }
    private static int executeUpdate0(Statement stmt, String sql, boolean autoGeneratedKeys) throws SQLException {

        return stmt.executeUpdate(sql, autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
    }
}



