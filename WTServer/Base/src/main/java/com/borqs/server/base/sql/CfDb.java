package com.borqs.server.base.sql;


import java.sql.Connection;
import java.sql.SQLException;

public class CfDb {
    public final ConnectionFactory connectionFactory;
    public final String db;

    public CfDb(ConnectionFactory connectionFactory, String db) {
        this.connectionFactory = connectionFactory;
        this.db = db;
    }

    public Connection getConnection() throws SQLException {
        return connectionFactory.getConnection(db);
    }

    public static CfDb[] of(ConnectionFactory cf1, String db1) {
        return new CfDb[]{new CfDb(cf1, db1)};
    }

    public static CfDb[] of(ConnectionFactory cf1, String db1, ConnectionFactory cf2, String db2) {
        return new CfDb[]{new CfDb(cf1, db1), new CfDb(cf2, db2)};
    }

    public static CfDb[] of(ConnectionFactory cf1, String db1, ConnectionFactory cf2, String db2, ConnectionFactory cf3, String db3) {
        return new CfDb[]{new CfDb(cf1, db1), new CfDb(cf2, db2), new CfDb(cf3, db3)};
    }

    public static CfDb[] of(ConnectionFactory cf1, String db1, ConnectionFactory cf2, String db2, ConnectionFactory cf3, String db3, ConnectionFactory cf4, String db4) {
        return new CfDb[]{new CfDb(cf1, db1), new CfDb(cf2, db2), new CfDb(cf3, db3), new CfDb(cf4, db4)};
    }
}
