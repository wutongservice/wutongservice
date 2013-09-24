package com.borqs.server.base.util;



import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    public static String formatDateAndTime(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
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

    public static String formatDateCh(long millis) {
        return formatDateCh(new Date(millis));
    }

    //get tody 0 timestamp
    public static long getTimesmorning(){
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
