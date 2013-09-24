package com.borqs.server.platform.video;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;

public class SimpleVideo extends VideoBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String videoTable = "video";

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.videoTable = conf.getString("video.simple.videoTable", "video");
    }

    @Override
    public void destroy() {
        this.videoTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }


    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean saveVideo0(Record video) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, video)}";

        String sql = SQLTemplate.merge(SQL,
                "table", videoTable, "alias", videoSchema.getAllAliases(),
                "video", video);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet getVideo0(String userId, boolean asc, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user_id='" + userId + "' and destroyed_time=0 ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", videoTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected Record getVideoById0(String video_id) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE video_id='" + video_id + "'";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", videoTable},
        });

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return rec;
    }

    @Override
    protected boolean deleteVideo0(String video_ids) {
        String sql = "delete from "+videoTable+" where video_id in (" + video_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
}
