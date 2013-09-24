package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.CommentService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("service.commentService")
public class CommentImpl extends ServiceSupport implements CommentService {
    protected AccountService accountService;

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public CommentImpl() {
    }

    RecordsWithTotal getComments0(RecordSession session, ServiceContext ctx, Params params, int count, int pages) {

        String account_id = params.getString("account_id", null);
        String version = params.getString("version", null);
        String product_id = params.getString("product_id", null);

        RecordsWithTotal datas = session.selectListWithTotal("comment.getComments", CC.map(
                "account_id=>", account_id,
                "version=>", version,
                "product_id=>", product_id,
                "count=>", count,
                "pages=>", pages
        ), RecordResultMapper.get());

        Map<String, String> map = accountService.getAccountDisplayNames(ctx, datas.getRecords().asStringArray("account_id"), "");
        for (Record rec : datas.getRecords()) {
            String authorName = MapUtils.getString(map, rec.asString("account_id"), "");
            rec.put("account_id", authorName);
        }
        localeSelector.selectLocale(datas, ctx);
        return datas;
    }

    @Override
    public RecordsWithTotal getComments(final ServiceContext ctx, final Params params, final int count, final int pages) {
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return getComments0(session, ctx, params, count, pages);
            }
        });
    }

    Integer createComment0(RecordSession session, ServiceContext ctx, Params params) {
        return session.insert("comment.createComment", params.getParams());

    }

    @Override
    public Integer createComment(final ServiceContext ctx, final Params params) {

        int i = openSession(new RecordSessionHandler<Integer>() {
            @Override
            public Integer handle(RecordSession session) throws Exception {
                return createComment0(session, ctx, params);
            }
        });
        // update product_version and products tables before create Comment
        updateCommentCount(ctx, params);

        return i;
    }

    @Override
    public Integer updateComment(final ServiceContext ctx, final Params params) {
        Record r = getComment(ctx, params);
        if (r == null || r.size() < 1)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "comment id is not exist");

        params.put("version", r.get("version"));
        params.put("product_id", r.get("product_id"));



        int i =  openSession(new RecordSessionHandler<Integer>() {
            @Override
            public Integer handle(RecordSession session) throws Exception {
                return session.update("comment.updateComment", params.getParams());
            }
        });

        // update product_version and products tables before modify Comment
        updateCommentCount(ctx, params);

        return i;
    }


    /**
     * change commentCount , rating , ratingCount of products
     *
     * @param ctx
     * @param params
     * @return
     */
    private Integer updateCommentCount(final ServiceContext ctx, final Params params) {

        //calculate the avg value of the rating
        Records rs = getAvgRating(ctx, params);

        Double rating_version = rs.get(0).get("rating_version") == null ? 0 : (Double) rs.get(0).get("rating_version");
        Double rating_product = rs.get(1).get("rating_version") == null ? 0 : (Double) rs.get(1).get("rating_version");

        params.put("rating_version", rating_version);
        params.put("rating_product", rating_product);

        return openSession(new RecordSessionHandler<Integer>() {
            @Override
            public Integer handle(RecordSession session) throws Exception {
                //update product_version table;
                int i = session.update("comment.updateCommentCountOfProductVersions", params.getParams());
                //update products table;
                return i + session.update("comment.updateCommentCountOfProducts", params.getParams());
            }
        });
    }


    /**
     * change commentCount , rating , ratingCount of product_versions
     *
     * @param ctx
     * @param params
     * @return
     */
    private Records getAvgRating(final ServiceContext ctx, final Params params) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return session.selectList("comment.getAvgRating", params.getParams());
            }
        });
    }

    /**
     * get comment Record by id
     *
     * @param ctx
     * @param params
     * @return
     */
    private Record getComment(final ServiceContext ctx, final Params params) {
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return session.selectOne("comment.getComment", params.getParams());
            }
        });
    }
}
