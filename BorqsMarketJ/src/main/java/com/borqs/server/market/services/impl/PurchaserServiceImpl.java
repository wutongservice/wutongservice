package com.borqs.server.market.services.impl;


import com.borqs.server.market.Features;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;


import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.RandomUtils2;
import com.borqs.server.market.utils.mybatis.SqlSessionHandler;
import com.borqs.server.market.utils.mybatis.SqlSessionUtils2;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.services.PurchaserService;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.Params;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.List;

import static com.borqs.server.market.Errors.*;

@Service("service.defaultPurchaserService")
public class PurchaserServiceImpl extends ServiceSupport implements PurchaserService {

    public PurchaserServiceImpl() {
    }

    private void attachPurchaseInfo(ServiceContext ctx, SqlSession session, List<Record> versionedProducts) {
        for (Record versionedProduct : versionedProducts) {

            // purchasable
            boolean purchasable = false;
            if (versionedProduct.asBoolean("free", false)) {
                if (Features.forBorqs()) {
                    purchasable = ctx.hasPurchaserId() || ctx.hasClientGoogleIds();
                } else if (Features.forCartoonMarket()) {
                    purchasable = ctx.hasPurchaserId();
                }

            } else {
                if (Features.forBorqs()) {
                    purchasable = ctx.hasPurchaserId() || ctx.hasClientGoogleIds() || ctx.hasClientDeviceId();
                } else if (Features.forCartoonMarket()) {
                    purchasable = true;
                }
            }
            versionedProduct.set("purchasable", purchasable);


            // purchased
            boolean purchased;
            Record id = null;
            if (Features.forBorqs()) {
                id = session.selectOne("market.attachPurchaseInfo_1", CC.map(
                        "id =>", versionedProduct.asString("id"),
                        "purchaser_id =>", ctx.getPurchaserId(),
                        "google_ids =>", ctx.getClientGoogleIdsAsList(),
                        "device_id =>", ctx.getClientDeviceId()
                ));
            } else if (Features.forCartoonMarket()) {
                id = session.selectOne("market.attachPurchaseInfo_1", CC.map(
                        "id =>", versionedProduct.asString("id"),
                        "purchaser_id =>", ctx.getPurchaserId(),
                        "google_ids =>", null,
                        "device_id =>", null
                ));
            }

            purchased = (id != null);
            versionedProduct.set("purchased", purchased);
        }
    }

    @Override
    public Records listProducts(final ServiceContext ctx, final String appId, final Params options) throws ServiceException {
        Validate.notNull(appId);

        final String lang = ctx.getClientLanguage("en_US");

        final String category = options.param("category").asString();
        final int appVersion = options.param("appVersion").asInt(0);
        final String mod = options.param("appMod").asString();
        final int price = options.param("price").asInt(PRICE_ALL);
        final String orderBy = options.param("orderBy").asString();
        final int page = options.param("page").asInt(0);
        final int count = options.param("count").asInt(20);
        //JsonNode allInstalled = options.param("allInstalled").asJson();


        return openSession(new SqlSessionHandler<Records>() {
            @Override
            public Records handleSession(SqlSession session) throws Exception {
                List<Record> vids = session.selectList("market.listProducts_1", CC.map(
                        "id =>", appId,
                        "category =>", category,
                        "lang =>", ctx.getClientLanguage(),
                        "appVersion =>", appVersion,
                        "mod =>", mod,
                        "price =>", price,
                        "orderBy =>", orderBy,
                        "offset =>", page * count,
                        "count =>", count
                ));

                Records vps = new Records();
                for (Record vid : vids) {
                    Record vp = session.selectOne("market.listProducts_2", CC.map(
                            "id =>", vid.asString("id"),
                            "version =>", vid.asInt("version")
                    ));
                    if (vp != null) {
                        vps.add(vp);
                    }
                }

                fieldTrimmer
                        .trimLanguage(vps, lang)
                        .trimPrice(vps, lang, false);
                attachPurchaseInfo(ctx, session, vps);
                return vps;
            }
        });
    }

    @Override
    public Record getProduct(final ServiceContext ctx, final String appId, final Integer version) throws ServiceException {
        Validate.notNull(appId);

        final String lang = ctx.getClientLanguage("en_US");

        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                int ver0;
                if (version == null) {
                    ver0 = 0; // TODO: xx
                } else {
                    ver0 = version;
                }
                Record versionedProduct = session.selectOne("market.getProduct_1", CC.map(
                        "id =>", appId,
                        "version =>", ver0
                ));

                if (versionedProduct != null) {
                    fieldTrimmer
                            .trimLanguage(versionedProduct, lang)
                            .trimPrice(versionedProduct, lang, false);
                    attachPurchaseInfo(ctx, session, Arrays.asList(versionedProduct));
                }
                return versionedProduct;
            }
        });
    }

    @Override
    public Record purchase(final ServiceContext ctx, final String id, final int version, final Params options) throws ServiceException {
        Validate.notNull(id);

        final long now = DateTimeUtils.nowMillis();
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                Record product = session.selectOne("market.purchase_1", CC.map(
                        "id =>", id,
                        "version =>", version
                ));
                if (product == null)
                    throw new ServiceException(E_ILLEGAL_VERSION, "Illegal versioned product id");

                if (!product.asBoolean("free", false)) {
                    // paid
                    if (Features.forBorqs()) {
                        if (!ctx.hasPurchaserId() && !ctx.hasClientGoogleIds())
                            throw new ServiceException(E_PERMISSION, "Need login as purchaser").withDetails(
                                    "Missing id",
                                    "Missing google id"
                            );
                    } else {
                        if (!ctx.hasPurchaserId())
                            throw new ServiceException(E_PERMISSION, "Need login as purchaser").withDetails(
                                    "Missing id"
                            );
                    }
                } else {
                    // free
                    if (Features.forBorqs()) {
                        if (!ctx.hasPurchaserId() && !ctx.hasClientGoogleIds() && !ctx.hasClientDeviceId())
                            throw new ServiceException(E_PERMISSION, "Need login as purchaser").withDetails(
                                    "Missing id",
                                    "Missing google id",
                                    "Missing device id"
                            );
                    }
                }

                String orderId = SqlSessionUtils2.selectValue(session, "market.purchase_2", CC.map(
                        "id =>", id,
                        "purchaser_id =>", ctx.getPurchaserId(),
                        "google_ids =>", ctx.getClientGoogleIdsAsList(),
                        "device_id =>", ctx.getClientDeviceId()
                ));

                boolean firstPurchase = false;
                if (orderId == null) {
                    // Purchase!
                    firstPurchase = true;
                    orderId = String.format("%s.%s.0", id, RandomUtils2.randomLong());
                    session.insert("market.purchase_3", CC.map(
                            "id =>", orderId,
                            "created_at =>", now,
                            "product_id =>", id,
                            "product_version =>", version,
                            "app_id =>", product.asString("app_id"),
                            "category =>", product.asString("category"),
                            "purchaser_id =>", StringUtils.trimToEmpty(ctx.getPurchaserId()),
                            "purchaser_device_id =>", StringUtils.trimToEmpty(ctx.getClientDeviceId()),
                            "purchaser_google_id1 =>", StringUtils.trimToEmpty(ctx.getClientGoogleId(0)),
                            "purchaser_google_id2 =>", StringUtils.trimToEmpty(ctx.getClientGoogleId(1)),
                            "purchaser_google_id3 =>", StringUtils.trimToEmpty(ctx.getClientGoogleId(2)),
                            "google_iab_order_id =>", options.param("google_iab_order_id").asString(""),
                            "purchaser_locale =>", StringUtils.trimToEmpty(ctx.getClientLanguage()),
                            "purchaser_ip =>", StringUtils.trimToEmpty(ctx.getClientIP()),
                            "purchaser_user_agent =>", StringUtils.trimToEmpty(ctx.getClientUserAgent())
                    ));
                    session.update("market.purchase_4", CC.map("id=>", id));
                    session.update("market.purchase_5", CC.map("id=>", id, "version=>", version));
                }

                String action = product.asString("action");
                Record r = Record.of(
                        "order_id =>", orderId,
                        "action =>", action,
                        "url =>", product.asString("action_url"),
                        "first_purchase =>", firstPurchase,
                        "app =>", product.asString("app_id"),
                        "app_id =>", product.asString("app_id")
                );
                if ("download".equals(action)) {
                    r.set("file_size", product.asLong("file_size"));
                    r.set("file_md5", product.asString("file_md5", ""));
                }

                fieldTrimmer.trimUrlField(r, "url");
                return r;
            }
        });
    }
}
