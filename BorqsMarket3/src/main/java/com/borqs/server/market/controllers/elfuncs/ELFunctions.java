package com.borqs.server.market.controllers.elfuncs;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.controllers.Attributes;
import com.borqs.server.market.service.PublishService;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.i18n.CountryNames;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ELFunctions {

    public static String getRequestLocale(PageContext pageContext) {
        ServiceContext ctx = Attributes.getServiceContext((HttpServletRequest)pageContext.getRequest());
        String locale = ctx != null ? ctx.getClientLocale() : "en_US";
        if (locale == null) {
            locale = "en_US";
        }
        return locale;
    }

    public static String datetimeToStr(PageContext pageContext, long millis) {
        return DateTimeUtils.formatDateTimeWithLocale(millis, getRequestLocale(pageContext));
    }

    public static String dateToStr(PageContext pageContext, long millis) {
        return DateTimeUtils.formatDateWithLocale(millis, getRequestLocale(pageContext));
    }

    public static String springMessage(PageContext pageContext, String code) {
        return SpringMessage.get(code, pageContext);
    }

    public static String formatCountryName(PageContext pageContext, String code) {
        return CountryNames.get(code, getRequestLocale(pageContext));
    }

    public static String formatSupportedMod(String[] mods) {
        if (ArrayUtils.isEmpty(mods)) {
            return "";
        } else {
            return StringUtils.join(mods, ",") + ",";
        }
    }

    public static String formatMinAppVersion(int version) {
        if (version != 0) {
            return Integer.toString(version);
        } {
            return "";
        }
    }

    public static String formatMaxAppVersion(int version) {
        if (version > 0 && version != Integer.MAX_VALUE) {
            return Integer.toString(version);
        } else {
            return "";
        }
    }

    public static boolean versionIsActive(int status) {
        return (status & ServiceConsts.PV_STATUS_ACTIVE) != 0;
    }

    public static boolean isCheckResultError(Record recOrCheckResult) {
        return recOrCheckResult instanceof CheckResult
                && ((CheckResult) recOrCheckResult).error();
    }

    public static String objectToJson(Map<String, Object> o, boolean pretty) {
        return (o == null || o.isEmpty()) ? "{}" : JsonUtils.toJson(o, pretty);
    }

    public static String arrayToJson(List<Object> array, boolean pretty) {
        return (array == null || array.isEmpty()) ? "[]" : JsonUtils.toJson(array, pretty);
    }

    public static String recordsFieldJoin(Records rec, String field, String sep) {
        if (rec == null || field == null) {
            return "";
        } else {
            return rec.join(field, sep);
        }
    }

    public static String stringFormat1(String pattern, Object arg1) {
        return String.format(pattern, new Object[]{arg1});
    }

    public static String stringFormat2(String pattern, Object arg1, Object arg2) {
        return String.format(pattern, arg1, arg2);
    }

    public static String stringFormat3(String pattern, Object arg1, Object arg2, Object arg3) {
        return String.format(pattern, arg1, arg2, arg3);
    }

    public static String stringFormat4(String pattern, Object arg1, Object arg2, Object arg3, Object arg4) {
        return String.format(pattern, arg1, arg2, arg3, arg4);
    }


    public static String getStatMonthsLabel(PageContext pageCtx, String m) {
        m = StringUtils.trimToEmpty(m);
        ServiceContext ctx = Attributes.getServiceContext((HttpServletRequest) pageCtx.getRequest());
        if ("all".equalsIgnoreCase(m)) {
            return SpringMessage.get("publish_productStat.text.all", ctx.getClientLocale());
        } else {
            int mm = Integer.parseInt(m);
            String ptn = SpringMessage.get("publish_productStat.text.month", ctx.getClientLocale());
            return String.format(ptn, mm);
        }
    }

    public static String escapeHtml(String s) {
        return s != null ? StringEscapeUtils.escapeHtml(s) : "";
    }
}
