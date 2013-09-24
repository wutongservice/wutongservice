package com.borqs.server.base.sql;


import java.sql.Connection;

public interface ConnectionsHandler<T> {
    T handle(Connection[] conns);
}
