package com.borqs.server.platform.sql;


import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetReader<T> {
    T read(ResultSet rs, T reuse) throws SQLException;
}
