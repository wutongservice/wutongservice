package com.borqs.server.platform.log;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class LogCall {

    private static final String START_CALL_TAG = "-> ";
    private static final String END_CALL_TAG = "<- ";

    private final String functionWithClass;
    private final Logger logger;
    private final Context context;
    private int callIndent;

    private LogCall(String function, Logger logger, Context context) {
        this.functionWithClass = function;
        this.logger = logger;
        this.context = context;
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.<String, Object>of());
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, String p1, Object v1) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.of(p1, v1));
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, String p1, Object v1, String p2, Object v2) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.of(p1, v1, p2, v2));
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, String p1, Object v1, String p2, Object v2, String p3, Object v3) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.of(p1, v1, p2, v2, p3, v3));
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, String p1, Object v1, String p2, Object v2, String p3, Object v3, String p4, Object v4) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.of(p1, v1, p2, v2, p3, v3, p4, v4));
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, Object[][] params) {
        return startCall(logger, clazz, func, ctx, CollectionsHelper.arraysToMap(params));
    }

    public static LogCall startCall(Logger logger, Class clazz, String func, Context ctx, Map<String, Object> params) {
        LogCall l = new LogCall(clazz.getName() + "." + func, logger, ctx);
        // TODO: format params
        if (l.context != null) {
            l.callIndent = l.context.getCallIndent();
            l.context.setCallIndent(l.callIndent + 1);
            if (l.logger.isDebugEnabled())
                l.logger.debug(ctx, StringUtils.repeat("  ", l.callIndent) + START_CALL_TAG + l.functionWithClass);
        } else {
            if (l.logger.isDebugEnabled())
                l.logger.debug(null, START_CALL_TAG + l.functionWithClass);
        }
        return l;
    }

    public void endCall() {
        if (context != null) {
            if (logger.isDebugEnabled())
                logger.debug(context, StringUtils.repeat("  ", callIndent) + END_CALL_TAG + functionWithClass);
            context.setCallIndent(callIndent);
        } else {
            if (logger.isDebugEnabled())
                logger.debug(context, END_CALL_TAG + functionWithClass);
        }
    }

    public void endCall(Throwable t) {
        if (context != null) {
            if (logger.isDebugEnabled())
                logger.debug(context, t, StringUtils.repeat("  ", callIndent) + END_CALL_TAG + functionWithClass);
            context.setCallIndent(callIndent);
        } else {
            if (logger.isDebugEnabled())
                logger.debug(context, t, END_CALL_TAG + functionWithClass);
        }
    }
}
