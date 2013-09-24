package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.services.AccountService;
import com.borqs.server.market.services.UserId;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.StringUtils2;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;


@Component
public class ServiceContextFactory {

    private AccountService accountService;

    public ServiceContextFactory() {
    }

    public AccountService getAccountService() {
        return accountService;
    }

    @Autowired
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    private static String trimLang(String lang) {
        lang = StringUtils.replace(lang, "-", "_");
        if (StringUtils.contains(lang, '_')) {
            String[] ss = StringUtils.split(lang, "_", 2);
            return ss[0] + "_" + ss[1].toUpperCase();
        } else {
            return lang.toLowerCase();
        }
    }

    public ServiceContext create(HttpServletRequest req) throws ServiceException {
        ServiceContext ctx = new ServiceContext();

        String userAgent = req.getHeader("User-Agent");
        String ticket = req.getParameter("ticket");

        ctx.setAccessTime(DateTimeUtils.nowMillis());
        ctx.setClientIP(req.getRemoteAddr());
        ctx.setClientUserAgent(userAgent);
        ctx.setClientDeviceId(req.getParameter("device_id"));

        UserAgent ua = UserAgent.parseUserAgentString(userAgent);
        if (ua != null) {
            // Browser
            DeviceType dt = ua.getOperatingSystem().getDeviceType();
            switch (dt) {
                case COMPUTER:
                    ctx.setClientDeviceType("pc");
                    break;
                case TABLET:
                    ctx.setClientDeviceType("pad");
                    break;
                case MOBILE:
                    ctx.setClientDeviceType("phone");
                    break;
            }
            ctx.setClientOS(ua.getOperatingSystem().getName());
            ctx.setClientBrowserRenderEngine(ua.getBrowser().getRenderingEngine().name());
        } else {
            // Try borqs user agent
            // TODO: xx
        }

        String lang = req.getParameter("locale");
        if (StringUtils.isEmpty(lang)) {
            String acceptLangs = req.getHeader("Accept-Language");
            if (acceptLangs != null) {
                lang = trimLang(StringUtils.substringBefore(acceptLangs, ","));
            } else {
                lang = "en_US";
            }
        }
        ctx.setClientLanguage(lang);

        String googleIds = req.getParameter("google_ids");
        if (googleIds != null) {
            ctx.setClientGoogleIds(StringUtils2.splitArray(googleIds, ',', true));
        }

        if (ticket != null) {
            UserId uid = accountService.getUserId(ctx, ticket);
            ctx.setClientRole(uid.getRole());
            ctx.setClientId(uid.getId());
        }

        return ctx;
    }

}
