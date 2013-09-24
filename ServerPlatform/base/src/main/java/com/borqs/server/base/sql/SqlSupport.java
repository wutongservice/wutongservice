package com.borqs.server.base.sql;


public abstract class SqlSupport {
    protected SQLExecutor sqlExecutor;

    protected SqlSupport() {
    }

    public SQLExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public void setSqlExecutor(SQLExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
}
