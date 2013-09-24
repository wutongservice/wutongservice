package com.borqs.server.market.services;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.VersionedProductId;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

public interface PublisherService {
    Records listBorqsApps(ServiceContext ctx) throws ServiceException;

    Record getBorqsApp(ServiceContext ctx, String appId) throws ServiceException;

    Records listCategories(ServiceContext ctx, String appId, boolean withPricetags) throws ServiceException;

    Records listProducts(ServiceContext ctx, String appId, String category) throws ServiceException;

    Record getProduct(ServiceContext ctx, String productId) throws ServiceException;

    Record getVersion(ServiceContext ctx, String productId, int version) throws ServiceException;

    Record publishProduct(ServiceContext ctx, String appId, String category, String productId, String defaultLanguage) throws ServiceException;

    Record updateProduct(ServiceContext ctx, String productId, Params params) throws ServiceException;

    Record publishVersion(ServiceContext ctx, String productId, int version, Params params) throws ServiceException;

    Record updateVersion(ServiceContext ctx, String productId, int version, Params params) throws ServiceException;

    int activeVersion(ServiceContext ctx, String productId, Integer version, int status) throws ServiceException;
}
