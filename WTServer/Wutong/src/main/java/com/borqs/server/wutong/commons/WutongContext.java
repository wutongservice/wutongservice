package com.borqs.server.wutong.commons;


import com.borqs.server.ServerException;
import com.borqs.server.base.auth.WebSignatures;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ElapsedCounter;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.UserLastVisitTimeCache;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;

public class WutongContext {

    public static String checkTicket(QueryParams qp) {
        String userId = GlobalLogics.getAccount().whoLogined(Context.dummy(), qp.checkGetString("ticket"));
        if (Constants.isNullUserId(userId))
            throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "Invalid ticket");
        return userId;
    }

    public static void checkSign(QueryParams qp) {
        Context ctx = Context.dummy();
        String appId = qp.checkGetString("appid");
        String sign = qp.checkGetString("sign");
        String signMethod = qp.getString("sign_method", "md5");

        String secret = GlobalLogics.getApp().getAppSecret(ctx, Integer.parseInt(appId));
        if (secret == null)
            throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "App secret error");

        if (!"md5".equalsIgnoreCase(signMethod))
            throw new ServerException(WutongErrors.MD5_INVALID, "Invalid sign method");

        String expectantSign = WebSignatures.md5Sign(secret, qp.keySet());
        if (!StringUtils.equals(sign, expectantSign))
            throw new ServerException(WutongErrors.MD5_INVALID, "Invalid md5 signatures");
    }

    public static String checkSignAndTicket(QueryParams qp) {
        checkSign(qp);
        return checkTicket(qp);
    }

    private static void fillUserScopes(Context ctx) {
        if (ctx.getViewerId() <= 0L)
            return;

        Configuration conf = GlobalConfig.get();
        if (conf != null) {
            HashSet<String> inUserScopes = new HashSet<String>();
            for (String key : conf.keySet()) {
                if (key.startsWith("userScope.")) {
                    long[] userIds = StringUtils2.splitIntArray(conf.getString(key, ""), ",");
                    if (ArrayUtils.contains(userIds, ctx.getViewerId()))
                        inUserScopes.add(StringUtils.removeStartIgnoreCase(key, "userScope."));
                }
            }
            ctx.putSession("inUserScopes", inUserScopes);
        }
    }

    public static Context getContextWithoutAppAndViewer(QueryParams qp) {
        Context ctx = new Context();

        // client call id
        ctx.setClientCallId(qp.getString("call_id", ""));

        // server call id
        ctx.setServerCallId(Long.toString(RandomUtils.generateId()));

        // user agent
        ctx.setUa(qp.getString("$ua", ""));

        // location
        ctx.setLocation(qp.getString("$loc", ""));

        // language
        ctx.setLanguage(qp.getString("$lang", ""));

        // appId
        ctx.setAppId("0");

        // viewerId
        ctx.setViewerId(0L);

        ctx.setElapsedCounter((ElapsedCounter) qp.get("$ec"));

        qp.removeKeys("$ua", "$loc", "$lang", "$ec");

        return ctx;
    }

    public static Context getContext(QueryParams qp, boolean needLogin) {
        Context ctx = new Context();


        // client call id
        ctx.setClientCallId(qp.getString("call_id", ""));

        // server call id
        ctx.setServerCallId(Long.toString(RandomUtils.generateId()));

        // user agent
        ctx.setUa(qp.getString("$ua", ""));

        // location
        ctx.setLocation(qp.getString("$loc", ""));

        // language
        ctx.setLanguage(qp.getString("$lang", ""));

        ctx.setElapsedCounter((ElapsedCounter) qp.get("$ec"));

        //scene
        ctx.putSession("scene",qp.getString("scene",""));

        qp.removeKeys("$ua", "$loc", "$lang", "$ec");

        // check sign
        String appId = qp.getString("appid", null);
        if (StringUtils.isNotEmpty(appId)) {
            String sign = qp.checkGetString("sign");
            String signMethod = qp.getString("sign_method", "md5");

            String secret = GlobalLogics.getApp().getAppSecret(ctx, Integer.parseInt(appId));
            if (secret == null)
                throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "App secret error");

            if (!"md5".equalsIgnoreCase(signMethod))
                throw new ServerException(WutongErrors.MD5_INVALID, "Invalid sign method");

            String expectantSign = WebSignatures.md5Sign(secret, qp.keySet());
            if (!StringUtils.equals(sign, expectantSign))
                throw new ServerException(WutongErrors.MD5_INVALID, "Invalid md5 signatures");

            ctx.setAppId(appId);
        } else {
            ctx.setAppId("0");
        }


        // check ticket
        String ticket = qp.getString("ticket", null);
        if (needLogin && StringUtils.isEmpty(ticket))
            throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "Need ticket");

        if (StringUtils.isNotEmpty(ticket)) {
            String viewerId = GlobalLogics.getAccount().whoLogined(ctx, ticket);
            if (Constants.isNullUserId(viewerId))
                throw new ServerException(WutongErrors.AUTH_TICKET_INVALID, "Invalid ticket");

            ctx.setViewerId(Long.parseLong(viewerId));
        } else {
            ctx.setViewerId(0L);
        }

        //add last visit time to cache
        UserLastVisitTimeCache.set(ctx.getViewerIdString(), DateUtils.nowMillis());
        fillUserScopes(ctx);
        return ctx;
    }

}
