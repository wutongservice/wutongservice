package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.DateRange;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.List;

public interface StatisticsService extends ServiceConsts {

    int OPT_FORMAT_COUNTRY_NAME = 1;

    String PURCHASE_COUNT = "count";
    String DOWNLOAD_COUNT = "download_count";
    void increaseCount(ServiceContext ctx, DateItem item, String countType, int n);

    // get Statistics

//    Records getTotalCountByDate(ServiceContext ctx, String productId, String dates, String retainStr);
//
//    Records getTotalCountByVersion(ServiceContext ctx, String productId, String dates, String version, String retainStr);
//
//    Records getTotalCountByCountry(ServiceContext ctx, String productId, String dates, List<String> countries, String retainStr);
//
//    Records getMaxCountCountry(ServiceContext ctx, String productId, String dates, int limit, String retainStr);
//
//    Records getTotalCountByCountryPieChart(ServiceContext ctx, String productId, String dates, List<String> countries);

    Records getAllAppsTotalCountByDate(ServiceContext ctx, String dates, List<String> apps, String retainStr);

    Records getAllAppsTotalCountByDatePieChart(ServiceContext ctx, String dates, List<String> apps, String retainStr);

    Records getMaxApps(ServiceContext ctx, String dates);

    //product list statistics
    Records getTodayDownloads(ServiceContext ctx, List<String> product_ids, String dates);

    Records getAllDownloads(ServiceContext ctx, List<String> product_ids);



    //
    Records dateBasedLineChart(ServiceContext ctx, String countType, DateRange range, Dimension dim, String[] conditions, int opts);

    Records donutChart(ServiceContext ctx, String countType, DateRange range, Dimension dim, String[] conditions, int opts);

    static class DateItem {
        private String date;
        private String appId;
        private String categoryId;
        private String productId;
        private int version;
        private String country;

        private DateItem() {
        }

        public static DateItem onDate(String date) {
            DateItem item = new DateItem();
            item.date = date;
            return item;
        }

        public static DateItem onTime(long ms) {
            return onDate(DateTimeUtils.toDateString(ms));
        }

        public DateItem forProduct(String appId, String categoryId, String productId, int version) {
            this.appId = appId;
            this.categoryId = categoryId;
            this.productId = productId;
            this.version = version;
            return this;
        }

        public DateItem atCountry(String country) {
            this.country = country;
            return this;
        }

        public String getDate() {
            return date;
        }

        public String getAppId() {
            return appId;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public String getProductId() {
            return productId;
        }

        public int getVersion() {
            return version;
        }

        public String getCountry() {
            return country;
        }
    }

    static class Dimension {
        private String col;
        private String asCol;
        private int max;

        private Dimension(String col, String asCol, int max) {
            this.col = col;
            this.asCol = asCol;
            this.max = max;
        }

        public String getCol() {
            return col;
        }

        public String getAsCol() {
            return asCol;
        }

        public int getMax() {
            return max;
        }

        public static Dimension of(String col, String asCol, int max) {
            return new Dimension(col, asCol, max);
        }

        public static Dimension of(String col, int max) {
            return new Dimension(col, col, max);
        }

        public static Dimension of(String col, String asCol) {
            return new Dimension(col, asCol, 0);
        }

        public static Dimension of(String col) {
            return new Dimension(col, col, 0);
        }

        public static Dimension ofDummy(String colValue, String asCol) {
            return new Dimension("\'" + StringEscapeUtils.escapeSql(colValue) + "\'", asCol, 0);
        }

        public static Dimension ofDummy(String col) {
            return ofDummy(col, col);
        }
    }
}
