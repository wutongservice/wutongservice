package com.borqs.server.platform.sql;


import org.apache.commons.dbcp.DataSourceConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DBCPConnectionFactory extends ConnectionFactory {
    private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

    public DBCPConnectionFactory() {
    }

    @Override
    protected DataSource createDataSource(String db, boolean xa) {
        DataSource ds = super.createDataSource(db, xa);

        ObjectPool cp = new GenericObjectPool(null);
        DataSourceConnectionFactory dmcf = new DataSourceConnectionFactory(ds);
        new PoolableConnectionFactory(dmcf, cp, null, null, false, true);
        return new PoolingDataSource(cp);
    }
}

