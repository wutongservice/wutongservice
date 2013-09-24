package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.DateRange;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface StatisticsService extends ServiceConsts {

    int OPT_FORMAT_COUNTRY_NAME = 1;

    String PURCHASE_COUNT = "count";
    String DOWNLOAD_COUNT = "download_count";
    void increaseCount(ServiceContext ctx, DateItem item, String countType, int n);

    Records dateBasedLineChart(ServiceContext ctx, String countType, DateRange range, Dimension dim, String[] conditions, int opts);

    Records donutChart(ServiceContext ctx, String countType, DateRange range, Dimension dim, String[] conditions, int opts);

    Records sumCounts(ServiceContext ctx, String[] countTypes, DateRange range, Dimension dim, String[] conditions);

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
        private Set<String> scopes;

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

        public String getAsColWithCol() {
            if (StringUtils.isEmpty(asCol)) {
                return col;
            } else {
                return asCol;
            }
        }

        public int getMax() {
            return max;
        }

        public Dimension setCol(String col) {
            this.col = col;
            return this;
        }

        public Dimension setAsCol(String asCol) {
            this.asCol = asCol;
            return this;
        }

        public Dimension setMax(int max) {
            this.max = max;
            return this;
        }

        public Set<String> getScopes() {
            return scopes;
        }

        public Dimension setScopes(Set<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Dimension in(Set<String> scopes) {
            return setScopes(scopes);
        }

        public Dimension in(List<String> scopes) {
            return setScopes(new HashSet<String>(scopes));
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
