package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;


public interface CommentService extends ServiceConsts {
    Record updateComment(final ServiceContext ctx, String productId, int version, String message, double rating);
    RecordsWithTotal listComments(final ServiceContext ctx, String productId, Integer version, final Paging paging);
    Record getMyCommentForProduct(final ServiceContext ctx, String productId, Integer version);
}
