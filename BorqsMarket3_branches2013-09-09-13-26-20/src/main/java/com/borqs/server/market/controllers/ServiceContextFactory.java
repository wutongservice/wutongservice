package com.borqs.server.market.controllers;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.record.Record;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;


@Component
public class ServiceContextFactory {

    private LocaleResolver localeResolver;
    private AccountService accountService;

    public ServiceContextFactory() {
    }

    public AccountService getAccountService() {
        return accountService;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
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

    private static boolean isBorqsUserAgent(String userAgent) {
        // TODO: check
        return false;
    }

    public ServiceContext create(HttpServletRequest req)  {
        ServiceContext ctx = Attributes.getServiceContext(req);
        if (ctx != null)
            return ctx;

        ctx = new ServiceContext();
        String userAgent = ObjectUtils.toString(req.getHeader("User-Agent"));
        String ticket = req.getParameter("ticket");
        if (ticket == null) {
            Cookie ticketCookie = WebUtils.getCookie(req, CookieNames.TICKET);
            if (ticketCookie != null)
                ticket = ticketCookie.getValue();
        }

        ctx.setAccessTime(DateTimeUtils.nowMillis());
        ctx.setClientIP(req.getRemoteAddr());
        ctx.setClientUserAgent(userAgent);
        ctx.setClientDeviceId(req.getParameter("device_id"));


        if (!isBorqsUserAgent(userAgent)) {
            UserAgent ua = UserAgent.parseUserAgentString(userAgent);
            if (ua != null) {
                // Browser
                ctx.setClientDeviceType(ua.getOperatingSystem().getDeviceType().getName());
                ctx.setClientOS(ua.getOperatingSystem().getName());
                ctx.setClientBrowserRenderEngine(ua.getBrowser().getRenderingEngine().name());
            } else {
                ctx.setClientDeviceType("Unknown");
                ctx.setClientOS("Unknown");
                ctx.setClientBrowserRenderEngine("Unknown");
            }
        } else {
            ctx.setClientDeviceType("");
            ctx.setClientOS("");
            ctx.setClientBrowserRenderEngine("");
        }


        String locale = req.getParameter("locale");
        if (StringUtils.isEmpty(locale)) {
            Locale l = localeResolver.resolveLocale(req);
            locale = StringUtils.trimToNull(l.toString());
//            String acceptLangs = req.getHeader("Accept-Language");
//            if (acceptLangs != null) {
//                lang = trimLang(StringUtils.substringBefore(acceptLangs, ","));
//            } else {
//                lang = "en_US";
//            }
        }
        ctx.setClientLocale(locale);

        ctx.setTicket(ticket);
        if (ticket != null) {
            Record account = accountService.getAccountIdByTicket(ctx, ticket, false);
            if (account == null)
                throw new ServiceException(Errors.E_ILLEGAL_TICKET, "Illegal ticket");
            ctx.setAccountId(account.asString("id"));
            ctx.setAccountName(account.asString("name", ""));
            ctx.setAccountEmail(account.asString("email", ""));
            ctx.setBorqs(account.asBoolean("borqs", false));
            ctx.setBoss(account.asBoolean("boss", false));
        }

        // set roles
        ctx.setRoles(accountService.getRoles(ctx));

        Attributes.setServiceContext(req, ctx);
        return ctx;
    }

}
