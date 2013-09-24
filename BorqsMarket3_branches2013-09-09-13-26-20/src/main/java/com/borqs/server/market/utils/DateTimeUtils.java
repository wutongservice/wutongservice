package com.borqs.server.market.utils;


import org.apache.commons.lang.LocaleUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    public static Date nowDate() {
        return new Date(nowMillis());
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static String format(long ms, String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(new Date(ms));
    }

    public static String toLongString(long ms) {
        return format(ms, "yyyy-MM-dd HH:mm:ss.SSS");
    }

    public static String toDateString(long ms) {
        return format(ms, "yyyy-MM-dd");
    }

    public static String toDateString(Date date) {
        return toDateString(date.getTime());
    }

    public static long dateMillis(String date) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date d = df.parse(date);
            return d.getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Illegal date format (yyyy-MM-dd)");
        }
    }

    public static String formatDateTimeWithLocale(long ms, String locale) {
        DateFormat df;
        try {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, LocaleUtils.toLocale(locale));
        } catch (Exception ignored) {
            df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
        }
        return df.format(new Date(ms));
    }

    public static String formatDateWithLocale(long ms, String locale) {
        DateFormat df;
        try {
            df = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtils.toLocale(locale));
        } catch (Exception ignored) {
            df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
        }
        return df.format(new Date(ms));
    }
}
