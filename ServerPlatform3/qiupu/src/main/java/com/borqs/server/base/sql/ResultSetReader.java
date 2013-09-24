package com.borqs.server.base.sql;


import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetReader<T> {
    T read(ResultSet rs, T reuse) throws SQLException;
}
