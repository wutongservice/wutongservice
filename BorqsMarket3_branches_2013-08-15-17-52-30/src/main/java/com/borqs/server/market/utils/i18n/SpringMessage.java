package com.borqs.server.market.utils.i18n;


import com.borqs.server.market.context.ServiceContext;
import org.apache.commons.lang.LocaleUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.Locale;

public class SpringMessage {
    private static final SpringMessage instance = new SpringMessage();

    private MessageSource messageSource;
    private LocaleResolver localeResolver;

    private SpringMessage() {
    }

    public static SpringMessage getInstance() {
        return instance;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public static String get(String code, String locale) {
        return get(code, LocaleUtils.toLocale(locale));
    }

    public static String get(String code, Locale locale) {
        return instance.messageSource.getMessage(code, null, locale);
    }

    public static String get(String code, ServiceContext ctx) {
        return get(code, ctx.getClientLocale());
    }

    public static String get(String code, PageContext pageContext) {
        Locale locale = instance.localeResolver.resolveLocale((HttpServletRequest) pageContext.getRequest());
        return get(code, locale);
    }
}
