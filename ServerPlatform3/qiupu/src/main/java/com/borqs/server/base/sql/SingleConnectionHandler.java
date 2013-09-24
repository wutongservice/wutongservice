package com.borqs.server.base.sql;


import java.sql.Connection;

public abstract class SingleConnectionHandler<T> implements ConnectionsHandler<T> {

    protected SingleConnectionHandler() {
    }

    @Override
    public final T handle(Connection[] conns) {
        Connection conn = conns[0];
        return handleConnection(conn);
    }

    protected abstract T handleConnection(Connection conn);
}
