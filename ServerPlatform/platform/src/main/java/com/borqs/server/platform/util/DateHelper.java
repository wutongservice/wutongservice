package com.borqs.server.platform.util;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateHelper {
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

    public static long secondsMillis(int second) {
        return 1000 * second;
    }

    public static long minutesMillis(int minutes) {
        return 1000 * 60 * minutes;
    }

    public static long hoursMillis(int hours) {
        return 1000 * 60 * 60 * hours;
    }

    public static long daysMillis(int days) {
        return 1000 * 60 * 60 * 24 * days;
    }

    public static long minutesSeconds(int minutes) {
        return 60 * minutes;
    }

    public static long hoursSeconds(int hours) {
        return 60 * 60 * hours;
    }

    public static long daysSeconds(int days) {
        return 60 * 60 * 24 * days;
    }

    public static String formatDuration(long duration) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format(new Date(duration - TimeZone.getDefault().getRawOffset()));
    }

}
