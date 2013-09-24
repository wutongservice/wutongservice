package com.borqs.server.base.log;


import com.borqs.server.base.context.Context;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;

public class Logger {
    private final org.slf4j.Logger logger;

    private final static LogSwitchLogic logswitch = new LogSwitchImpl();

    private Logger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public static Logger getLogger(String name) {
        return new Logger(LoggerFactory.getLogger(name));
    }

    public static Logger getLogger(Class clazz) {
        return new Logger(LoggerFactory.getLogger(clazz));
    }

    private String makeMessage(Context ctx, String title, String msg, Throwable t) {
        StringBuilder buff = new StringBuilder();
        if (ctx != null) {
            String viewerId = ctx.getViewerId() > 0 ? Long.toString(ctx.getViewerId()) : "";
            buff.append("[").append(viewerId).append("]");
            buff.append("[").append(ObjectUtils.toString(ctx.getAppId())).append("]");
            buff.append("[").append(ObjectUtils.toString(ctx.getClientCallId())).append("]");
            buff.append("[").append(ObjectUtils.toString(ctx.getServerCallId())).append("]");
            buff.append("[").append(ObjectUtils.toString(ctx.getUa()).split(" ")[0]).append("]");
            buff.append("[").append(ObjectUtils.toString(ctx.getLocation())).append("]");
        } else {
            buff.append("[]");
            buff.append("[]");
            buff.append("[]");
            buff.append("[]");
            buff.append("[]");
            buff.append("[]");
        }
        if (title != null) {
            buff.append(" ").append(title);
        }
        buff.append(" ").append(ObjectUtils.toString(msg));
        if (t != null) {
            StringWriter w = new StringWriter();
            t.printStackTrace(new PrintWriter(w));
            buff.append(" ").append(w.toString());
        }
        return buff.toString();
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled() && logswitch.isDebugEnable(logger.getName());
    }

    public void trace(Context ctx, String msg) {
        if (logger.isTraceEnabled())
            logger.trace(makeMessage(ctx, null, msg, null));
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled() && logswitch.isDebugEnable(logger.getName());
    }

    public void debug(Context ctx, String msg) {
        if(logger.isDebugEnabled())
        logger.debug(makeMessage(ctx, null, msg, null));
    }

    public void info(Context ctx, String msg) {
        logger.info(makeMessage(ctx, null, msg, null));
    }

    public void warn(Context ctx, Throwable t) {
        logger.warn(makeMessage(ctx, null, null, t));
    }

    public void warn(Context ctx, Throwable t, String msg) {
        logger.warn(makeMessage(ctx, null, msg, t));
    }

    public void error(Context ctx, Throwable t) {
        logger.error(makeMessage(ctx, null, null, t));
    }

    public void error(Context ctx, Throwable t, String msg) {
        logger.error(makeMessage(ctx, null, msg, t));
    }

    private static String paramToStr(Object o) {
        if (o == null)
            return "null";

        StringBuilder buff = new StringBuilder();
        if (o instanceof CharSequence) {
            buff.append('\"').append(o).append('\"');
        } else if (o instanceof Number || o instanceof Boolean) {
            buff.append(o);
        } else if (o instanceof Character) {
            buff.append('\'').append(o).append('\'');
        } else if (o.getClass().isArray()) {
            buff.append('[');
            int len = Array.getLength(o);
            if (len > 0) {
                buff.append(paramToStr(Array.get(o, 0)));
            }
            for (int i = 1; i < len; i++) {
                buff.append(", ");
                buff.append(paramToStr(Array.get(o, i)));
            }
            buff.append(']');
        } else {
            buff.append("#OBJ:").append(o);
        }
        return buff.toString();
    }

    private static final String START_CALL_TRACED_KEY = "log.traceStartCall";
    public void traceStartCall(Context ctx, String method, Object... params) {
        if (logger.isTraceEnabled()) {
            if (ctx == null)
                return;

            Boolean traced = (Boolean) ctx.getSession(START_CALL_TRACED_KEY);
            if (BooleanUtils.isTrue(traced))
                return;

            StringBuilder buff = new StringBuilder();
            buff.append("(");
            if (params.length > 0) {
                buff.append(paramToStr(params[0]));
                for (int i = 1; i < params.length; i++) {
                    buff.append(", ");
                    buff.append(paramToStr(params[i]));
                }
            }
            buff.append(")");

            logger.trace(makeMessage(ctx, "->", method + buff.toString(), null));
            ctx.putSession(START_CALL_TRACED_KEY, true);
        }
    }

    public void traceEndCall(Context ctx, String method) {
        //if (logger.isTraceEnabled())
        //    logger.trace(makeMessage(ctx, "<-", method, null));
    }

    public boolean isOpEnabled() {
        return logger.isInfoEnabled();
    }

    public void op(Context ctx, String msg) {
        logger.info(makeMessage(ctx, "OP:", msg, null));
    }

    public void traceSql(Context ctx, String sql) {
        if (logger.isTraceEnabled())
            logger.trace(makeMessage(ctx, "SQL:", sql, null));
    }

    public void traceHttpReq(HttpServletRequest req) {
        String qs = "";
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            qs = ObjectUtils.toString(req.getQueryString());
        }

        if (qs.isEmpty()) {
            trace(null, String.format("REQ: %s %s", req.getMethod(), req.getRequestURI()));
        } else {
            trace(null, String.format("REQ: %s %s?%s", req.getMethod(), req.getRequestURI(), qs));
        }
    }

    public void traceHttpResp(Object resp) {
        //trace(null, "RESP: " + resp);
    }
}
