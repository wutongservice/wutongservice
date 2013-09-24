package com.borqs.server.base.migrate.input;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.migrate.RecordInput;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLInput implements RecordInput {
    private final ConnectionFactory connectionFactory;
    private final String db;
    private final String sql;
    private ResultSet resultSet;

    public SQLInput(ConnectionFactory connectionFactory, String db, String sql) {
        Validate.notNull(connectionFactory);
        Validate.notNull(db);
        Validate.notNull(sql);
        this.connectionFactory = connectionFactory;
        this.db = db;
        this.sql = sql;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public String getDb() {
        return db;
    }

    public String getSql() {
        return sql;
    }

    @Override
    public Record input() {
        try {
            if (resultSet.next()) {
                return SQLExecutor.bindRecord(resultSet,  null);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_FOR_MIGRATE_ERROR, e);
        }
    }

    @Override
    public void init() {
        try {
            Connection conn = connectionFactory.getConnection(db);
            Statement statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_FOR_MIGRATE_ERROR, e);
        }
    }

    @Override
    public void destroy() {
        try {
            Statement stmt = null;
            Connection conn = null;
            if (resultSet != null) {
                stmt = resultSet.getStatement();
                resultSet.close();
                resultSet = null;
            }

            if (stmt != null) {
                conn = stmt.getConnection();
                stmt.close();
            }

            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new ServerException(BaseErrors.PLATFORM_SQL_FOR_MIGRATE_ERROR, e);
        }
    }
}
