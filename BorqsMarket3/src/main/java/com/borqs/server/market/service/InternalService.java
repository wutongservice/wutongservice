package com.borqs.server.market.service;

import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

/**
 * Created with IntelliJ IDEA.
 * User: wutong
 * Date: 9/13/13
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 * it is for show internal debug information
 */
public interface  InternalService extends ServiceConsts{
    //get product purchase total count
    Record getProductPurchaseCount(final ServiceContext ctx,final  String product_id);
    Records getProductPurchaseRecords(final ServiceContext ctx,final String product_id, final int start, final  int end);
}
