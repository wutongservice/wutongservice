package com.borqs.server.base.sql;


import java.sql.Statement;

public interface SQLStatementHandler {
    void handle(Statement stmt) throws java.sql.SQLException;
}
