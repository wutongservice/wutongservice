package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;


public interface OrderService extends ServiceConsts {
    RecordsWithTotal getOrder(final ServiceContext ctx, final Params params, final Paging paging);
}
