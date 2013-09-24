package com.borqs.server.platform.log;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.MDC;

public class CsvLayout extends LayoutBase<ILoggingEvent> {

    static final String KEY_REMOTE = "b.r";
    static final String KEY_ACCESS = "b.a";
    static final String KEY_VIEWER = "b.v";
    static final String KEY_APP = "b.A";
    static final String KEY_USER_AGENT = "b.u";
    static final String KEY_INTERNAL = "b.i";
    static final String KEY_PRIVACY_ENABLED = "b.p";
    static final String KEY_LEVEL = "b.l";


    // logback
    public static final char NAME = 'n';
    public static final char THREAD = 'T';
    public static final char LEVEL = 'l';
    public static final char MESSAGE = 'm';
    public static final char TIMESTAMP = 't';
    public static final char ERROR = 'E';

    // context
    public static final char REMOTE = 'r';
    public static final char ACCESS = 'a';
    public static final char VIEWER = 'v';
    public static final char APP = 'A';
    public static final char USER_AGENT = 'u';
    public static final char INTERNAL = 'i';
    public static final char PRIVACY_ENABLED = 'p';

    public static final String DEFAULT_OPTIONS = "tlnTravAuipmE";

    private volatile String options;

    public CsvLayout() {
        this(DEFAULT_OPTIONS);
    }

    public CsvLayout(String options) {
        this.options = options;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    @Override
    public String doLayout(ILoggingEvent e) {
        StringBuilder buff = new StringBuilder();

        String opts = options;
        int len = opts.length();
        for (int i = 0; i < len; i++) {
            if (i > 0)
                buff.append(',');

            char c = opts.charAt(i);
            switch (c) {
                case NAME:
                    buff.append(escape(e.getLoggerName()));
                    break;

                case THREAD:
                    buff.append(escape(e.getThreadName()));
                    break;

                case LEVEL: {
                    if (e.getLevel().levelInt == Level.INFO_INT)
                        buff.append(MDC.get(KEY_LEVEL).equals(Logger.LEVEL_INFO) ? e.getLevel().levelStr : "OPER");
                    else
                        buff.append(e.getLevel().levelStr);
                }
                break;

                case MESSAGE:
                    buff.append(escape(e.getMessage()));
                    break;

                case TIMESTAMP:
                    buff.append(DateHelper.formatDateAndTime(e.getTimeStamp()));
                    break;

                case ERROR: {
                    IThrowableProxy tp = e.getThrowableProxy();
                    if (tp != null)
                        buff.append(escape(throwToString(tp)));
                }
                break;

                case REMOTE:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_REMOTE)));
                    break;

                case ACCESS:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_ACCESS)));
                    break;

                case VIEWER:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_VIEWER)));
                    break;

                case APP:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_APP)));
                    break;

                case USER_AGENT:
                    buff.append(ObjectUtils.toString(escape(e.getMdc().get(KEY_USER_AGENT))));
                    break;

                case INTERNAL:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_INTERNAL)));
                    break;

                case PRIVACY_ENABLED:
                    buff.append(ObjectUtils.toString(e.getMdc().get(KEY_PRIVACY_ENABLED)));
                    break;
            }
        }
        buff.append('\n');
        return buff.toString();
    }

    private static String escape(String s) {
        return s == null ? "" : StringEscapeUtils.escapeCsv(s);
    }

    private static String throwToString(IThrowableProxy tp) {
        StringBuilder buff = new StringBuilder();
        throwToStringHelper(tp, buff);
        return buff.toString();
    }

    private static void throwToStringHelper(IThrowableProxy tp, StringBuilder buff) {
        buff.append(tp.getClassName()).append(":\'").append(tp.getMessage()).append("\' ");
        StackTraceElementProxy[] stack = tp.getStackTraceElementProxyArray();
        if (ArrayUtils.isNotEmpty(stack)) {
            buff.append(" << ");
            for (StackTraceElementProxy e : stack)
                buff.append(e.getStackTraceElement().toString()).append(" << ");
        }
        IThrowableProxy cause = tp.getCause();
        if (cause != null) {
            buff.append("|| ");
            throwToStringHelper(cause, buff);
        }
    }
}
