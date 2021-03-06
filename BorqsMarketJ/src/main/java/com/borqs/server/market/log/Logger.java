package com.borqs.server.market.log;


import com.borqs.server.market.context.ServiceContext;
import org.apache.commons.lang.ObjectUtils;

public class Logger {
    private final org.apache.log4j.Logger logger;

    private Logger(org.apache.log4j.Logger logger) {
        this.logger = logger;
    }

    public static Logger get(String name) {
        return new Logger(org.apache.log4j.Logger.getLogger(name));
    }

    public static Logger get(Class clazz) {
        return new Logger(org.apache.log4j.Logger.getLogger(clazz));
    }

    private static String withContext(ServiceContext ctx, Object msg) {
        // TODO: xx
        return ObjectUtils.toString(msg);
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public void trace(ServiceContext ctx, Object msg) {
        logger.trace(withContext(ctx, msg));
    }

    public void trace(ServiceContext ctx, Object msg, Throwable t) {
        logger.trace(withContext(ctx, msg), t);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void debug(ServiceContext ctx, Object msg) {
        logger.debug(withContext(ctx, msg));
    }

    public void debug(ServiceContext ctx, Object msg, Throwable t) {
        logger.debug(withContext(ctx, msg), t);
    }

    public void info(ServiceContext ctx, Object msg) {
        logger.info(withContext(ctx, msg));
    }

    public void info(ServiceContext ctx, Object msg, Throwable t) {
        logger.info(withContext(ctx, msg), t);
    }

    public void warn(ServiceContext ctx, Object msg) {
        logger.warn(withContext(ctx, msg));
    }

    public void warn(ServiceContext ctx, Object msg, Throwable t) {
        logger.warn(withContext(ctx, msg), t);
    }

    public void error(ServiceContext ctx, Object msg) {
        logger.error(withContext(ctx, msg));
    }

    public void error(ServiceContext ctx, Object msg, Throwable t) {
        logger.error(withContext(ctx, msg), t);
    }

    public void fatal(ServiceContext ctx, Object msg) {
        logger.fatal(withContext(ctx, msg));
    }

    public void fatal(ServiceContext ctx, Object msg, Throwable t) {
        logger.fatal(withContext(ctx, msg), t);
    }
}
