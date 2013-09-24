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

    private static List<String> trimConditions(String[] conds) {
        ArrayList<String> l = new ArrayList<String>();
        if (ArrayUtils.isNotEmpty(conds)) {
            Collections.addAll(l, conds);
        }
        return l;
    }

    Records dateBasedLineChart(RecordSession session,
                               ServiceContext ctx,
                               String countType,
                               DateRange range,
                               Dimension dim,
                               String[] conditions,
                               int opts) {

        Map<String, Object> params = CC.map(
                "dim_col=>", dim.getCol(),
                "dim_as_col=>", dim.getAsCol(),
                "count_type=>", countType,
                "min_date=>", range.getMin(),
                "max_date=>", range.getMax(),
                "conditions=>", trimConditions(conditions)
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
        Map<String, Object> params = CC.map(
                "dim_col=>", dim.getCol(),
                "dim_as_col=>", dim.getAsCol(),
                "count_type=>", countType,
                "min_date=>", range.getMin(),
                "max_date=>", range.getMax(),
                "conditions=>", trimConditions(conditions),
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

    Records sumCounts(RecordSession session, ServiceContext ctx, String[] countTypes, DateRange range, Dimension dim, String[] conditions) {
        if (ArrayUtils.isEmpty(countTypes)) {
            return new Records();
        }
        if (range == null) {
            range = DateRange.allDates();
        }

        Records sums = session.selectList("sumCounts", CC.map(
                "dim_col=>", dim.getCol(),
                "dim_as_col=>", dim.getAsColWithCol(),
                "min_date=>", range.getMin(),
                "max_date=>", range.getMax(),
                "conditions=>", trimConditions(conditions),
                "dim_scope=>", dim.getScopes(),
                "count_cols=>", Arrays.asList(countTypes)
        ));

        return sums;
    }


        @Override
    public Records sumCounts(final ServiceContext ctx, final String[] countTypes, final DateRange range, final Dimension dim, final String[] conditions) {
        Validate.notNull(ctx);
        Validate.notNull(dim);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return sumCounts(session, ctx, countTypes, range, dim, conditions);
            }
        });
    }
}
