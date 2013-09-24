package com.borqs.server.market.service;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;


public interface PurchaseService extends ServiceConsts {

    // browse product
    RecordsWithTotal listProducts(ServiceContext ctx, Params options) ;

    // get product
    Record getProduct(ServiceContext ctx, String productId, Integer version, String appMod) ;

    // purchase
    Record purchase(ServiceContext ctx, Params params) ;

    // get purchased
    RecordsWithTotal listPurchased(ServiceContext ctx, Params options);

    // list recommend partitions
    Records listPartitions(ServiceContext ctx, String appId, String categoryId);

    // list products in recommend partition
    RecordsWithTotal listPartitionProducts(ServiceContext ctx, String partitionId, Params options);

    // purchased
    Records hasPurchased(ServiceContext ctx, String[] productIds);

}
