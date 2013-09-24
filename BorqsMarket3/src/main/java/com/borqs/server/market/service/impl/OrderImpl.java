package com.borqs.server.market.service.impl;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.OrderService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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

    RecordsWithTotal getOrder0(RecordSession session, ServiceContext ctx, Params params, Paging paging) {

        String productName = params.param("product_name").asString();
        String version = params.param("product_version").asString();
        String orderStartDate = params.param("orderStartDate").asString();
        String orderEndDate = params.param("orderEndDate").asString();
        String orderMonth = StringUtils.trimToNull(params.param("orderMonth").asString());
        if (orderMonth != null) {
            orderMonth += "-01";
        }
        String productId = params.param("product_id").asString();

        RecordsWithTotal datas = session.selectListWithTotal("order.getOrder", CC.map(
                "product_name=>", productName,
                "product_version=>", version,
                "orderStartDate=>", orderStartDate,
                "orderEndDate=>", orderEndDate,
                "orderMonth=>", orderMonth,
                "author_id=>", ctx.getAccountId(),
                "product_id=>", productId,
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ), GenericMapper.get());

        accountService.fillDisplayName(session, ctx, datas.getRecords(), "purchaser_id", "purchaser_name");
        localeSelector.selectLocale(datas, ctx);
        return datas;
    }

    @Override
    public RecordsWithTotal getOrder(final ServiceContext ctx, final Params params, final Paging paging) {
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return getOrder0(session, ctx, params, paging);
            }
        });
    }
}
