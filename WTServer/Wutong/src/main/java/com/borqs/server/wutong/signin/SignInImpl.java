package com.borqs.server.wutong.signin;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ErrorUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.wutong.GlobalLogics;
import org.apache.commons.lang.ObjectUtils;

public class SignInImpl implements SignInLogic, Initializable {
    private static final Logger L = Logger.getLogger(SignInImpl.class);


    protected final Schema signinSchema = Schema.loadClassPath(SignInImpl.class, "signin.schema");

    private ConnectionFactory connectionFactory;
    private String db;
    private String signinTable = "sign_in";

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
        signinSchema.loadAliases(conf.getString("schema.signin.alias", null));
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.signinTable = conf.getString("signin.simple.settingTable", "sign_in");
    }

    @Override
    public void destroy() {
        this.signinTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private static String toStr(Object o) {
        return ObjectUtils.toString(o);
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    protected boolean saveSignIn0(Record sign_in) {
        final String SQL = "INSERT INTO ${table} ${values_join(alias, sign_in)}";

        String sql = SQLTemplate.merge(SQL,
                "table", signinTable, "alias", signinSchema.getAllAliases(),
                "sign_in", sign_in);
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean saveSignIn(Context ctx, Record sinIn) {
        try {
            Schemas.checkRecordIncludeColumns(sinIn, "user_id", "longitude", "latitude");
            sinIn.put("sign_id", Long.toString(RandomUtils.generateId()));
            sinIn.put("created_time", DateUtils.nowMillis());
            return saveSignIn0(sinIn);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }


    protected RecordSet getSignIn0(String userId, boolean asc, int page, int count) {
        String SQL = "SELECT * FROM ${table}"
                + " WHERE user_id='" + userId + "' and type=0 ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getSignIn(Context ctx, String userId, boolean asc, int page, int count) {
        try {
            return getSignIn0(toStr(userId), asc, page, count);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean deleteSignIn0(String sign_ids) {
        String sql = "delete from sign_in where  sign_id in (" + sign_ids + ")";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean deleteSignIn(Context ctx, String sign_ids) {
        try {
            return deleteSignIn0(toStr(sign_ids));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getUserShaking0(String userId, long dateDiff, boolean asc, int page, int count) {
        long dateDiff0 = DateUtils.nowMillis() - dateDiff;
        String SQL = "SELECT distinct(user_id) FROM ${table}"
                + " WHERE user_id<>'" + userId + "' and type=1 and created_time>=" + dateDiff0 + " ORDER BY created_time ${asc} ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
                {"asc", asc ? "ASC" : "DESC"},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        RecordSet out_recs = new RecordSet();
        for (Record rec : recs) {
            String sql0 = "select user_id,longitude,latitude,geo from " + signinTable + " where" +
                    " user_id='" + rec.getString("user_id") + "' and type=1 and created_time>=" + dateDiff0 + "" +
                    "  ORDER BY created_time desc limit 1";
            Record rec0 = se.executeRecord(sql0, null);
            out_recs.add(rec0);
        }
        return out_recs;
    }

    @TraceCall
    @Override
    public RecordSet getUserShaking(Context ctx, String userId, long dateDiff, boolean asc, int page, int count) {
        try {
            return getUserShaking0(toStr(userId), dateDiff, asc, page, count);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getUserNearBy0(String userId, int page, int count) {
        String SQL = "SELECT distinct(user_id) FROM ${table}"
                + " WHERE user_id<>'" + userId + "' ORDER BY created_time desc ${limit}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", signinTable},
                {"limit", SQLUtils.pageToLimit(page, count)},
        });

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);

        RecordSet out_recs = new RecordSet();
        for (Record rec : recs) {
            String sql0 = "select user_id,longitude,latitude,geo from " + signinTable + " where" +
                    " user_id='" + rec.getString("user_id") + "'" +
                    "  ORDER BY created_time desc limit 1";
            Record rec0 = se.executeRecord(sql0, null);
            out_recs.add(rec0);
        }
        return out_recs;
    }

    @TraceCall
    @Override
    public RecordSet getUserNearBy(Context ctx, String userId, int page, int count) {
        try {
            return getUserNearBy0(toStr(userId), page, count);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    // platform
    @TraceCall
    @Override
    public boolean signInP(Context ctx, String userId, String longitude, String latitude, String altitude, String speed, String geo, int type) {
        Record r = new Record();
        r.put("user_id", userId);
        r.put("longitude", longitude);
        r.put("latitude", latitude);
        r.put("altitude", altitude);
        r.put("speed", speed);
        r.put("geo", geo);
        r.put("type", type);
        return saveSignIn(ctx, r);
    }

    public static final String USER_COLUMNS_SHAK =
            "user_id, display_name, remark,perhaps_name,image_url, status, gender, in_circles, his_friend, bidi";


    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    @Override
    @TraceCall
    public double GetDistanceP(Context ctx, double lng1, double lat1, double lng2, double lat2) {
        double EARTH_RADIUS = 6378137;
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    @Override
    @TraceCall
    public RecordSet getUserShakingP(Context ctx, String userId, String longitude0, String latitude0, int page, int count) {

        long dateDiff = 10 * 60 * 1000L;

        double longitude_me = Double.parseDouble(longitude0);
        double latitude_me = Double.parseDouble(latitude0);

        RecordSet recs = getUserShaking(ctx, userId, dateDiff, false, 0, 1000);
        // 找同时在摇的1000个人出来
        if (recs.size() > 0) {
            for (Record rec : recs) {
                //每个人的距离算出来
                double longitude = Double.parseDouble(rec.getString("longitude"));
                double latitude = Double.parseDouble(rec.getString("latitude"));
                double distance = GetDistanceP(ctx, longitude_me, latitude_me, longitude, latitude);
                rec.put("distance", distance);
            }
            recs.sort("distance", true);
            recs.sliceByPage(page, count);

            for (Record rec1 : recs) {
                Record user = GlobalLogics.getAccount().getUser(ctx, userId, rec1.getString("user_id"), USER_COLUMNS_SHAK, true);
                user.copyTo(rec1);
            }

        }
        return recs;
    }


    @TraceCall
    @Override
    public RecordSet getUserNearByP(Context ctx, String userId, String longitude0, String latitude0, int page, int count) {
        double longitude_me = Double.parseDouble(longitude0);
        double latitude_me = Double.parseDouble(latitude0);

        RecordSet recs = getUserNearBy(ctx, userId, 0, 1000);
        // 找最近签到的1000个人出来
        if (recs.size() > 0) {
            for (Record rec : recs) {
                //每个人的距离算出来
                double longitude = Double.parseDouble(rec.getString("longitude"));
                double latitude = Double.parseDouble(rec.getString("latitude"));
                double distance = GetDistanceP(ctx, longitude_me, latitude_me, longitude, latitude);
                rec.put("distance", distance);
            }
            recs.sort("distance", true);
            recs.sliceByPage(page, count);

            for (Record rec1 : recs) {
                Record user = GlobalLogics.getAccount().getUser(ctx, userId, rec1.getString("user_id"), USER_COLUMNS_SHAK, true);
                user.copyTo(rec1);
            }
        }
        return recs;
    }
}
