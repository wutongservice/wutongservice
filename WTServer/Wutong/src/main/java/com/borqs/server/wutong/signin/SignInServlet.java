package com.borqs.server.wutong.signin;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

import static com.borqs.server.wutong.Constants.*;

public class SignInServlet extends WebMethodServlet {
    public SignInServlet() {
    }


    @WebMethod("place/checkin")
    public Record userSignIn(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();
        StreamLogic p = GlobalLogics.getStream();

        Context ctx = WutongContext.getContext(qp, true);
        String loc = ctx.getLocation();
        if (!StringUtils.isBlank(loc)) {
            String longitude = parseLocation(loc, "longitude");
            String latitude =  parseLocation(loc, "latitude");
            String altitude =  parseLocation(loc, "altitude");
            String speed =  parseLocation(loc, "speed");
            String geo =  parseLocation(loc, "geo");

            String message = getBundleString(ctx.getUa(), "platform.stream.sign.imhere");
            if (latitude.length() > 0 && latitude.length() > 0)
                si.signInP(ctx, ctx.getViewerIdString(), longitude, latitude, altitude, speed, geo, 0);
            Record mock = p.postP(ctx, ctx.getViewerIdString(), SIGN_IN_POST, message, "[]", String.valueOf(APP_TYPE_BPC),
                    "", "", qp.getString("app_data", ""), "", false, "", "", ctx.getLocation(), "", "", true, true, true, "", Constants.POST_SOURCE_PEOPLE, qp.getInt("scene", 0L));
//            return p.getFullPostsForQiuPuP(ctx, ctx.getViewerIdString(), post_id, true).getFirstRecord();
            return mock;
        } else {
            throw new ServerException(WutongErrors.COMMON_GEO_ERROR, "Must have location");
        }
    }

    @WebMethod("user/shaking")
    public RecordSet userShaking(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();

        Context ctx = WutongContext.getContext(qp, true);
        String loc = ctx.getLocation();
        if (!StringUtils.isBlank(loc)) {
            String longitude = parseLocation(loc, "longitude");
            String latitude = parseLocation(loc, "latitude");
            String altitude = parseLocation(loc, "altitude");
            String speed = parseLocation(loc, "speed");
            String geo = parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                si.signInP(ctx, ctx.getViewerIdString(), longitude, latitude, altitude, speed, geo, 1);
            RecordSet recs = si.getUserShakingP(ctx, ctx.getViewerIdString(), longitude, latitude, (int) qp.getInt("page", 0), (int) qp.getInt("count", 100));
            //find who shaking in 3 minutes
            return recs;
        } else {
            throw new ServerException(WutongErrors.COMMON_GEO_ERROR, "Must have location");
        }
    }

    @WebMethod("user/nearby")
    public RecordSet userNearBy(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();

        Context ctx = WutongContext.getContext(qp, true);
        String loc = ctx.getLocation();

        if (!StringUtils.isBlank(loc)) {
            String longitude = parseLocation(loc, "longitude");
            String latitude = parseLocation(loc, "latitude");
            String altitude = parseLocation(loc, "altitude");
            String speed = parseLocation(loc, "speed");
            String geo = parseLocation(loc, "geo");
            if (latitude.length() > 0 && latitude.length() > 0)
                si.signInP(ctx, ctx.getViewerIdString(), longitude, latitude, altitude, speed, geo, 2);
            RecordSet recs = si.getUserNearByP(ctx, ctx.getViewerIdString(), longitude, latitude, (int) qp.getInt("page", 0), (int) qp.getInt("count", 100));
            //find who shaking in 3 minutes
            return recs;
        } else {
            throw new ServerException(WutongErrors.COMMON_GEO_ERROR, "Must have location");
        }
    }

    @WebMethod("user/distance")
    public String getDistance(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();

        Context ctx = WutongContext.getContext(qp, false);

        double lot1 = 116.4633908;
        double lat1 = 39.9851468;
        double lot2 = 116.4639472;
        double lat2 = 39.9853495;

        double n1 = si.GetDistanceP(ctx, lot1, lat1, lot2, lat2);

        double n2 = si.GetDistanceP(ctx, lot2, lat2, lot1, lat1);

        return "";
    }

    @WebMethod("place/get")
    public RecordSet userGetSignIn(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();

        Context ctx = WutongContext.getContext(qp, true);
        return si.getSignIn(ctx, ctx.getViewerIdString(),qp.getBoolean("asc",false),(int)qp.getInt("page",0),(int)qp.getInt("count",20));
    }

    @WebMethod("place/remove")
    public boolean userRemoveSignIn(HttpServletRequest req, QueryParams qp) {
        SignInLogic si = GlobalLogics.getSignIn();

        Context ctx = WutongContext.getContext(qp, true);
        return si.deleteSignIn(ctx, qp.checkGetString("checkin_ids"));
    }

}
