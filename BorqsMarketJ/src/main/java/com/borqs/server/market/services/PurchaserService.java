package com.borqs.server.market.services;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.VersionedProductId;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.Params;

public interface PurchaserService {

    public static final int PRICE_ALL = 0;
    public static final int PRICE_FREE = 1;
    public static final int PRICE_PAID = 2;


    Records listProducts(ServiceContext ctx,
                         String appId,
                         Params options) throws ServiceException;

    Record getProduct(ServiceContext ctx,
                      String appId,
                      Integer version) throws ServiceException;

    Record purchase(ServiceContext ctx,
                    String id,
                    int version,
                    Params options) throws ServiceException;
}
