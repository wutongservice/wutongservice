package com.borqs.server.platform.sql;


import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.Initializable;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class ConnectionFactory implements Initializable {
    private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

    protected ConnectionFactory() {
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    protected DataSource getDataSource(String db, boolean xa) {
        synchronized (this) {
            DataSource ds = dataSources.get(db);
            if (ds == null) {
                ds = createDataSource(db, xa);
                dataSources.put(db, ds);
            }
            return ds;
        }
    }

    protected DataSource createDataSource(String db, boolean xa) {
        if (xa) {
            AtomikosDataSourceBean adsb = new AtomikosDataSourceBean();
            adsb.setUniqueResourceName(db);
            adsb.setXaDataSourceClassName(MysqlXADataSource.class.getName());
            Properties props = new Properties();
            props.setProperty("URL", db);
            adsb.setXaProperties(props);
            return adsb;
        } else {
            MysqlDataSource ds = new MysqlDataSource();
            ds.setUrl(db);
            return ds;
        }
    }

    public Connection getConnection(String db, boolean xa) {
        try {
            DataSource ds = getDataSource(db, xa);
            return ds.getConnection();
        } catch (SQLException e) {
            throw new ServerException(E.SQL, e);
        }
    }
}
