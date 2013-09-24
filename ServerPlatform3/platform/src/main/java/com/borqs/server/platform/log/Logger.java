package com.borqs.server.platform.log;


import com.borqs.server.platform.context.Context;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Array;
import java.util.Collection;

public class Logger {
    private final org.slf4j.Logger logger;

    //public static final String LEVEL_TRACE = "t";
    public static final String LEVEL_DEBUG = "d";
    public static final String LEVEL_INFO = "i";
    public static final String LEVEL_WARN = "w";
    public static final String LEVEL_ERROR = "e";
    public static final String LEVEL_OPER = "o";

    private Logger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public static Logger wrap(org.slf4j.Logger logger) {
        return new Logger(logger);
    }

    public static Logger get(String name) {
        return wrap(LoggerFactory.getLogger(name));
    }

    public static Logger get(Class clazz) {
        return wrap(LoggerFactory.getLogger(clazz));
    }

    private static String makeMessage(Context ctx, String level, String msg) {
        if (ctx != null) {
            MDC.put(CsvLayout.KEY_REMOTE, ObjectUtils.toString(ctx.getRemote()));
            MDC.put(CsvLayout.KEY_ACCESS, Long.toString(ctx.accessId));
            MDC.put(CsvLayout.KEY_VIEWER, Long.toString(ctx.getViewer()));
            MDC.put(CsvLayout.KEY_APP, Integer.toString(ctx.getApp()));
            MDC.put(CsvLayout.KEY_INTERNAL, ctx.isInternal() ? "1" : "0");
            MDC.put(CsvLayout.KEY_PRIVACY_ENABLED, ctx.isPrivacyEnabled() ? "1" : "0");
            MDC.put(CsvLayout.KEY_USER_AGENT, ObjectUtils.toString(ctx.getRawUserAgent()));
        } else {
            MDC.remove(CsvLayout.KEY_REMOTE);
            MDC.remove(CsvLayout.KEY_ACCESS);
            MDC.remove(CsvLayout.KEY_VIEWER);
            MDC.remove(CsvLayout.KEY_APP);
            MDC.remove(CsvLayout.KEY_INTERNAL);
            MDC.remove(CsvLayout.KEY_PRIVACY_ENABLED);
            MDC.remove(CsvLayout.KEY_USER_AGENT);
        }

        if (level != null) {
            MDC.put(CsvLayout.KEY_LEVEL, level);
        } else {
            MDC.remove(CsvLayout.KEY_LEVEL);
        }

        return msg;
    }

    public String getName() {
        return logger.getName();
    }

//    public boolean isTraceEnabled() {
//        return logger.isTraceEnabled();
//    }
//
//    public void trace(Context ctx, String msg) {
//        logger.trace(makeMessage(ctx, LEVEL_TRACE, msg));
//    }
//
//    public void trace(Context ctx, String msg, Object... args) {
//        logger.trace(makeMessage(ctx, LEVEL_TRACE, String.format(msg, args)));
//    }
//
//    public void trace(Context ctx, Throwable t, String msg) {
//        logger.trace(makeMessage(ctx, LEVEL_TRACE, msg), t);
//    }
//
//    public void trace(Context ctx, Throwable t, String msg, Object... args) {
//        logger.trace(makeMessage(ctx, LEVEL_TRACE, String.format(msg, args)), t);
//    }


    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public void error(Context ctx, String msg) {
        logger.error(makeMessage(ctx, LEVEL_ERROR, msg));
    }

    public void error(Context ctx, String msg, Object... args) {
        logger.error(makeMessage(ctx, LEVEL_ERROR, String.format(msg, args)));
    }

    public void error(Context ctx, Throwable t, String msg) {
        logger.error(makeMessage(ctx, LEVEL_ERROR, msg), t);
    }

    public void error(Context ctx, Throwable t, String msg, Object... args) {
        logger.error(makeMessage(ctx, LEVEL_ERROR, String.format(msg, args)), t);
    }


    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public void info(Context ctx, String msg) {
        logger.info(makeMessage(ctx, LEVEL_INFO, msg));
    }

    public void info(Context ctx, String msg, Object... args) {
        logger.info(makeMessage(ctx, LEVEL_INFO, String.format(msg, args)));
    }

    public void info(Context ctx, Throwable t, String msg) {
        logger.info(makeMessage(ctx, LEVEL_INFO, msg), t);
    }

    public void info(Context ctx, Throwable t, String msg, Object... args) {
        logger.info(makeMessage(ctx, LEVEL_INFO, String.format(msg, args)), t);
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public void warn(Context ctx, String msg) {
        logger.warn(makeMessage(ctx, LEVEL_WARN, msg));
    }

    public void warn(Context ctx, String msg, Object... args) {
        logger.warn(makeMessage(ctx, LEVEL_WARN, String.format(msg, args)));
    }

    public void warn(Context ctx, Throwable t, String msg) {
        logger.warn(makeMessage(ctx, LEVEL_WARN, msg), t);
    }

    public void warn(Context ctx, Throwable t, String msg, Object... args) {
        logger.warn(makeMessage(ctx, LEVEL_WARN, String.format(msg, args)), t);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(Context ctx, String msg) {
        logger.debug(makeMessage(ctx, LEVEL_DEBUG, msg));
    }

    public void debug(Context ctx, String msg, Object... args) {
        logger.debug(makeMessage(ctx, LEVEL_DEBUG, String.format(msg, args)));
    }

    public void debug(Context ctx, Throwable t, String msg) {
        logger.debug(makeMessage(ctx, LEVEL_DEBUG, msg), t);
    }

    public void debug(Context ctx, Throwable t, String msg, Object... args) {
        logger.debug(makeMessage(ctx, LEVEL_DEBUG, String.format(msg, args)), t);
    }

    public boolean isOperEnabled() {
        return isInfoEnabled();
    }

    public void oper(Context ctx, String action, Object param) {
        if (param != null)
            logger.info(makeMessage(ctx, LEVEL_OPER, action + " # " + paramToStr(param)));
        else
            logger.info(makeMessage(ctx, LEVEL_OPER, action));
    }

    private static String paramToStr(Object o) {
        if (o.getClass().isArray()) {
            int len = Array.getLength(o);
            Object[] a = new Object[len];
            for (int i = 0; i < len; i++)
                a[i] = Array.get(o, i);
            return StringUtils.join(a, ",");
        } else if (o instanceof Collection) {
            return StringUtils.join((Collection)o, ",");
        } else {
            return o.toString();
        }
    }
}
