package com.borqs.server.platform.audio;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.platform.audio.AudioBase;

public class SimpleAudio extends AudioBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String audioTable = "audio";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.audioTable = conf.getString("audio.simple.audioTable", "audio");
    }

    @Override
    public void destroy() {
        this.audioTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveAudio0(Record audio) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, audio)}";

        String sql = SQLTemplate.merge(SQL,
                "table", audioTable, "alias", audioSchema.getAllAliases(),
                "audio", audio);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getAudio0(String userId, boolean asc, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user_id='" + userId + "' and destroyed_time=0 ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", audioTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected Record getAudioById0(String audio_id) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE audio_id='" + audio_id + "'";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", audioTable},
        });

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected boolean deleteAudio0(String audio_ids) {
        String sql = "delete from "+audioTable+" where audio_id in (" + audio_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
