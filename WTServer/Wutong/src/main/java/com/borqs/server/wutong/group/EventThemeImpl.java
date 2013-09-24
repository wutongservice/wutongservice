package com.borqs.server.wutong.group;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class EventThemeImpl implements EventThemeLogic, Initializable {
    private static final Logger L = Logger.getLogger(EventThemeImpl.class);

    private ConnectionFactory connectionFactory;
    private String db;
    private String eventThemeTable;


    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("eventTheme.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.eventThemeTable = conf.getString("eventTheme.simple.conversationTable", "event_theme");
    }

    @Override
    public void destroy() {
        this.eventThemeTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @TraceCall
    @Override
    public Map<Long, Record> getEventThemes(Context ctx, long... themeIds) {
        String sql = new SQLBuilder.Select().select("id", "creator", "updated_time", "name", "image_url")
                .from(eventThemeTable)
                .where("id IN (${theme_ids})", "theme_ids", StringUtils2.join(themeIds, ",")).toString();
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.toIntRecordMap("id");
    }

    @TraceCall
    @Override
    public Record addEventTheme(Context ctx, long id, long creator, long updatedTime, String name, String imageUrl) {
        Record rec = new Record();
        rec.set("id", id)
                .set("creator", creator)
                .set("updated_time", updatedTime)
                .set("name", ObjectUtils.toString(name))
                .set("image_url", ObjectUtils.toString(imageUrl)) ;
        String sql = SQLBuilder.forInsert(eventThemeTable, rec);
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
        return rec;
    }

    @TraceCall
    @Override
    public RecordSet getEventThemes(Context ctx, int page, int count) {
        if (page <= 0)
            page = 0;
        if (count <= 0 || count >= 100)
            count = 100;
        String sql = new SQLBuilder.Select()
                .select("id", "creator", "updated_time", "name", "image_url")
                .from(eventThemeTable)
                .orderBy("updated_time", "DESC")
                .page(page, count)
                .toString();


        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }
}
