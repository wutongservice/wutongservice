package com.borqs.server.platform.util;


public class TimeRange {
    public final long min;
    public final long max;

    public TimeRange(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public static TimeRange all() {
        return new TimeRange(0, Long.MAX_VALUE);
    }

    public static TimeRange of(long min, long max) {
        return new TimeRange(min, max);
    }

    public static TimeRange before(long max) {
        return new TimeRange(0, max);
    }

    public static TimeRange after(long min) {
        return new TimeRange(min, Long.MAX_VALUE);
    }

    @Override
    public String toString() {
        return String.format("[%s...%s]", min, max);
    }
}
