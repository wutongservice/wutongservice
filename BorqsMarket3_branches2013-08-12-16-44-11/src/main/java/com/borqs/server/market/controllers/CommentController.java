package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.CommentService;
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
public class CommentController extends AbstractController {

    protected LocaleResolver localeResolver;
    protected CommentService commentService;

    public CommentController() {
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
    @Qualifier("service.commentService")
    public void setCommentService(CommentService commentService) {
        this.commentService = commentService;
    }

    private static final ParamsSchema createComment = new ParamsSchema()
            .required("product_id", "product_id: string not blank", notBlank())
            .required("message", "message: string not blank", notBlank());

    @RequestMapping(value = "/comment/create", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse createComment(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = createComment.validate(params);

        params.put("id", "C_" + RandomUtils2.randomLong());
        params.put("created_at", DateTimeUtils.nowMillis());
        params.put("updated_at", DateTimeUtils.nowMillis());

        String rating = params.getString("rating", "");
        if (StringUtils.isBlank(rating))
            rating = "5";
        params.put("rating", Double.parseDouble(rating));
        params.put("account_id", ctx.getAccountId());

        Record record = new Record();
        record.putAll(params.getParams());
        commentService.createComment(ctx, params);

        return APIResponse.of(record);
    }

    private static final ParamsSchema updateComment = new ParamsSchema()
            .required("id", "id: string not blank", notBlank());

    @RequestMapping(value = "/comment/update", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse updateComment(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {

        params = updateComment.validate(params);
        params.put("updated_at", DateTimeUtils.nowMillis());

        Record record = new Record();
        record.putAll(params.getParams());
        commentService.updateComment(ctx, params);

        return APIResponse.of(record);
    }

    @RequestMapping(value = "/comment/get", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse getComments(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        int page = params.getInt("page", 0);
        int count = params.getInt("count", 10);

        Records rs = commentService.getComments(ctx, params, page, count).getRecords();

        return APIResponse.of(rs);
    }
}
