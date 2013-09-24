package com.borqs.server.schedule.perhapsname;


import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.sql.*;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PerhapsNameJob implements AppMain {
    private String perhapsUrl;
    private CibindLogic cibind;
    private AccountLogic account;
    private Table userTable;
    private Table userPropertyTable;
    private SqlExecutor sqlExecutor;


    public void setPerhapsUrl(String perhapsUrl) {
        this.perhapsUrl = perhapsUrl;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setUserTable(Table userTable) {
        this.userTable = userTable;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }

    public void setUserPropertyTable(Table userPropertyTable) {
        this.userPropertyTable = userPropertyTable;
    }

    private ShardResult shardUser() {
        return userTable.getShard(0);
    }

    private ShardResult shardProperty(long userId) {
        return userPropertyTable.shard(userId);
    }

    private String getPerhapsName(String url0) {
        if (StringUtils.isEmpty(url0))
            return "";
        URL url;
        try {
            url = new URL(url0);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10 * 1000);

            String s = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            StringBuffer sb = new StringBuffer();
            while ((s = in.readLine()) != null) {
                sb.append(s);
            }
            in.close();
            conn.disconnect();
            return sb.toString().trim();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String formatUrl(long userId) {
        Context ctx = Context.createForViewer(userId);

        BindingInfo[] bindingInfos = cibind.getBindings(ctx, userId);
        if (bindingInfos.length == 0)
            return "";

        StringBuilder urlMiddle = new StringBuilder(perhapsUrl);
        for (BindingInfo b : bindingInfos) {
            if (BindingInfo.EMAIL.equals(b.getType())) {
                urlMiddle.append("byemail/").append(b.getInfo());
            } else if (BindingInfo.MOBILE_TEL.equals(b.getInfo())) {
                urlMiddle.append("bymobile/").append(b.getInfo());
            } else {
                urlMiddle.append("byborqsid/").append(userId);
            }
        }
        return urlMiddle.append(".json?limit=2").toString();
    }

    @Override
    public void run(String[] args) throws Exception {
        Context ctx = Context.createForViewer(10000);

        long longs = setPerhapsName(ctx, 0);
        while(longs != 0){
            longs = setPerhapsName(ctx, longs);

        }
    }

    public long setPerhapsName(final Context ctx, final long userId) {
         long finalUserId = 0;
        final ShardResult userSR = shardUser();
        long [] longs =  sqlExecutor.openConnection(userSR.db, new SingleConnectionHandler<long[]>() {
            @Override
            protected long[] handleConnection(Connection conn) {
                String sql = getUserIds(userSR.table, userId);
                final List<Long> existsIds = new ArrayList<Long>();
                SqlExecutor.executeCustom(ctx, conn, sql, new ResultSetHandler() {
                    @Override
                    public void handle(ResultSet rs) throws SQLException {
                        readIds(rs, existsIds);
                    }
                });
                long[] userIds = CollectionsHelper.toLongArray(existsIds);
                return userIds;
            }
        });

        // save perhapsName
        Users users = account.getUsers(ctx, new String[]{User.COL_PERHAPS_NAME},longs);
        for(User user:users){
            User user0 = new User();
            Context ctx0 = Context.createForViewer(user.getUserId());
            String perhapsName = getPerhapsName(formatUrl(user.getUserId()));
            user0.setUserId(user.getUserId());
            user0.setProperty(User.COL_PERHAPS_NAME, perhapsName);
            account.update(ctx0,user0);
            finalUserId = user0.getUserId();
        }
        if(longs.length<100)
            return 0;
        return finalUserId ;
    }

    private String getUserIds(String table, long userId) {
        return new Sql()
                .select(" user_id ")
                .from(table)
                .where(" destroyed_time=0")
                .and("user_id>:userId", "userId", userId)
                .orderBy("user_id", "asc")
                .limit(100)
                .toString();
    }

    public static List<Long> readIds(ResultSet rs, List<Long> reuse) throws SQLException {
        if (reuse == null)
            reuse = new ArrayList<Long>();
        while (rs.next()) {
            reuse.add(rs.getLong("user_id"));
        }
        return reuse;
    }
}
