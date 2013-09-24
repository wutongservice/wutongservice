package com.borqs.server.pubapi;


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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class PublicApiSupport {

    protected LoginLogic login;
    protected AppLogic app;

    protected PublicApiSupport() {
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
        return parseGeoLocation(rawLocation);
    }

    private static GeoLocation parseGeoLocation(String rawLocation) {
        rawLocation = rawLocation.replace(';', '\n');

        Properties p0 = setAsText(rawLocation);
        return new GeoLocation(Double.parseDouble((String)p0.get("latitude")), Double.parseDouble((String)p0.get("longitude")));
    }

    public static Properties setAsText(String text) throws IllegalArgumentException {
        Properties props = new Properties();
        if (text != null) {
            ByteArrayInputStream bas = null;
            try {
                //cannot parse Chinese language
                bas = new ByteArrayInputStream(text.getBytes("UTF-8"));
                props.load(bas);
            } catch (IOException ex) {
                // Should never happen.
                throw new IllegalArgumentException(
                        "Failed to parse [" + text + "] into Properties: " + ex.getMessage());
            }finally {
                if(bas != null)
                    try {
                        bas.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        return props;
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
                String actualSign = AppSign.sign(signMethod, appSecret, req.getKeys());
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
