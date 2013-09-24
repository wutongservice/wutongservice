package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.service.StatisticsService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/")
public class GstatController extends AbstractController {
    protected StatisticsService statisticsService;

    @Autowired
    @Qualifier("service.statisticsService")
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public GstatController() {
    }

    private void setupNavigationData(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        saveCurrentModule(req, resp, ServiceConsts.MODULE_PUBLISH);
    }

    @RequestMapping(value = "/gstat")
    public ModelAndView indexPage() {
        return redirect("/gstat/all");
    }


    @RequestMapping(value = "/gstat/all", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView allPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        setupNavigationData(ctx, req, resp);

        int months = params.getInt("months", 1);
        if(months<1)
            months = 1;
        String startDate = months > 0 ? DateTimeUtils.toDateString(DateUtils.addMonths(DateTimeUtils.nowDate(), -months)) : null;

        Records apps = statisticsService.getMaxApps(ctx, startDate);
        localeSelector.selectLocale(apps, ctx);
        List<String> appList = apps.asStringList("apps");

        Records rs = statisticsService.getAllAppsTotalCountByDate(ctx, startDate, appList, null);
        Records records = new Records();
        for (Record r : rs) {
            Record record = ContainValue(records, (String) r.get("dates"));
            record.put("dates", r.get("dates"));
            record.put((String) r.get("name"), r.get("count"));
        }

        Records rsPie = statisticsService.getAllAppsTotalCountByDatePieChart(ctx, startDate, appList, null);
        return new ModelAndView("gstat_all", CC.map(
                "months=>", months,
                "records=>", JsonUtils.toJson(records, true),
                "appList=>", JsonUtils.toJson(apps.asStringList("name"), true),
                "rsPie=>", JsonUtils.toJson(rsPie, true)));
    }

    private Record ContainValue(Records records, String value) {
        Record record = new Record();
        for (Record r : records) {
            if (r.containsValue(value)) {
                return r;
            }
        }
        records.add(record);
        return record;
    }
}
