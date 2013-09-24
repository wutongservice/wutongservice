package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.ShareService;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.RandomUtils2;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

import static com.borqs.server.market.utils.validation.Predicates.notBlank;

@Controller
@RequestMapping("/")
public class ShareController extends AbstractController {

    protected LocaleResolver localeResolver;
    protected ShareService shareService;

    public ShareController() {
    }

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }


    @Autowired
    @Qualifier("service.shareService")
    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    private static final ParamsSchema createShare = new ParamsSchema()
            .required("category_id", "category_id: string not blank", notBlank())
            .required("app_id", "app_id: string not blank", notBlank());

    @RequestMapping(value = "/share/create", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse createShare(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
//        params.put("app_id", "app1");
//        params.put("category_id", "category1");

        params = createShare.validate(params);

        params.put("id", "S_" + RandomUtils2.randomLong());
        params.put("created_at", DateTimeUtils.nowMillis());
        params.put("updated_at", DateTimeUtils.nowMillis());

        String rating = params.getString("rating", "");
        if (StringUtils.isBlank(rating))
            rating = "5";
        params.put("rating", Double.parseDouble(rating));
        params.put("author_id", ctx.getAccountId()==null?"":ctx.getAccountId());
        params.put("author_name", ctx.getAccountName());
        params.put("author_email", ctx.getAccountEmail());

        Record record = new Record();
        record.putAll(params.getParams());
        shareService.createShare(ctx, params);

        return APIResponse.of(record);
    }

    private static final ParamsSchema deleteShare = new ParamsSchema()
            .required("id", "id: string not blank", notBlank());

    @RequestMapping(value = "/share/delete", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse deleteShare(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {

        params = deleteShare.validate(params);

        shareService.deleteShare(ctx, params);
        return APIResponse.of(true);
    }

    @RequestMapping(value = "/share/get", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse getShares(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        int page = params.getInt("page", 0);
        int count = params.getInt("count", 10);

        Records rs = shareService.getShares(ctx, params, page, count).getRecords();

        return APIResponse.of(rs);
    }
}
