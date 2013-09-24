package com.borqs.server.wutong.link;

import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.like.LikeLogic;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class LinkCacheImpl implements LinkCacheLogic, Initializable {
    private static final Logger L = Logger.getLogger(LinkCacheImpl.class);
    public final Schema linkCacheSchema = Schema.loadClassPath(LinkCacheImpl.class, "link_cache.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String linkCacheTable;
    private Configuration conf;

    public LinkCacheImpl(){}

    @Override
    public void init() {
        conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf
                .getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.linkCacheTable = conf.getString("link.simple.linkCacheTable", "link_cache");
    }

    public void destroy() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public boolean saveLinkCache(Context ctx, String host,String img_name) {
        final String METHOD = "saveLinkCache";
        L.traceStartCall(ctx, METHOD, host, img_name);
        long n = 0;
        if (!hasImgInHost(ctx, host, img_name)) {
            final String sql = "INSERT INTO " + linkCacheTable + " (host,img_name) values ('" + host + "','" + img_name + "')";
            SQLExecutor se = getSqlExecutor();
            n = se.executeUpdate(sql);
        }

        L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    public boolean hasImgInHost(Context ctx, String host, String img_name) {
        final String METHOD = "hasImgInHost";
        L.traceStartCall(ctx, METHOD, host, img_name);
        String sql = "select * from " + linkCacheTable + " where host='" + host + "' and img_name='" + img_name + "'";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        L.traceEndCall(ctx, METHOD);
        return recs.size() > 0 ? true : false;
    }

}
