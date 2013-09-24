package com.borqs.server.intrapi;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.app.AppLogic;
import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.util.GeoLocation;
import com.borqs.server.platform.web.topaz.Request;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public abstract class InternalApiSupport {

    protected LoginLogic login;
    protected AppLogic app;

    protected InternalApiSupport() {
    }

    public LoginLogic getLogin() {
        return login;
    }

    public void setLogin(LoginLogic login) {
        this.login = login;
    }

    public AppLogic getApp() {
        return app;
    }

    public void setApp(AppLogic app) {
        this.app = app;
    }

    public static GeoLocation getGeoLocation(String rawLocation) {
        // longitude=116.357711;latitude=39.9545312;altitude=0.0;speed=0.0;time=1332805233205;geo=Beijing,xxx
        // TODO: parse
        return new GeoLocation(0.0, 0.0);
    }

    public Context checkContext(Request req, boolean needLogin) {
        String ticket = req.getString("ticket", null);
        Context ctx = new Context();

        // location
        String loc = req.decodeHeader("location");
        ctx.setLocation(loc);
        if (StringUtils.isNotEmpty(loc))
            ctx.setGeoLocation(getGeoLocation(loc));

        // user agent
        ctx.setRawUserAgent(req.getRawUserAgent());

        // flags
        ctx.setInternal(false);
        ctx.setPrivacyEnabled(true);
        if (ticket != null)
            ctx.setViewer(login.whoLogined(ctx, ticket));

        // remote
        ctx.setRemote(ObjectUtils.toString(req.httpRequest.getRemoteAddr()));

        if (needLogin)
            ctx.checkLogined();

        int appId = req.getInt("appid", App.APP_NONE);
        if (appId > 0) {
            ctx.setApp(appId);
            String signMethod = req.getString("sign_method", "md5");
            String sign = req.getString("sign", null);
            if (sign != null) {
                String appSecret = app.getApp(ctx, appId).getSecret();
                String actualSign = AppSign.sign(signMethod, appSecret, req.getHttpKeys());
                if (!actualSign.equals(sign))
                    throw new ServerException(E.INVALID_SIGN);
            }
        }

        return ctx;
    }

    public static PeopleIds checkPeopleIds(Request req, String key) {
        String s = req.checkString(key);
        return PeopleIds.parse(null, s).unique();
    }

    public static PeopleIds checkPeopleIds(Request req, String prefKey, String secondKey) {
        String s = req.checkString(prefKey, secondKey);
        return PeopleIds.parse(null, s).unique();
    }
}
