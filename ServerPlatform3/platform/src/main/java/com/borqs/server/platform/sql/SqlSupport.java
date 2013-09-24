package com.borqs.server.platform.sql;


public abstract class SqlSupport {
    protected SqlExecutor sqlExecutor;

    protected SqlSupport() {
    }

    public SqlExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
}
