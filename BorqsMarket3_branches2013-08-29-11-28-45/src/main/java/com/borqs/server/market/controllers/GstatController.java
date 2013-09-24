package com.borqs.server.market.controllers;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.StatTypes;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.service.StatisticsService;
import com.borqs.server.market.utils.*;
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
import java.util.Map;

@Controller
@RequestMapping("/")
public class GstatController extends AbstractController {
    protected StatisticsService statisticsService;
    protected AccountService accountService;

    public GstatController() {
    }

    @Autowired
    @Qualifier("service.statisticsService")
    public void setStatisticsService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    private void setupNavigationData(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp) {
        saveCurrentModule(req, resp, ServiceConsts.MODULE_PUBLISH);
    }

    @RequestMapping(value = "/gstat")
    public ModelAndView indexPage() {
        return redirect("/gstat/all");
    }


    private void getCountStatModelsHelper(Map<String, Object> models, ServiceContext ctx, DateRange range, String countType) {
        Records totalPerDayLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.ofDummy("Downloads"), null, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByCountryLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.of("country", 8), null, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByCountryDonut = statisticsService.donutChart(ctx, countType, range,
                StatisticsService.Dimension.of("country", 8), null, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByVersionLine = statisticsService.dateBasedLineChart(ctx, countType, range,
                StatisticsService.Dimension.of("version"), null, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        Records totalPerDayByVersionDonut = statisticsService.donutChart(ctx, countType, range,
                StatisticsService.Dimension.of("version"), null, StatisticsService.OPT_FORMAT_COUNTRY_NAME);

        models.put("totalPerDayLine", totalPerDayLine);
        models.put("totalPerDayByCountryLine", totalPerDayByCountryLine);
        models.put("totalPerDayByCountryDonut", totalPerDayByCountryDonut);
        models.put("totalPerDayByVersionLine", totalPerDayByVersionLine);
        models.put("totalPerDayByVersionDonut", totalPerDayByVersionDonut);
    }

    private void getPurchaseCountStatModels(Map<String, Object> models, ServiceContext ctx, DateRange range) {
        getCountStatModelsHelper(models, ctx, range, StatisticsService.PURCHASE_COUNT);
    }


    private void getDownloadCountStatModels(Map<String, Object> models, ServiceContext ctx, DateRange range) {
        getCountStatModelsHelper(models, ctx, range, StatisticsService.DOWNLOAD_COUNT);
    }

    private void getAccountsCountModels(Map<String, Object> models, ServiceContext ctx) {
        long accountCount = accountService.getAccountsCount(ctx);
        models.put("accountsCount", accountCount);
    }

    @RequestMapping(value = "/gstat/all", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView allPage(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) {
        if (!ctx.hasAccountId())
            return redirect("/signin");

        setupNavigationData(ctx, req, resp);

        Records allStatTypes = StatTypes.allStatTypes(ctx);
        int months = getStatMonthsWithParams(params, "months", 1);
        String statType = params.param("stat_type").asString(StatTypes.STAT_PURCHASE_COUNT);

        DateRange range = DateRange.monthsAgo(months);
        Map<String, Object> models = CC.map(
                "current_stat_type=>", statType,
                "current_months=>", months <= 0 ? "all" : Integer.toString(months),
                "available_stat_types=>", allStatTypes
        );

        getAccountsCountModels(models, ctx);

        if (StatTypes.STAT_PURCHASE_COUNT.equalsIgnoreCase(statType)) {
            getPurchaseCountStatModels(models, ctx, range);
        } else if (StatTypes.STAT_DOWNLOAD_COUNT.equalsIgnoreCase(statType)) {
            getDownloadCountStatModels(models, ctx, range);
        } else {
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal stat_type");
        }

        return new ModelAndView("gstat_all", models);
    }
}
