package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.StatisticsService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/")
public class StatisticsController extends AbstractController {
    protected AccountService accountService;
    protected LocaleResolver localeResolver;
    protected StatisticsService statisticsService;

    public StatisticsController() {
    }

    public AccountService getAccountService() {
        return accountService;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public static ModelAndView redirect(String to) {
        return new ModelAndView("redirect:" + to);
    }

    @Autowired
    @Qualifier("service.statisticsService")
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }


    /*@RequestMapping(value = "/testIp", method = RequestMethod.GET)
    public void test(ServiceContext ctx, Params params) {
        params.put("app_id", "1");
        params.put("category_id", "2");
        params.put("product_id", "3");
        params.put("version", "4");
        params.put("country", "5");
        params.put("dates", "6");
        params.put("count", "7");

        statisticsService.saveStatistics(ctx, params);
    }*/

    @RequestMapping(value = "/getStatistic", method = RequestMethod.GET)
    public ModelAndView getStatistic(ServiceContext ctx, Params params) throws ParseException {
//        params.put("product_id", "com.borqs.se.theme.wood");
//        params.put("version", "3");
//        params.put("country", "AU");
//        params.put("dates", "6");
//
//        int range = -1;
//
//        range = getRangeDay(params);
//
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        Date current = new Date();
//
//        String start_date = null;
//        if (range != 0) {
//            Date date = DateUtils.addMonths(current, range);
//            start_date = dateFormat.format(date);
//        }
//
//        Records productsPerDate = statisticsService.getTotalCountByDate(ctx, (String) params.get("product_id"), start_date, "dates,count");
//        Records countryMax = statisticsService.getMaxCountCountry(ctx, (String) params.get("product_id"), start_date, 5, "country");
//        List<String> maxCounties = countryMax.asStringList("country");
//        Records countryProductsPerDate = statisticsService.getTotalCountByCountry(ctx, (String) params.get("product_id"), start_date, maxCounties, "dates,count,country");
//
//        Records records = new Records();
//        for (Record r : countryProductsPerDate) {
//            Record record = ContainValue(records,(String) r.get("dates"));
//
//            record.put("dates", r.get("dates"));
//            record.put((String) r.get("country"), r.get("count"));
//        }
//        Records versionProductsPerDate = statisticsService.getTotalCountByVersion(ctx, (String) params.get("product_id"), start_date, (String) params.get("version"), "dates,count,version");
//        Set versions = new HashSet();
//        //
//        Records recordVersion = new Records();
//        for (Record r : versionProductsPerDate) {
//            Record record = ContainValue(recordVersion, (String) r.get("dates"));
//            versions.add(r.get("version"));
//            record.put("dates", r.get("dates"));
//            record.put((String) r.get("version"), r.get("count"));
//        }
//
//        return new ModelAndView("statistics",
//                CC.map("purchaseCountPerdate=>", JsonUtils.toJson(productsPerDate, true),
//                        "countryProductsPerDate=>", JsonUtils.toJson(records, true),
//                        "maxCounties=>", "'" + countryMax.join("country", "','") + "'",
//                        "versions=>", JsonUtils.toJson(versions, true),
//                        "versionProductsPerDate=>", JsonUtils.toJson(recordVersion, true)));
        return null;
    }

    private Record ContainValue(Records records,String value){
        Record record = new Record();
        for(Record r: records){
            if(r.containsValue(value)){
                return r;
            }
        }
        records.add(record);
        return record;
    }
    private int getRangeDay(Params params) {
        int range;
        String d = (String) params.get("dates");

        if (StringUtils.equals(d, "1")) {
            range = -1;
        } else if (StringUtils.equals(d, "3")) {
            range = -3;
        } else if (StringUtils.equals(d, "6")) {
            range = -6;
        } else if (StringUtils.equals(d, "12")) {
            range = -12;
        } else {
            range = 0;
        }
        return range;
    }

    /*@RequestMapping(value = "/test333", method = RequestMethod.GET)
    public ModelAndView getStatistic3(ServiceContext ctx, Params params) throws ParseException {
        long now = DateTimeUtils.nowMillis();
        Params statisticParam = new Params();
        statisticParam.put("product_id", "product_id");
        statisticParam.put("category_id", "category_id");
        statisticParam.put("app_id", "app_id");
        statisticParam.put("version", "version");
        //statisticParam.put("ip", "125.92.141.237");
        //statisticParam.put("ip", "1.52.215.102");
        statisticParam.put("ip", "210.193.13.215");
        statisticParam.put("count", "1");
        String date = DateTimeUtils.format(now, "yyyy-MM-dd");
        statisticParam.put("dates", date);

        statisticsService.saveStatistics(ctx, statisticParam);
        return null;
    }*/

}
