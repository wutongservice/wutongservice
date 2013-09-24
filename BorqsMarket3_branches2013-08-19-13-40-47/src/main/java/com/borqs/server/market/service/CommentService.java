package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;


public interface CommentService extends ServiceConsts {
    Integer createComment(final ServiceContext ctx,final Params params);
    RecordsWithTotal getComments(final ServiceContext ctx, final Params params, final int count, final int pages);
    Integer updateComment(final ServiceContext ctx,final Params params);
}
