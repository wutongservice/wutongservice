package com.borqs.server.platform.sql;


import java.sql.Connection;

public interface ConnectionsHandler<T> {
    T handle(Connection[] conns);
}
