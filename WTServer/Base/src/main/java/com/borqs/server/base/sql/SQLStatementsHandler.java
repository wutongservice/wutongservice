package com.borqs.server.base.sql;


import java.sql.Statement;

public interface SQLStatementsHandler {
    void handle(Statement[] stmts) throws java.sql.SQLException;
}
