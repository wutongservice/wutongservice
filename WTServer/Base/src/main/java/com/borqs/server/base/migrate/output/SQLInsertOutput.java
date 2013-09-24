package com.borqs.server.base.migrate.output;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;

public class SQLInsertOutput extends SQLOutput {

    protected final String table;

    public SQLInsertOutput(ConnectionFactory connectionFactory, String db, String table) {
        super(connectionFactory, db);
        this.table = table;
    }


    @Override
    protected String makeSql(Record rec) {
        return SQLBuilder.forInsert(table, rec);
    }
}
