package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;


public interface ShareService extends ServiceConsts {
    Integer createShare(final ServiceContext ctx, final Params params);

    RecordsWithTotal getShares(final ServiceContext ctx, final Params params, final int count, final int pages);

    Integer deleteShare(final ServiceContext ctx, final Params params);
}
