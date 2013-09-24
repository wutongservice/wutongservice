package com.borqs.server.base.migrate.output;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;

public class SQLInsertIgnoreOutput extends SQLInsertOutput {
    public SQLInsertIgnoreOutput(ConnectionFactory connectionFactory, String db, String table) {
        super(connectionFactory, db, table);
    }

    @Override
    protected String makeSql(Record rec) {
        return SQLBuilder.forInsertIgnore(table, rec);
    }
}
