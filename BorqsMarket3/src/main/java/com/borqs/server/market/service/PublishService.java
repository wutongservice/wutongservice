package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.resfile.ResourceFile;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

import java.util.Map;

public interface PublishService extends ServiceConsts {

    Records listApps(ServiceContext ctx) ;

    Record getApp(ServiceContext ctx, String id) ;

    Records listCategories(ServiceContext ctx, String appId, boolean withPricetags) ;

    Record getCategory(ServiceContext ctx, String appId, String categoryId, boolean withPricetags) ;

    Records listPricetags(ServiceContext ctx, String appId, String categoryId) ;


    Record createProduct(ServiceContext ctx, Record product) ;

    Record updateProduct(ServiceContext ctx, Record product) ;

    void deleteProduct(ServiceContext ctx, String id) ;

    Record getProduct(ServiceContext ctx, String id, boolean withVersions, boolean withAvailablePricetags) ;

    Records listProducts(ServiceContext ctx, String appId, String categoryId, boolean withVersions, boolean withAvailablePricetags) ;


    Record createVersion(ServiceContext ctx, Record versionRec) ;

    Record updateVersion(ServiceContext ctx, Record versionRec) ;

    void deleteVersion(ServiceContext ctx, String id, int version) ;

    Record getVersion(ServiceContext ctx, String id, int version) ;

    Records listVersions(ServiceContext ctx, String id) ;


    Record activeVersion(ServiceContext ctx, String id, Integer version, boolean active) ;

    void releaseVersion(ServiceContext ctx, String id, int version);

    Record uploadProduct(ServiceContext ctx, ResourceFile productFile, boolean createProduct, Params params) ;

    CheckResult checkProductForUpload(ServiceContext ctx, ResourceFile productFile, boolean forCreateProduct, Params params);

    CheckResult checkProductForUpdate(ServiceContext ctx, String id, Params params, Record current);

    CheckResult checkVersionForUpdate(ServiceContext ctx, String id, int version, Params params, Record current);
}
