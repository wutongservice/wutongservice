package com.borqs.server.market.service.impl;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.StatisticsService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.DateRange;
import com.borqs.server.market.utils.i18n.CountryNames;
import com.borqs.server.market.utils.i18n.SpringMessage;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("service.statisticsService")
public class StatisticsImpl extends ServiceSupport implements StatisticsService {
    public StatisticsImpl() {
    }

    void increaseCount(RecordSession session, ServiceContext ctx, DateItem item, String countType, int n) {
        if (n == 0)
            return;

        session.insert("statistics.increaseStatCount", CC.map(
                "app_id=>", ObjectUtils.toString(item.getAppId()),
                "category_id=>", ObjectUtils.toString(item.getCategoryId()),
                "product_id=>", ObjectUtils.toString(item.getProductId()),
                "version=>", item.getVersion(),
                "country=>", ObjectUtils.toString(item.getCountry()),
                "date=>", ObjectUtils.toString(item.getDate()),
                "count_type=>", ObjectUtils.toString(countType),
                "n=>", n
        ));
    }

    @Override
    public void increaseCount(final ServiceContext ctx, final DateItem item, final String countType, final int n) {
        Validate.notNull(item);
        Validate.notNull(countType);

        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                increaseCount(session, ctx, item, countType, n);
                return null;
            }
        });

    }

    Records getStatistics(RecordSession session, ServiceContext ctx, String productId, String version, String country, String dates, String retainStr) {
        Records datas = session.selectList("statistics.getStatistics", CC.map(
                "product_id=>", productId,
                "version=>", version,
                "country=>", country,
                "dates=>", dates

        ));

        return datas.retain(retainStr);
    }


//    Records getTotalCountByDate0(RecordSession session, ServiceContext ctx, String productId, String dates, String retainStr) {
//        Records datas = session.selectList("statistics.getTotalCountByDate", CC.map(
//                "product_id=>", productId,
//                "dates=>", dates
//        ));
//
//        return datas.retain(retainStr);
//    }
//
//    @Override
//    public Records getTotalCountByDate(final ServiceContext ctx, final String productId, final String dates, final String retainStr) {
//        return openSession(new RecordSessionHandler<Records>() {
//            @Override
//            public Records handle(RecordSession session) throws Exception {
//                return getTotalCountByDate0(session, ctx, productId, dates, retainStr);
//            }
//        });
//    }
//
//    Records getTotalCountByVersion0(RecordSession session, ServiceContext ctx, String productId, String dates, String version, String retainStr) {
//        Records datas = session.selectList("statistics.getTotalCountByVersion", CC.map(
//                "product_id=>", productId,
//                "dates=>", dates,
//                "version=>", version
//        ));
//
//        return datas.retain(retainStr);
//    }
//
//    @Override
//    public Records getTotalCountByVersion(final ServiceContext ctx, final String productId, final String dates, final String version, final String retainStr) {
//        return openSession(new RecordSessionHandler<Records>() {
//            @Override
//            public Records handle(RecordSession session) throws Exception {
//                return getTotalCountByVersion0(session, ctx, productId, dates, version, retainStr);
//            }
//        });
//    }
//
//    Records getTotalCountByCountry0(RecordSession session, ServiceContext ctx, String productId, String dates, List<String> countries, String retainStr) {
//        Records datas = session.selectList("statistics.getTotalCountByCountry", CC.map(
//                "product_id=>", productId,
//                "dates=>", dates,
//                "countries=>", countries
//        ));
//
//        for (Record r : datas) {
//            String country = CountryNames.get((String) r.get("country"), ctx.getClientLocale());
//            if (StringUtils.isNotBlank(country))
//                r.put("country", country);
//        }
//        return datas.retain(retainStr);
//    }
//
//    @Override
//    public Records getTotalCountByCountry(final ServiceContext ctx, final String productId, final String dates, final List<String> countries, final String retainStr) {
//        return openSession(new RecordSessionHandler<Records>() {
//            @Override
//            public Records handle(RecordSession session) throws Exception {
//                return getTotalCountByCountry0(session, ctx, productId, dates, countries, retainStr);
//            }
//        });
//    }
//
//    Records getMaxCountCountry0(RecordSession session, ServiceContext ctx, String productId, String dates, int limit, String retainStr) {
//        Records datas = session.selectList("statistics.getMaxCountCountry", CC.map(
//                "product_id=>", productId,
//                "dates=>", dates,
//                "limit=>", limit
//        ));
//
//        return datas.retain(retainStr);
//    }
//
//    @Override
//    public Records getMaxCountCountry(final ServiceContext ctx, final String productId, final String dates, final int limit, final String retainStr) {
//        return openSession(new RecordSessionHandler<Records>() {
//            @Override
//            public Records handle(RecordSession session) throws Exception {
//                return getMaxCountCountry0(session, ctx, productId, dates, limit, retainStr);
//            }
//        });
//    }
//
//    Records getTotalCountByCountryPieChart0(RecordSession session, ServiceContext ctx, String productId, String dates, final List<String> countries) {
//        Records datas = session.selectList("statistics.getTotalCountByCountryPieChart", CC.map(
//                "product_id=>", productId,
//                "dates=>", dates,
//                "other=>", SpringMessage.get("publish_productStat.text.other", ctx),
//                "countries=>", countries
//        ));
//        for (Record r : datas) {
//            String country = CountryNames.get((String) r.get("label"), ctx.getClientLocale());
//            if (StringUtils.isNotBlank(country))
//                r.put("label", country);
//        }
//        return datas;
//    }
//
//    @Override
//    public Records getTotalCountByCountryPieChart(final ServiceContext ctx, final String productId, final String dates, final List<String> countries) {
//        return openSession(new RecordSessionHandler<Records>() {
//            @Override
//            public Records handle(RecordSession session) throws Exception {
//                return getTotalCountByCountryPieChart0(session, ctx, productId, dates, countries);
//            }
//        });
//    }

    //TODO boss statistics
    public Records getMaxApps(RecordSession session, ServiceContext ctx, String dates) {
        Records datas = session.selectList("statistics.getMaxApps", CC.map(
                "dates=>", dates
        ), RecordResultMapper.get());

        return datas;
    }

    @Override
    public Records getMaxApps(final ServiceContext ctx, final String dates) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getMaxApps(session, ctx, dates);
            }
        });
    }

    Records getAllAppsTotalCountByDate(RecordSession session, ServiceContext ctx, String dates, List<String> apps, String retainStr) {
        Records datas = session.selectList("statistics.getAllAppsTotalCountByDate", CC.map(
                "dates=>", dates,
                "apps=>", apps
        ), RecordResultMapper.get());
        localeSelector.selectLocale(datas, ctx);
        return datas;
    }

    @Override
    public Records getAllAppsTotalCountByDate(final ServiceContext ctx, final String dates, final List<String> apps, final String retainStr) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getAllAppsTotalCountByDate(session, ctx, dates, apps, retainStr);
            }
        });

    }

    Records getAllAppsTotalCountByDatePieChart(RecordSession session, ServiceContext ctx, String dates, List<String> apps, String retainStr) {
        Records datas = session.selectList("statistics.getAllAppsTotalCountByDatePieChart", CC.map(
                "dates=>", dates,
                "apps=>", apps
        ), RecordResultMapper.get());
        localeSelector.selectLocale(datas, ctx);
        datas.renameField("name", "label");
        return datas;
    }

    @Override
    public Records getAllAppsTotalCountByDatePieChart(final ServiceContext ctx, final String dates, final List<String> apps, final String retainStr) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getAllAppsTotalCountByDatePieChart(session, ctx, dates, apps, retainStr);
            }
        });
    }

    Records getTodayDownloads(RecordSession session, ServiceContext ctx, String dates, List<String> product_ids) {
        Records datas = session.selectList("statistics.getTodayDownloads", CC.map(
                "dates=>", dates,
                "product_ids=>", product_ids
        ), RecordResultMapper.get());
        localeSelector.selectLocale(datas, ctx);
        datas.renameField("name", "label");
        return datas;
    }

    @Override
    public Records getTodayDownloads(final ServiceContext ctx, final List<String> product_ids, final String dates) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getTodayDownloads(session, ctx, dates, product_ids);
            }
        });
    }


    Records getAllDownloads(RecordSession session, ServiceContext ctx, List<String> product_ids) {
        Records datas = session.selectList("statistics.getAllDownloads", CC.map(
                "product_ids=>", product_ids
        ), RecordResultMapper.get());

        localeSelector.selectLocale(datas, ctx);
        datas.renameField("name", "label");
        return datas;
    }

    @Override
    public Records getAllDownloads(final ServiceContext ctx, final List<String> product_ids) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getAllDownloads(session, ctx, product_ids);
            }
        });
    }


    private Record ensureDateItem(Records statResult, String value) {
        for (Record r : statResult) {
            if (StringUtils.equals(r.asString("dates"), value))
                return r;
        }
        Record item = new Record().set("dates", value);
        statResult.add(item);
        return item;
    }

    static Records displayForLineChart(Records recs, String locale) {
        if (recs != null) {
            for (Record rec : recs) {
                ArrayList<String> ccl = new ArrayList<String>(rec.keySet());
                for (String cc : ccl) {
                    String cn = CountryNames.get(cc, locale);
                    if (StringUtils.isNotEmpty(cn))
                        rec.renameField(cc, cn);
                }
            }
        }
        return recs;
    }

    Records dateBasedLineChart(RecordSession session,
                               ServiceContext ctx,
                               String countType,
                               DateRange range,
                               Dimension dim,
                               String[] conditions,
                               int opts) {
        List<String> conds = ArrayUtils.isNotEmpty(conditions) ? Arrays.asList(conditions) : new ArrayList<String>();

        Map<String, Object> params = CC.map(
                "dim_col=>", dim.getCol(),
                "dim_as_col=>", dim.getAsCol(),
                "count_type=>", countType,
                "min_date=>", range.getMin(),
                "max_date=>", range.getMax(),
                "conditions=>", conds
        );

        List<String> dimInCols = null;
        if (dim.getMax() > 0) {
            Records tops = session.selectList("getTopForDateBasedLineChart",
                    CC.map(new LinkedHashMap<String, Object>(params), "n=>", dim.getMax()));
            dimInCols = tops.asStringList(dim.getAsCol());
        }

        if (dim.getMax() > 0 && CollectionUtils.isEmpty(dimInCols)) {
            return new Records();
        }

        Records rawData = session.selectList("getDataForDateBasedLineChart",
                CC.map(new LinkedHashMap<String, Object>(params), "dim_in_cols=>", dimInCols));

        Records statResult = new Records();
        for (Record rec : rawData) {
            Record record = ensureDateItem(statResult, rec.asString("dates"));
            record.put(rec.asString(dim.getAsCol()), rec.asLong(countType));
        }

        if ((opts & OPT_FORMAT_COUNTRY_NAME) != 0) {
            displayForLineChart(statResult, ctx.getClientLocale());
        }
        return statResult;
    }

    @Override
    public Records dateBasedLineChart(final ServiceContext ctx, final String countType, final DateRange range, final Dimension dim, final String[] conditions, final int opts) {
        Validate.notNull(ctx);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return dateBasedLineChart(session, ctx, countType, range, dim, conditions, opts);
            }
        });
    }


    static Records displayForDonutChart(Records recs, String locale) {
        if (recs != null) {
            for (Record rec : recs) {
                String cc = rec.asString("label");
                String cn = CountryNames.get(cc, locale);
                if (StringUtils.isNotEmpty(cn))
                    rec.put("label", cn);
            }
        }
        return recs;
    }

    Records donutChart(RecordSession session,
                       ServiceContext ctx,
                       String countType,
                       DateRange range,
                       Dimension dim,
                       String[] conditions,
                       int opts) {
        List<String> conds = ArrayUtils.isNotEmpty(conditions) ? Arrays.asList(conditions) : new ArrayList<String>();


        Map<String, Object> params = CC.map(
                "dim_col=>", dim.getCol(),
                "dim_as_col=>", dim.getAsCol(),
                "count_type=>", countType,
                "min_date=>", range.getMin(),
                "max_date=>", range.getMax(),
                "conditions=>", conds,
                "other_label=>", SpringMessage.get("publish_productStat.text.other", ctx)
        );

        List<String> dimInCols = null;
        if (dim.getMax() > 0) {
            Records tops = session.selectList("getTopForDateBasedLineChart",
                    CC.map(new LinkedHashMap<String, Object>(params), "n=>", dim.getMax()));
            dimInCols = tops.asStringList(dim.getAsCol());
        }

        if (dim.getMax() > 0 && CollectionUtils.isEmpty(dimInCols)) {
            return new Records();
        }

        Records statResult = session.selectList("getDataForDonutChart",
                CC.map(new LinkedHashMap<String, Object>(params), "dim_in_cols=>", dimInCols));

        if ((opts & OPT_FORMAT_COUNTRY_NAME) != 0) {
            displayForDonutChart(statResult, ctx.getClientLocale());
        }

        return statResult;
    }

    @Override
    public Records donutChart(final ServiceContext ctx, final String countType, final DateRange range, final Dimension dim, final String[] conditions, final int opts) {
        Validate.notNull(ctx);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return donutChart(session, ctx, countType, range, dim, conditions, opts);
            }
        });
    }
}
