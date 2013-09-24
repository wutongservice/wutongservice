package com.borqs.server.base.sql;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleConnectionFactory extends ConnectionFactory {
    public SimpleConnectionFactory() {
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public Connection getConnection(String db) throws SQLException {
        String url = getConnectionString(db);
        return DriverManager.getConnection(url);
    }
}
