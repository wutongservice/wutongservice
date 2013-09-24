package com.borqs.server.market.utils;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import java.util.Date;

public class DateRange {
    private final String min;
    private final String max;

    public DateRange(String min, String max) {
        this.min = min;
        this.max = max;
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }

    public static DateRange monthsAgo(int months) {
        Date d = DateUtils.addMonths(DateTimeUtils.nowDate(), -months);
        return new DateRange(DateTimeUtils.toDateString(d), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DateRange dateRange = (DateRange) o;

        return StringUtils.equals(min, dateRange.min)
                && StringUtils.equals(max, dateRange.max);
    }

    @Override
    public int hashCode() {
        return ObjectUtils2.hashCodeMulti(min, max);
    }

    @Override
    public String toString() {
        return ObjectUtils.toString(min) + " to " + ObjectUtils.toString(max);
    }
}

