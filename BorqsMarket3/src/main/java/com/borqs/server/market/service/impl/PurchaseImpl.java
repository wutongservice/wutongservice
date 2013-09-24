package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.AppMods;
import com.borqs.server.market.models.MLTexts;
import com.borqs.server.market.models.PublishChannels;
import com.borqs.server.market.service.PurchaseService;
import com.borqs.server.market.service.StatisticsService;
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
import org.codehaus.jackson.node.JsonNodeFactory;
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


    private boolean correctPurchased(RecordSession session, ServiceContext ctx, String productId, boolean orgPurchased) {
        // TODO: 修正由于去广告购买失败的问题，认为只要是注册用户就购买这个产品已经成功，这个需要以后注释掉
//        if ("com.borqs.se.object.fighter".equals(productId)) {
//            return true;
//        } else {
//            return orgPurchased;
//        }
        return orgPurchased;
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
            String productId = versionedProduct.asString("id");
            String orderId = session.selectStringValue("market.findOrderIdForPurchase", CC.map(
                    "id=>", productId,
                    "purchaser_id=>", ctx.getAccountId(),
                    //"device_id=>", ctx.getClientDeviceId()
                    "device_id=>", null
            ), null);
            boolean purchased = orderId != null;
            purchased = correctPurchased(session, ctx, productId, purchased);
            versionedProduct.set("purchased", purchased);
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
            mod = AppMods.getDefaultAppMod(session, appId, categoryId);
        }
        String publishChannel = options.param("publish_channel").asString();
        if (StringUtils.isEmpty(publishChannel)) {
            publishChannel = PublishChannels.getDefaultPublishChannel(session, appId);
        }
        int price = options.param("paid").asInt(PT_PRICE_ALL);
        String tag = StringUtils.trimToNull(options.param("tag").asString());
        String orderBy = options.param("order_by").asString();
        boolean withBeta = options.param("with_beta").asBoolean(false);
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
                "beta=>", withBeta,
                "publish_channel=>", publishChannel,
                "paid_field=>", PublishChannels.getPaidField(publishChannel),
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ));

        Records versionedProducts = new Records();
        for (Record versionedProductId : versionedProductIds.getRecords()) {
            Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                    "id=>", versionedProductId.asString("id"),
                    "version=>", versionedProductId.asInt("version"),
                    "paid_field=>", PublishChannels.getPaidField(publishChannel),
                    "price_field=>", PublishChannels.getPriceField(publishChannel)
            ), GenericMapper.get());
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

    Record getProduct(RecordSession session, ServiceContext ctx, String productId, Integer version, Params options) {
        String appMod = options.param("app_mod").asString(null);
        String publishChannel = options.param("publish_channel").asString(null);
        boolean withBeta = options.param("with_beta").asBoolean(false);

        int ver0;
        if (version == null || version == Integer.MIN_VALUE) {
            Record appIdWithCategoryId = session.selectOne("market.getAppIdAndCategoryIdByProductIdForPurchase",
                    CC.map("id=>", productId));
            if (appIdWithCategoryId != null) {
                String appId = appIdWithCategoryId.asString("app_id");
                String categoryId = appIdWithCategoryId.asString("category_id");
                if (StringUtils.isEmpty(appMod)) {
                    appMod = AppMods.getDefaultAppMod(session, appId, categoryId);
                }
                if (StringUtils.isEmpty(publishChannel)) {
                    publishChannel = PublishChannels.getDefaultPublishChannel(session, appId);
                }
            }
            ver0 = session.selectIntValue("market.findProductMaxVersionForPurchase", CC.map(
                    "product_id=>", productId,
                    "app_mod=>", appMod,
                    "publish_channel=>", publishChannel,
                    "beta=>", withBeta
            ), Integer.MIN_VALUE);
            if (ver0 == Integer.MIN_VALUE)
                return null;
        } else {
            ver0 = version;
        }
        Record versionedProduct = session.selectOne("market.getDetailedVersionedProductForPurchase", CC.map(
                "id=>", productId,
                "version=>", ver0
        ), GenericMapper.get());

        if (versionedProduct != null) {
            localeSelector.selectLocale(versionedProduct, ctx.getClientLocale());
            urlCompleter.completeUrl(versionedProduct);
            attachPurchaseInfo(session, ctx, Arrays.asList(versionedProduct));
        }
        return versionedProduct;
    }

    @Override
    public Record getProduct(final ServiceContext ctx, final String productId, final Integer version, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(productId);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getProduct(session, ctx, productId, version, options);
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

        String appId = session.selectStringValue("market.getProductAppIdForPurchase", CC.map("id=>", productId), null);
        if (appId == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product id");

        String publishChannel = params.param("publish_channel").asString();
        if (StringUtils.isEmpty(publishChannel)) {
            publishChannel = PublishChannels.getDefaultPublishChannel(session, appId);
        }

        Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase2", CC.map(
                "id=>", productId,
                "version=>", version,
                "paid_field=>", PublishChannels.getPaidField(publishChannel),
                "price_field=>", PublishChannels.getPriceField(publishChannel)
        ), GenericMapper.get());
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

//            boolean hasOrder = false;
//            if (params.hasParam("google_iab_order"))
//                hasOrder = true;
//            if (params.hasParam("cmcc_mm_order") && params.hasParam("cmcc_mm_trade"))
//                hasOrder = true;
//
//            boolean missingOrder = false;
//            if (paymentType == PT_PAYMENT_ONCE) {
//                if (firstPurchase && !hasOrder)
//                    missingOrder = true;
//            } else if (paymentType == PT_PAYMENT_REPURCHASABLE) {
//                if (!hasOrder)
//                    missingOrder = true;
//            }
//
//            if (missingOrder)
//                throw new ServiceException(E_ILLEGAL_PARAM, "Missing google_iab_order, cmcc_mm_order|cmcc_mm_trade");
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
                        CC.map("id=>", productId), GenericMapper.get());
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
                    "product_app_id=>", appId,
                    "purchaser_device_id=>", ctx.getClientDeviceId(""),
                    "purchaser_locale=>", ctx.getClientLocale(""),
                    "purchaser_ip=>", ctx.getClientIP(""),
                    "purchaser_ua=>", ctx.getClientUserAgent(""),
                    "google_iab_order=>", params.param("google_iab_order").asString(null),
                    "cmcc_mm_order=>", params.param("cmcc_mm_order").asString(null),
                    "cmcc_mm_trade=>", params.param("cmcc_mm_trade").asString(null),
                    "pay_cs=>", payCS,
                    "pay_amount=>", payAmount,
                    "data1=>", params.param("data1").asString(null),
                    "data2=>", params.param("data2").asString(null)
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

    Records purchaseBatch(RecordSession session, ServiceContext ctx, List<Params> paramsList) {
        Records r = new Records();
        for (Params params : paramsList) {
            if (params == null)
                continue;

            Record pr = purchase(session, ctx, params);
            r.add(pr);
        }
        return r;
    }

    @Override
    public Records purchaseBatch(final ServiceContext ctx, final List<Params> paramsList) {
        Validate.notNull(ctx);
        Validate.notNull(paramsList);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return purchaseBatch(session, ctx, paramsList);
            }
        });
    }

    RecordsWithTotal listPurchased(RecordSession session, ServiceContext ctx, String appId, String categoryId, Params options) {
        if (!ctx.hasAccountId()) {
            return new RecordsWithTotal(new Records(), 0);
        }

        Paging paging = options.getPaging(10000);

        RecordsWithTotal versionedProductIds = session.selectListWithTotal("findPurchasedVersionedProductIdForPurchase", CC.map(
                "purchaser_id=>", ctx.getAccountId(),
                "offset=>", paging.getOffset(),
                "app_id=>", appId,
                "category_id=>", categoryId,
                "count=>", paging.getCount()
        ));

        String publishChannel = options.param("publish_channel").asString();
        if (StringUtils.isEmpty(publishChannel) && appId != null) {
            publishChannel = PublishChannels.getDefaultPublishChannel(session, appId);
        }

        Records versionedProducts = new Records();
        if (!versionedProductIds.isEmpty()) {
            for (Record versionedProductId : versionedProductIds.getRecords()) {
                Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                        "id=>", versionedProductId.asString("product_id"),
                        "version=>", versionedProductId.asInt("product_version"),
                        "paid_field=>", publishChannel != null ? PublishChannels.getPaidField(publishChannel) : "paid",
                        "price_field=>", publishChannel != null ? PublishChannels.getPriceField(publishChannel) : "price"
                ), GenericMapper.get());
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
    public RecordsWithTotal listPurchased(final ServiceContext ctx, final String appId, final String categoryId, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(options);
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listPurchased(session, ctx, appId, categoryId, options);
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
                purchased = correctPurchased(session, ctx, productId, purchased);
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

    Record getPartition(RecordSession session, ServiceContext ctx, String partitionId) {
        Record rec = session.selectOne("market.getPartitionByIdForPurchase", CC.map("id=>", partitionId), GenericMapper.get());
        localeSelector.selectLocale(rec, ctx.getClientLocale("en_US"));
        urlCompleter.completeUrl(rec);
        return rec;
    }


    Record completePromotionItem(RecordSession session, ServiceContext ctx, JsonNode promotionNode, String appMod, String publishChannel, boolean withBeta) {
        int type = promotionNode.path("type").asInt(0);
        String locale = ctx.getClientLocale("en_US");

        String target = promotionNode.path("target").asText();
        String promotionName = localeSelector.selectLocaleForText(MLTexts.trimNode(promotionNode.get("name"), locale), locale);
        String promotionDesc = localeSelector.selectLocaleForText(MLTexts.trimNode(promotionNode.get("description"), locale), locale);
        String promotionImageUrl = promotionNode.path("logo_image").asText();
        if (StringUtils.isNotEmpty(promotionImageUrl)) {
            promotionImageUrl = urlCompleter.completeImageUrl(promotionImageUrl);
        }
        Record rec = null;
        if (type == PPT_PRODUCT) {
            if (StringUtils.isNotEmpty(target)) {
                Record product = getProduct(session, ctx, target, null, Params.of(
                        "app_mod=>", appMod, "publish_channel=>", publishChannel, "with_beta=>", withBeta));
                if (product != null) {
                    if (StringUtils.isBlank(promotionImageUrl)) {
                        promotionImageUrl = product.asString("promotion_image", "");
                        if (StringUtils.isBlank(promotionImageUrl))
                            promotionImageUrl = product.asString("logo_image", "");
                    }
                    rec = new Record().set("type", type).set("target", target)
                            .set("name", StringUtils.isNotEmpty(promotionName) ? promotionName : product.asString("name", ""))
                            .set("description", StringUtils.isNotEmpty(promotionDesc) ? promotionDesc : product.asString("description", ""))
                            .set("promotion_image", promotionImageUrl);
                }
            }
        } else if (type == PPT_PARTITION) {
            if (StringUtils.isNotEmpty(target)) {
                Record partition = getPartition(session, ctx, target);
                if (partition != null) {
                    if (StringUtils.isBlank(promotionImageUrl)) {
                        promotionImageUrl = partition.asString("logo_image", "");
                    }
                    rec = new Record().set("type", type).set("target", target)
                            .set("name", StringUtils.isNotEmpty(promotionName) ? promotionName : partition.asString("name", ""))
                            .set("description", StringUtils.isNotEmpty(promotionDesc) ? promotionDesc : partition.asString("description", ""))
                            .set("promotion_image", promotionImageUrl);
                }
            }
        } else if (type == PPT_TAG || type == PPT_SORT) {
            if (StringUtils.isNotEmpty(target)) {
                String tag = ObjectUtils.toString(target);
                return new Record().set("type", type).set("target", target)
                        .set("name", StringUtils.isNotEmpty(promotionName) ? promotionName : tag)
                        .set("description", StringUtils.isNotEmpty(promotionDesc) ? promotionDesc : "")
                        .set("promotion_image", ObjectUtils.toString(promotionImageUrl));
            }
        } else if (type == PPT_SHARE) {
            return new Record().set("type", type).set("target", "")
                    .set("name", StringUtils.isNotEmpty(promotionName) ? promotionName : "")
                    .set("description", StringUtils.isNotEmpty(promotionDesc) ? promotionDesc : "")
                    .set("promotion_image", ObjectUtils.toString(promotionImageUrl));
        }

        return rec;
    }

    Records getPromotions(RecordSession session, ServiceContext ctx, String appId, String categoryId, Params opts) throws Exception {
        /*
         return spec
         [{
            "type":PPT_PRODUCT,
            "name":"Product name",
            "description":"Product description",
            "promotion_image":"Product promotion image (if exists) OR Product logo image",
            "target":"product id"
         },{
            "type":PPT_PARTITION,
            "name":"Partition name",
            "description":"Partition description",
            "promotion_image":"Partition logo image",
            "target":"partition id"
         },{
            "type":PPT_TAG,
            "name":"",
            "description":"",
            "promotion_image":"Last ...",
            "target":""
         },
         {
            "type":PPT_SHARE,
            "name":"",
            "description":"",
            "promotion_image":"Last share logo image",
            "target":""
         }
         ]
         */
        String mod = opts.param("app_mod").asString();
        if (StringUtils.isEmpty(mod)) {
            mod = AppMods.getDefaultAppMod(session, appId, categoryId);
        }
        String publishChannel = opts.param("publish_channel").asString();
        if (StringUtils.isEmpty(publishChannel)) {
            publishChannel = PublishChannels.getDefaultPublishChannel(session, appId);
        }
        boolean withBeta = opts.param("with_beta").asBoolean(false);

        Record promotionRec = session.selectOne("market.getPromotionsForPurchase", CC.map(
                "app_id=>", appId,
                "category_id=>", ObjectUtils.toString(categoryId)
        ));
        String promotionsJson = promotionRec != null ? promotionRec.asString("list") : "[]";
        JsonNode promotionsNode;
        if (StringUtils.isBlank(promotionsJson)) {
            promotionsNode = JsonNodeFactory.instance.arrayNode();
        } else {
            promotionsNode = JsonUtils.parseJson(promotionsJson);
        }
        if (!promotionsNode.isArray())
            throw new ServiceException(Errors.E_PROMOTION, "Promotions list is not array value");

        Records promotions = new Records();
        for (int i = 0; i < promotionsNode.size(); i++) {
            JsonNode promotionNode = promotionsNode.get(i);
            if (!promotionNode.isObject())
                throw new ServiceException(Errors.E_PROMOTION, "Promotion item is not object value");

            Record promotion = completePromotionItem(session, ctx, promotionNode, mod, publishChannel, withBeta);
            if (promotion != null)
                promotions.add(promotion);
        }
        return promotions;
    }

    @Override
    public Records getPromotions(final ServiceContext ctx, final String appId, final String categoryId, final Params opts) {
        Validate.notNull(ctx);
        Validate.notNull(appId);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return getPromotions(session, ctx, appId, categoryId, opts);
            }
        });
    }

    private RecordsWithTotal parseVersionedProductIds(RecordSession session, ServiceContext ctx, Record partition, String mod, String publishChannel, boolean withBeta, int appVersion, Paging paging) {
        ArrayList<String> productIds = new ArrayList<String>();
        Collections.addAll(productIds, StringUtils2.splitArray(ObjectUtils.toString(partition.asString("list", "")), ',', true));
        if (productIds.isEmpty())
            return new RecordsWithTotal(new Records(), 0);

        if (StringUtils.isEmpty(mod)) {
            mod = AppMods.getDefaultAppMod(session, partition.asString("app_id"), partition.asString("category_id"));
        }
        if (StringUtils.isEmpty(publishChannel)) {
            publishChannel = PublishChannels.getDefaultPublishChannel(session, partition.asString("app_id"));
        }

        RecordsWithTotal versionedProductIds = session.selectListWithTotal("market.listLastVersionedProductIdsForPurchase", CC.map(
                "product_ids=>", productIds,
                "app_version=>", appVersion,
                "app_mod=>", mod,
                "publish_channel=>", publishChannel,
                "beta=>", withBeta,
                "locale=>", ctx.getClientLocale(),
                "joined_product_ids=>", StringUtils.join(productIds, ","),
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ));
        return versionedProductIds;
    }

    RecordsWithTotal listProductsInPartition(RecordSession session, ServiceContext ctx, String partitionId, Params opts, Paging paging) {
        Record partition = session.selectOne("market.getPartitionByIdForPurchase", CC.map("id=>", partitionId));
        Records versionedProducts = new Records();
        RecordsWithTotal versionedProductIds = null;
        if (partition != null && !partition.isEmpty()) {
            int appVersion = opts.param("app_version").asInt(0);
            String appMod = opts.param("app_mod").asString();
            String publishChannel = opts.param("publish_channel").asString();
            boolean withBeta = opts.param("with_beta").asBoolean(false);

            if (StringUtils.isEmpty(publishChannel)) {
                publishChannel = PublishChannels.getDefaultPublishChannel(session, partition.asString("app_id"));
            }

            versionedProductIds = parseVersionedProductIds(session, ctx, partition, appMod, publishChannel, withBeta, appVersion, paging);
            if (versionedProductIds != null && versionedProductIds.getRecords() != null && !versionedProductIds.getRecords().isEmpty()) {
                for (Record versionedProductId : versionedProductIds.getRecords()) {
                    Record versionedProduct = session.selectOne("market.getSimpleVersionedProductForPurchase", CC.map(
                            "id=>", versionedProductId.asString("id"),
                            "version=>", versionedProductId.asInt("version"),
                            "paid_field=>", PublishChannels.getPaidField(publishChannel),
                            "price_field=>", PublishChannels.getPriceField(publishChannel)
                    ), GenericMapper.get());
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
    public RecordsWithTotal listProductsInPartition(final ServiceContext ctx, final String partitionId, final Params opts, final Paging paging) {
        Validate.notNull(ctx);
        Validate.notNull(partitionId);
        Validate.notNull(paging);

        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listProductsInPartition(session, ctx, partitionId, opts, paging);
            }
        });
    }
}
