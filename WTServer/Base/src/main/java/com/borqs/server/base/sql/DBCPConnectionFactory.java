package com.borqs.server.base.sql;


import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DBCPConnectionFactory extends ConnectionFactory {
    private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

    public DBCPConnectionFactory() {
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {

    }

    @Override
    public Connection getConnection(String db) throws SQLException {
        Validate.notNull(db);

        DataSource ds;
        synchronized (this) {
            ds = dataSources.get(db);
            if (ds == null) {
                ds = createDbcpDataSource(db);
                dataSources.put(db, ds);
            }
        }

        return ds.getConnection();
    }

    private DataSource createDbcpDataSource(String db) {
        String connStr = getConnectionString(db);
        ObjectPool cp = new GenericObjectPool(null);
        DriverManagerConnectionFactory dmcf = new DriverManagerConnectionFactory(connStr, null);
        new PoolableConnectionFactory(dmcf, cp, null, null, false, true);
        return new PoolingDataSource(cp);
    }
}

