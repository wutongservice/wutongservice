package com.borqs.server.base.util;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static Date now() {
        return new Date(System.currentTimeMillis());
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static long nowNano() {
        return System.nanoTime();
    }

    public static String formatDateAndTime(long millis) {
        return formatDateAndTime(new Date(millis));
    }

    public static String formatDateMinute(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return f.format(date);
    }

    public static String formatDateMinute(long millis) {
        return formatDateMinute(new Date(millis));
    }

    public static String formatDateLocale(Date date) {
        String ww = String.format(Locale.ENGLISH, "%ta", date);
        String MM = String.format(Locale.ENGLISH, "%tb", date);
        String dd = String.format(Locale.ENGLISH, "%te", date);
        String hh = String.format(Locale.ENGLISH, "%tl", date);
        String mm = String.format(Locale.ENGLISH, "%tM", date);
        String ap = String.format(Locale.ENGLISH, "%tp", date);
        return ww + " " + MM + " " + dd + " " + hh + ":" + mm + ap;
    }

    public static String getMonth(long millis) {
        Date date = new Date(millis);
        return String.format(Locale.ENGLISH, "%tB", date);
    }

    public static String getDay(long millis) {
        Date date = new Date(millis);
        return String.format(Locale.ENGLISH, "%te", date);
    }

    public static String getWeekday(long millis) {
        Date date = new Date(millis);
        return String.format(Locale.ENGLISH, "%tA", date);
    }

    public static String getTime(long millis) {
        Date date = new Date(millis);
        String hh = String.format(Locale.ENGLISH, "%tl", date);
        String mm = String.format(Locale.ENGLISH, "%tM", date);
        String ap = String.format(Locale.ENGLISH, "%tp", date);
        return hh + ":" + mm + " " + ap;
    }

    public static String formatDateLocale(long millis) {
        return formatDateLocale(new Date(millis));
    }

    public static String formatDateAndTime(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return f.format(date);
    }

    public static String formatDateAndSecond(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return f.format(date);
    }

    public static String formatDate(long millis) {
        return formatDate(new Date(millis));
    }

    public static String formatDate(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        return f.format(date);
    }

    public static String formatDateCh(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy年MM月dd日");
        return f.format(date);
    }

    public static String formatDateAndTimeCh(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy年MM月dd日 HH点mm分ss秒");
        return f.format(date);
    }

    public static String formatDateCh(long millis) {
        return formatDateCh(new Date(millis));
    }

    public static String formatDateAndTimeCh(long millis) {
        return formatDateAndTimeCh(new Date(millis));
    }

    //get tody 0 timestamp
    public static long getTimesmorning() {
        return getTimestamp(0);
    }

    public static long getTimestamp(int hour) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
