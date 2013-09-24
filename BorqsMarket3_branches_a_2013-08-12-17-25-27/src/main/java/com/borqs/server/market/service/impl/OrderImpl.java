package com.borqs.server.market.service.impl;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.OrderService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("service.orderService")
public class OrderImpl extends ServiceSupport implements OrderService {

    private AccountImpl accountService;

    public OrderImpl() {
    }

    public AccountImpl getAccountService() {
        return accountService;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountImpl accountService) {
        this.accountService = accountService;
    }

    RecordsWithTotal getOrder0(RecordSession session, ServiceContext ctx, Params params, int count, int pages) {

        String product_name = params.getString("product_name", null);
        String product_version = params.getString("product_version", null);
        String orderStartDate = params.getString("orderStartDate", null);
        String orderEndDate = params.getString("orderEndDate", null);
        String orderMonth = params.getString("orderMonth", null)==null?null:params.getString("orderMonth", null)+"-01";
        String product_id = params.getString("product_id", null);

        RecordsWithTotal datas = session.selectListWithTotal("order.getOrder", CC.map(
                "product_name=>", product_name,
                "product_version=>", product_version,
                "orderStartDate=>", orderStartDate,
                "orderEndDate=>", orderEndDate,
                "orderMonth=>", orderMonth,
                "author_id=>", ctx.getAccountId(),
                "product_id=>", product_id,
                "count=>", count,
                "pages=>", pages
        ), RecordResultMapper.get());

        accountService.fillDisplayName(session, ctx, datas.getRecords(), "purchaser_id", "purchaser_name");
        localeSelector.selectLocale(datas, ctx);
        return datas;
    }

    @Override
    public RecordsWithTotal getOrder(final ServiceContext ctx, final Params params, final int count, final int pages) {
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return getOrder0(session, ctx, params, count, pages);
            }
        });
    }
}
