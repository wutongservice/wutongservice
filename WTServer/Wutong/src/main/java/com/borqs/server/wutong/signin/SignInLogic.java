package com.borqs.server.wutong.signin;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface SignInLogic {
    boolean saveSignIn(Context ctx, Record sinIn);

    RecordSet getSignIn(Context ctx, String userId, boolean asc, int page, int count);

    boolean deleteSignIn(Context ctx, String sign_ids);

    RecordSet getUserShaking(Context ctx, String userId, long dateDiff, boolean asc, int page, int count);

    RecordSet getUserNearBy(Context ctx, String userId, int page, int count);

    // platform
    boolean signInP(Context ctx, String userId, String longitude, String latitude, String altitude, String speed, String geo, int type);

    RecordSet getUserShakingP(Context ctx, String userId, String longitude0, String latitude0, int page, int count);

    RecordSet getUserNearByP(Context ctx, String userId, String longitude0, String latitude0, int page, int count);

    double GetDistanceP(Context ctx, double lng1, double lat1, double lng2, double lat2);
}
