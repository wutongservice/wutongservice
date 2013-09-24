package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.AppMods;
import com.borqs.server.market.service.PurchaseService;
import com.borqs.server.market.service.StatisticsService;
import com.borqs.server.market.service.impl.partitions.PartitionRule;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.borqs.server.market.Errors.*;

@Service("service.defaultPurchaseService")
public class PurchaseImpl extends ServiceSupport implements PurchaseService {

    public PurchaseImpl() {
    }

    protected StatisticsImpl statisticsService;

    @Autowired
    @Qualifier("service.statisticsService")
    public void setStatisticsService(StatisticsImpl statisticsService) {
        this.statisticsService = statisticsService;
    }

    private void attachPurchaseInfo(RecordSession session, ServiceContext ctx, List<Record> versionedProducts) {
        for (Record versionedProduct : versionedProducts)
            attachPurchaseInfo(session, ctx, versionedProduct);
    }

    private void attachPurchaseInfo(RecordSession session, ServiceContext ctx, Record versionedProduct) {
        if (versionedProduct.asInt("paid") == PT_PRICE_FREE) {
            versionedProduct.set("purchasable", ctx.hasClientDeviceId());
        } else {
            versionedProduct.set("purchasable", ctx.hasAccountId());
        }

        if (ctx.hasAccountId()) {
            String orderId = session.selectStringValue("market.findOrderIdForPurchase", CC.map(
                    "id=>", versionedProduct.asString("id"),
                    "purchaser_id=>", ctx.getAccountId(),
                    //"device_id=>", ctx.getClientDeviceId()
                    "device_id=>", null
            ), null);
            versionedProduct.set("purchased", orderId != null);
        } else {
            versionedProduct.set("purchased", false);
        }
    }

    RecordsWithTotal listProducts(RecordSession session, ServiceContext ctx, Params options) {
        String appId = options.param("app_id").asString();
        String categoryId = options.param("category_id").asString();
        int appVersion = options.param("app_version").asInt(0);
        String mod = options.param("app_mod").asString();
        if (StringUtils.isEmpty(mod)) {
            mod = AppMods.getDefaultAppModForPurchase(appId, categoryId);
        }
        int price = options.param("paid").asInt(PT_PRICE_ALL);
        String tag = StringUtils.trimToNull(options.param("tag").asString());
        String orderBy = options.param("order_by").asString();
        Paging paging = options.getPaging(50);

        RecordsWithTotal versionedProductIds = session.selectListWithTotal("market.listVersionedProductIdsForPurchase", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId,
                "locale=>", ctx.getClientLocale(),
                "app_version=>", appVersion,
                "app_mod=>", mod,
                "paid=>", price,
                "order_by=>", orderBy,
                "tag=>", tag,
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ));

        Records versionedProducts = new Records();
        for (Record versionedProductId : versionedProductIds.getRecords()) {
            Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                    "id=>", versionedProductId.asString("id"),
                    "version=>", versionedProductId.asInt("version")
            ), RecordResultMapper.get());
            versionedProducts.addUnlessNull(versionedProduct);
        }

        localeSelector.selectLocale(versionedProducts, ctx);
        urlCompleter.completeUrl(versionedProducts);
        attachPurchaseInfo(session, ctx, versionedProducts);

        return new RecordsWithTotal(versionedProducts, versionedProductIds.getTotal());
    }

    @Override
    public RecordsWithTotal listProducts(final ServiceContext ctx, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(options);

        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listProducts(session, ctx, options);
            }
        });
    }

    Record getProduct(RecordSession session, ServiceContext ctx, String productId, Integer version, String appMod) {
        int ver0;
        if (version == null || version == Integer.MIN_VALUE) {
            if (StringUtils.isEmpty(appMod)) {
                Record appIdWitCategoryId = session.selectOne("market.getAppIdAndCategoryIdByProductIdForPurchase",
                        CC.map("id=>", productId));
                if (appIdWitCategoryId != null) {
                    appMod = AppMods.getDefaultAppModForPurchase(appIdWitCategoryId.asString("app_id"),
                            appIdWitCategoryId.asString("category_id"));
                }
            }
            ver0 = session.selectIntValue("market.findProductMaxVersionForPurchase", CC.map(
                    "product_id=>", productId,
                    "app_mod=>", appMod
            ), Integer.MIN_VALUE);
        } else {
            ver0 = version;
        }
        Record versionedProduct = session.selectOne("market.getDetailedVersionedProductForPurchase", CC.map(
                "id=>", productId,
                "version=>", ver0
        ), RecordResultMapper.get());

        if (versionedProduct != null) {
            localeSelector.selectLocale(versionedProduct, ctx.getClientLocale());
            urlCompleter.completeUrl(versionedProduct);
            attachPurchaseInfo(session, ctx, Arrays.asList(versionedProduct));
        }
        return versionedProduct;
    }

    @Override
    public Record getProduct(final ServiceContext ctx, final String productId, final Integer version, final String appMod) {
        Validate.notNull(ctx);
        Validate.notNull(productId);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getProduct(session, ctx, productId, version, appMod);
            }
        });
    }

    Record purchase(RecordSession session, ServiceContext ctx, Params params) {
        if (!ctx.hasAccountId() && !ctx.hasClientDeviceId())
            throw new ServiceException(Errors.E_PERMISSION, "Missing account_id and device_id");

        String purchaserId = ctx.getAccountId();

        // check product with version
        long now = DateTimeUtils.nowMillis();
        String productId = params.param("id").asString("");
        int version = params.param("version").asInt();

        Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase2", CC.map(
                "id=>", productId,
                "version=>", version
        ), RecordResultMapper.get());
        if (versionedProduct == null) {
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal versioned product id");
        }

        String orderId;
        if (purchaserId != null) {
            orderId = session.selectStringValue("market.findOrderIdForPurchase", CC.map(
                    "id=>", productId,
                    "purchaser_id=>", ctx.getAccountId(),
                    //"device_id=>", ctx.getClientDeviceId(),
                    "device_id=>", null
            ), null);
        } else {
            orderId = null;
        }

        // TODO: check the order id has be used for other user!

        // check client order info
        int paid = versionedProduct.asInt("paid");
        int paymentType = versionedProduct.asInt("payment_type");
        boolean firstPurchase = orderId == null;
        if (paid == PT_PRICE_PAID) {
            if (purchaserId == null)
                throw new ServiceException(E_PERMISSION, "Missing purchaser");

            boolean hasOrder = false;
            if (params.hasParam("google_iab_order"))
                hasOrder = true;
            if (params.hasParam("cmcc_mm_order") && params.hasParam("cmcc_mm_trade"))
                hasOrder = true;

            boolean missingOrder = false;
            if (paymentType == PT_PAYMENT_ONCE) {
                if (firstPurchase && !hasOrder)
                    missingOrder = true;
            } else if (paymentType == PT_PAYMENT_REPURCHASABLE) {
                if (!hasOrder)
                    missingOrder = true;
            }

            if (missingOrder)
                throw new ServiceException(E_ILLEGAL_PARAM, "Missing google_iab_order, cmcc_mm_order|cmcc_mm_trade");
        }


        boolean createNewOrder;
        if (ctx.hasAccountId()) {
            if (paymentType == PT_PAYMENT_ONCE) {
                createNewOrder = firstPurchase;
            } else if (paymentType == PT_PAYMENT_REPURCHASABLE) {
                createNewOrder = true;
            } else {
                throw new ServiceException(E_UNKNOWN, "payment_type error");
            }
        } else {
            createNewOrder = false;
        }

        StatisticsService.DateItem statDateItem = StatisticsService.DateItem
                .onTime(now)
                .forProduct(versionedProduct.asString("app_id", ""),
                        versionedProduct.asString("category_id", ""),
                        productId,
                        version)
                .atCountry(ctx.getCountry());

        // Create an new order
        if (createNewOrder) {
            String payCS = "";
            double payAmount = 0.0;

            if (paid == PT_PRICE_PAID) {
                Record priceRec = session.selectOne("market.getProductPriceForPurchase",
                        CC.map("id=>", productId), RecordResultMapper.get());
                localeSelector.selectLocale(priceRec, ctx);
                JsonNode priceNode = priceRec.getJsonNode("price");
                if (priceNode != null) {
                    payCS = priceNode.path("cs").asText();
                    payAmount = priceNode.path("amount").asDouble(0.0);
                } else {
                    // TODO: log warning
                }
            }

            orderId = String.format("%s.%s", productId, RandomUtils2.randomLong());
            session.insert("market.createOrderForPurchase", CC.map(
                    "order_id=>", orderId,
                    "now=>", now,
                    "purchaser_id=>", ObjectUtils.toString(purchaserId),
                    "product_id=>", productId,
                    "product_version=>", version,
                    "product_category_id=>", versionedProduct.asString("category_id", ""),
                    "product_app_id=>", versionedProduct.asString("app_id", ""),
                    "purchaser_device_id=>", ctx.getClientDeviceId(""),
                    "purchaser_locale=>", ctx.getClientLocale(""),
                    "purchaser_ip=>", ctx.getClientIP(""),
                    "purchaser_ua=>", ctx.getClientUserAgent(""),
                    "google_iab_order=>", params.param("google_iab_order").asString(null),
                    "cmcc_mm_order=>", params.param("cmcc_mm_order").asString(null),
                    "cmcc_mm_trade=>", params.param("cmcc_mm_trade").asString(null),
                    "pay_cs=>", payCS,
                    "pay_amount=>", payAmount
            ));
            session.update("market.increaseProductPurchaseCountForPurchase", CC.map("id=>", productId));
            session.update("market.increaseVersionPurchaseCountForPurchase", CC.map("id=>", productId, "version=>", version));
            statisticsService.increaseCount(session, ctx, statDateItem, StatisticsService.PURCHASE_COUNT, 1);
        }


        // Create a new download summary
        String firstDownloadId = session.selectStringValue("market.findFirstDownloadIdForPurchase", CC.map(
                "product_id=>", productId,
                "purchaser_id=>", purchaserId,
                "device_id=>", ctx.getClientDeviceId()
        ), null);
        if (firstDownloadId == null) {
            session.update("market.increaseProductDownloadCountForPurchase", CC.map("id=>", productId));
            session.update("market.increaseVersionDownloadCountForPurchase", CC.map("id=>", productId, "version=>", version));
            statisticsService.increaseCount(session, ctx, statDateItem, StatisticsService.DOWNLOAD_COUNT, 1);
        }

        String downloadId = String.format("%s.%s", productId, RandomUtils2.randomLong());
        session.insert("market.insertDownloadForPurchase", CC.map(
                "id=>", downloadId,
                "now=>", now,
                "order_id=>", ObjectUtils.toString(orderId),
                "purchaser_id=>", purchaserId != null ? purchaserId : "",
                "product_id=>", productId,
                "product_version=>", version,
                "product_category_id=>", versionedProduct.asString("category_id", ""),
                "product_app_id=>", versionedProduct.asString("app_id", ""),
                "device_id=>", ctx.getClientDeviceId(""),
                "locale=>", ctx.getClientLocale(""),
                "ip=>", ctx.getClientIP(""),
                "ua=>", ctx.getClientUserAgent("")
        ));


        // return result
        int action = versionedProduct.asInt("action");
        Record r = Record.of(
                "order_id=>", orderId,
                "action=>", action,
                "url=>", versionedProduct.asString("url"),
                "first_purchase=>", firstPurchase,
                "app_id=>", versionedProduct.asString("app_id")
        );
        if (action == PV_ACTION_DOWNLOAD) {
            r.set("file_size", versionedProduct.asLong("file_size", 0L));
            r.set("file_md5", versionedProduct.asString("file_md5", ""));
        }

        urlCompleter.completeUrl(r);
        return r;
    }

    @Override
    public Record purchase(final ServiceContext ctx, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(params);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return purchase(session, ctx, params);
            }
        });
    }

    RecordsWithTotal listPurchased(RecordSession session, ServiceContext ctx, Params options) {
        if (!ctx.hasAccountId()) {
            return new RecordsWithTotal(new Records(), 0);
        }

        Paging paging = options.getPaging(10000);

        RecordsWithTotal versionedProductIds = session.selectListWithTotal("findPurchasedVersionedProductIdForPurchase", CC.map(
                "purchaser_id=>", ctx.getAccountId(),
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ));

        Records versionedProducts = new Records();
        if (!versionedProductIds.isEmpty()) {
            for (Record versionedProductId : versionedProductIds.getRecords()) {
                Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                        "id=>", versionedProductId.asString("product_id"),
                        "version=>", versionedProductId.asInt("product_version")
                ), RecordResultMapper.get());
                if (versionedProduct != null) {
                    versionedProduct.set("order_id", versionedProductId.asString("order_id"));
                    versionedProduct.set("purchased_at", versionedProductId.asString("purchased_at"));
                    versionedProducts.addUnlessNull(versionedProduct);
                }
            }
        }
        localeSelector.selectLocale(versionedProducts, ctx);
        urlCompleter.completeUrl(versionedProducts);
        attachPurchaseInfo(session, ctx, versionedProducts);
        return new RecordsWithTotal(versionedProducts, versionedProductIds.getTotal());
    }

    @Override
    public RecordsWithTotal listPurchased(final ServiceContext ctx, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(options);
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listPurchased(session, ctx, options);
            }
        });
    }

    Records listPartitions(RecordSession session, ServiceContext ctx, String appId, String categoryId) {
        Records partitions = session.selectList("market.listPartitionsForPurchase", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId
        ), RecordResultMapper.get());
        localeSelector.selectLocale(partitions, ctx);
        urlCompleter.completeUrl(partitions);
        return partitions;
    }

    @Override
    public Records listPartitions(final ServiceContext ctx, final String appId, final String categoryId) {
        Validate.notNull(ctx);
        Validate.notNull(appId);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listPartitions(session, ctx, appId, categoryId);
            }
        });
    }

    private RecordsWithTotal parseVersionedProductIds(RecordSession session, ServiceContext ctx, Record partition, Params options) {
        Paging paging = options.getPaging(100);
        ArrayList<String> productIds = new ArrayList<String>();
        Collections.addAll(productIds, StringUtils2.splitArray(ObjectUtils.toString(partition.asString("list", "")), ',', true));
        if (productIds.isEmpty())
            return new RecordsWithTotal(new Records(), 0);

        int appVersion = options.param("app_version").asInt(0);
        String mod = options.param("app_mod").asString();
        if (StringUtils.isEmpty(mod)) {
            mod = AppMods.getDefaultAppModForPurchase(
                    partition.asString("app_id"), partition.asString("category_id")
            );
        }

        RecordsWithTotal versionedProductIds = session.selectListWithTotal("market.listLastVersionedProductIdsForPurchase", CC.map(
                "product_ids=>", productIds,
                "app_version=>", appVersion,
                "app_mod=>", mod,
                "locale=>", ctx.getClientLocale(),
                "joined_product_ids=>", StringUtils.join(productIds, ","),
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ));
        return versionedProductIds;
    }

    RecordsWithTotal listPartitionProducts(RecordSession session, ServiceContext ctx, String partitionId, Params options) {
        Record partition = session.selectOne("market.getPartitionByIdForPurchase", CC.map("id=>", partitionId));
        Records versionedProducts = new Records();
        RecordsWithTotal versionedProductIds = null;
        if (partition != null && !partition.isEmpty()) {
            int type = partition.asInt("type");

            if (type == PTT_LIST) {
                versionedProductIds = parseVersionedProductIds(session, ctx, partition, options);
            } else if (type == PTT_RULE) {
                String ruleId = partition.asString("rule");
                PartitionRule rule = PartitionRule.getRule(ruleId);
                if (rule != null)
                    versionedProductIds = rule.getProductIds(ctx, options);
            }

            if (versionedProductIds != null && versionedProductIds.getRecords() != null && !versionedProductIds.getRecords().isEmpty()) {
                for (Record versionedProductId : versionedProductIds.getRecords()) {
                    Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                            "id=>", versionedProductId.asString("id"),
                            "version=>", versionedProductId.asInt("version")
                    ), RecordResultMapper.get());
                    versionedProducts.addUnlessNull(versionedProduct);
                }

                localeSelector.selectLocale(versionedProducts, ctx);
                urlCompleter.completeUrl(versionedProducts);
                attachPurchaseInfo(session, ctx, versionedProducts);
            }
        }
        return new RecordsWithTotal(versionedProducts, versionedProductIds != null ? versionedProductIds.getTotal() : 0);
    }

    @Override
    public RecordsWithTotal listPartitionProducts(final ServiceContext ctx, final String partitionId, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(partitionId);

        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listPartitionProducts(session, ctx, partitionId, options);
            }
        });
    }

    Records hasPurchased(RecordSession session, ServiceContext ctx, String[] productIds) {
        Records r = new Records();
        if (ctx.hasAccountId()) {
            Set<String> purchasedProductIds = session.selectList("market.hasPurchasedForPurchase", CC.map(
                    "purchaser_id=>", ctx.getAccountId(),
                    //"device_id=>", ctx.getClientDeviceId(),
                    "device_id=>", null,
                    "product_ids=>", Arrays.asList(productIds)
            )).asStringSet("purchased_product_id");


            for (String productId : productIds) {
                boolean purchased = purchasedProductIds.contains(productId);
                r.add(new Record().set("id", productId).set("purchased", purchased));
            }
        } else {
            for (String productId : productIds)
                r.add(new Record().set("id", productId).set("purchased", false));
        }
        return r;
    }

    @Override
    public Records hasPurchased(final ServiceContext ctx, final String[] productIds) {
        Validate.notNull(ctx);
        Validate.notNull(productIds);

        if (productIds.length == 0) {
            return new Records();
        }

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return hasPurchased(session, ctx, productIds);
            }
        });
    }

}
