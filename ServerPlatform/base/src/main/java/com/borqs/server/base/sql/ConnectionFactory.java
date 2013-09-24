package com.borqs.server.base.sql;


import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.borqs.server.ErrorCode;
import com.borqs.server.ServerException;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class ConnectionFactory {
    public static final String MYSQL = "mysql";
    private final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();
    public abstract void open();
    public abstract void close();
    public abstract Connection getConnection(String db) throws SQLException;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Load mysql jdbc driver error");
        }
    }

    public static ConnectionFactory close(ConnectionFactory cf) {
        if (cf != null)
            cf.close();
        return null;
    }

    public static  String getConnectionString(String db) {
        Validate.notNull(db);
        String[] ss = StringUtils.split(db, "/", 5);
        Validate.isTrue(ss.length >= 4);

        String dbType = ss[0];
        String addr = ss[1];
        String dbName = ss[2];
        String user = ss[3];
        String pwd = ss.length > 4 ? ss[4] : null;

        if (MYSQL.equalsIgnoreCase(dbType)) {
            String url = String.format("jdbc:mysql://%s/%s?user=%s", addr, dbName, user);
            if (pwd != null)
                url = url + String.format("&password=%s", pwd);
            
            url += "&useUnicode=true&characterEncoding=utf8&autoReconnect=true";
            return url;
        } else {
            Validate.isTrue(false);
            return null;
        }
    }

    private static SimpleConnectionFactory SIMPLE = new SimpleConnectionFactory();
    private static DBCPConnectionFactory DBCP = new DBCPConnectionFactory();
    public static ConnectionFactory getConnectionFactory(String name) {
        if (StringUtils.endsWithIgnoreCase(name, "simple"))
            return SIMPLE;
        else if (StringUtils.equalsIgnoreCase(name, "dbcp"))
            return DBCP;
        else
            throw new IllegalArgumentException("connection name error " + name);
    }


    public Connection getConnection(String db, boolean xa) {
        try {
            DataSource ds = getDataSource(db, xa);
            return ds.getConnection();
        } catch (SQLException e) {
            throw new ServerException(ErrorCode.SQL, e);
        }
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
}
