package com.borqs.server.market.service.impl;

import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.InternalService;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.springframework.stereotype.Service;


/**
 * Created with IntelliJ IDEA.
 * User: wutong
 * Date: 9/13/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
@Service("service.internalDebug")
public class InternalImpl extends ServiceSupport implements InternalService{

    @Override
    public Record getProductPurchaseCount(final ServiceContext ctx,final  String product_id) {

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return null;
            }
        });
    }

    @Override
    public Records getProductPurchaseRecords(final ServiceContext ctx, final String product_id, final int start,final int end) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return null;
            }
        });
    }
}
