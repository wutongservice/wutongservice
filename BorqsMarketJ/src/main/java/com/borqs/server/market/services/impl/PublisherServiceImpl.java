package com.borqs.server.market.services.impl;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.services.UserId;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.SqlSessionHandler;
import com.borqs.server.market.utils.mybatis.SqlSessionUtils2;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.services.PublisherService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import static com.borqs.server.market.Errors.*;

import java.util.*;

@Service("service.defaultPublisherService")
public class PublisherServiceImpl extends ServiceSupport implements PublisherService {


    public PublisherServiceImpl() {
    }


    protected Records listBorqsApps(ServiceContext ctx, SqlSession session) {
        List<Record> apps = session.selectList("market.publisher_listBorqsApps_1");
        return new Records(apps);
    }

    @Override
    public Records listBorqsApps(final ServiceContext ctx) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Records>() {
            @Override
            public Records handleSession(SqlSession session) throws Exception {
                return listBorqsApps(ctx, session);
            }
        });
    }

    protected Record getBorqsApp(ServiceContext ctx, SqlSession session, String appId) {
        return session.selectOne("market.publisher_getBorqsApp_1", CC.map("id=>", appId));
    }

    @Override
    public Record getBorqsApp(final ServiceContext ctx, final String appId) throws ServiceException {
        Validate.notNull(appId);
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return getBorqsApp(ctx, session, appId);
            }
        });
    }

    protected boolean categoryExists(ServiceContext ctx, SqlSession session, String appId, String category) {
        Object o = SqlSessionUtils2.selectValue(session, "market.categoryExists_1", CC.map("app=>", appId, "category=>", category));
        return PrimitiveTypeConverter.toBoolean(o);
    }

    protected Records listPricetags(ServiceContext ctx, SqlSession session, String appId, String category, boolean withAppAndCategory) {
        String lang = ctx.getClientLanguage("en_US");
        List<Record> pricetags = session.selectList("market.publisher_listCategories_1", CC.map(
                "app_id =>", ObjectUtils.toString(appId),
                "category =>", category,
                "with_app_and_category =>", withAppAndCategory
        ));

        fieldTrimmer.trimLanguage(pricetags, lang).trimPrice(pricetags, lang, true);
        return new Records(pricetags);
    }

    protected Records listCategories(ServiceContext ctx, SqlSession session, String appId, boolean withPricetags) {
        List<Record> categories = session.selectList("market.publisher_listCategories_1", CC.map("app_id=>", ObjectUtils.toString(appId)));
        if (withPricetags) {
            for (Record category : categories) {
                Records pricetags = listPricetags(ctx, session, appId, category.asString("category"), false);
                category.set("pricetags", pricetags);
            }
        }
        return new Records(categories);
    }

    @Override
    public Records listCategories(final ServiceContext ctx, final String appId, final boolean withPricetags) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Records>() {
            @Override
            public Records handleSession(SqlSession session) throws Exception {
                return listCategories(ctx, session, appId, withPricetags);
            }
        });
    }

    protected static void sortVersion(List<Record> pvc) {
        Collections.sort(pvc, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                Integer pid1 = o1.asInt("product_id");
                Integer pid2 = o2.asInt("product_id");
                return -pid1.compareTo(pid2);
            }
        });
    }

    protected Records listVersions(ServiceContext ctx, SqlSession session, String productId, boolean includeProductId) {
        Map<String, Records> pvs = listVersions(ctx, session, new String[]{productId}, includeProductId);
        return pvs.values().iterator().next();
    }

    protected Map<String, Records> listVersions(ServiceContext ctx, SqlSession session, String[] productIds, boolean includeProductId) {
        LinkedHashMap<String, Records> r = new LinkedHashMap<String, Records>();
        if (ArrayUtils.isNotEmpty(productIds)) {
            for (String pid : productIds)
                r.put(pid, new Records());

            List<Record> pvs = session.selectList("market.publisher_listVersions_1", CC.map("product_ids=>", Arrays.asList(productIds)));
            for (Record pv0 : pvs) {
                String pid0 = pv0.asString("product_id");
                Records pvc = r.get(pid0);
                pvc.add(pv0);
                if (!includeProductId)
                    pv0.removeField("product_id");
            }

            for (Records pvc : r.values())
                sortVersion(pvc);
        }
        return r;
    }

    protected Records listProducts(ServiceContext ctx, SqlSession session, String appId, String category) throws ServiceException {
        String authorId = checkAccount(ctx, UserId.ROLE_PUBLISHER);
        List<Record> products0 = session.selectList("market.publisher_listProducts_1", CC.map(
                "app_id =>", appId,
                "category =>", category,
                "author_id =>", authorId
        ));
        Records products = new Records(products0);
        if (!products.isEmpty()) {
            String[] productIds = products.valuesAsStringArray("id");
            Map<String, Records> pvs = listVersions(ctx, session, productIds, false);
            for (Record product : products) {
                product.set("versions", pvs.get(product.asString("id")));
            }
        }
        return products;
    }

    @Override
    public Records listProducts(final ServiceContext ctx, final String appId, final String category) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Records>() {
            @Override
            public Records handleSession(SqlSession session) throws Exception {
                return listProducts(ctx, session, appId, category);
            }
        });
    }

    protected Record getProduct(ServiceContext ctx, SqlSession session, String productId) {
        Record product = session.selectOne("market.publisher_getProduct_1", CC.map("id=>", productId, "author_id=>", ctx.getClientId()));
        if (product != null) {
            Records pvc = listVersions(ctx, session, productId, false);
            product.set("versions", pvc);
        }
        return product;
    }

    @Override
    public Record getProduct(final ServiceContext ctx, final String productId) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return getProduct(ctx, session, productId);
            }
        });
    }

    protected Record getVersion(ServiceContext ctx, SqlSession session, String productId, int version) {
        Record pv = session.selectOne("market.publisher_getVersion_1", CC.map(
                "id=>", productId,
                "version=>", version,
                "author_id=>", ctx.getClientId()
        ));
        if (pv != null) {
            String pricetagId = pv.asString("pricetag_id", null);
            if (StringUtils.isNotEmpty(pricetagId)) {
                Record price = session.selectOne("market.publisher_getVersion_2", CC.map(
                        "app_id=>", pv.asString("app_id"),
                        "category=>", pv.asString("category"),
                        "pricetag_id=>", pv.asString("pricetag_id")
                ));
                pv.set("free", price.asInt("free"));
                pv.set("price", price.get("price"));
            } else {
                pv.set("free", null).set("price", null);
            }
        }
        return pv;
    }

    @Override
    public Record getVersion(final ServiceContext ctx, final String productId, final int version) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return getVersion(ctx, session, productId, version);
            }
        });
    }

    protected boolean productExists(ServiceContext ctx, SqlSession session, String productId) {
        Object r = SqlSessionUtils2.selectValue(session, "market.publisher_productExists_1", CC.map("id=>", productId));
        return PrimitiveTypeConverter.toBoolean(r);
    }

    protected String getAuthorId(ServiceContext ctx, SqlSession session, String productId) {
        return SqlSessionUtils2.selectValue(session, "market.publisher_getAuthorId_1", CC.map("id=>", productId));
    }

    protected int getMaxVersion(ServiceContext ctx, SqlSession session, String productId) {
        Integer r = SqlSessionUtils2.selectValue(session, "market.publisher_getMaxVersion_1", CC.map("id=>", productId));
        return r != null ? r : -1;
    }

    protected void checkCategory(ServiceContext ctx, SqlSession session, String appId, String category) throws ServiceException {
        boolean b = categoryExists(ctx, session, appId, category);
        if (!b)
            throw new ServiceException(E_ILLEGAL_CATEGORY, String.format("Illegal category %s.%s", appId, category));
    }

    protected Record publishProduct(ServiceContext ctx, SqlSession session, String appId, String category, String productId, String defaultLanguage) throws ServiceException {

        if (StringUtils.isEmpty(defaultLanguage))
            defaultLanguage = "en_US";

        checkCategory(ctx, session, appId, category);
        if (productExists(ctx, session, productId))
            throw new ServiceException(E_ILLEGAL_PRODUCT, "Product exists " + productId);


        long now = DateTimeUtils.nowMillis();
        int n = session.insert("market.publisher_publishProduct_1", CC.map(
                "id=>", productId,
                "now=>", now,
                "app_id=>", appId,
                "category=>", category,
                "author_name=>", "",
                "author_email=>", "",
                "author_phone=>", "",
                "author_id=>", ctx.getClientId(),
                "default_lang=>", defaultLanguage
        ));
        return getProduct(ctx, session, productId);
    }

    @Override
    public Record publishProduct(final ServiceContext ctx, final String appId, final String category, final String productId, final String defaultLanguage) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return publishProduct(ctx, session, appId, category, productId, defaultLanguage);
            }
        });
    }

    protected Record updateProduct(ServiceContext ctx, SqlSession session, String productId, Params params) throws ServiceException {
        if (!productExists(ctx, session, productId))
            throw new ServiceException(E_ILLEGAL_PRODUCT, "Illegal product");

        JsonNode logoImage = params.param("logo_image").asJson();
        JsonNode coverImage = params.param("cover_image").asJson();
        JsonNode name = params.param("name").asJson();
        JsonNode desc = params.param("description").asJson();
        String defLang = params.param("default_lang").asString();
        checkMultipleLanguageValues(ctx, session, name);
        checkMultipleLanguageValues(ctx, session, desc);
        checkLanguage(ctx, session, defLang);
        checkImageValue(logoImage);
        checkImageValue(coverImage);
        long now = DateTimeUtils.nowMillis();
        if (!params.isEmpty()) {
            session.update("market.publisher_updateProduct_1", CC.map(
                    "id=>", ObjectUtils.toString(productId),
                    "pricetag_id=>", params.param("pricetag_id").asString(),
                    "available_langs=>", params.param("available_langs").asString(),
                    "type1=>", params.param("type1").asString(),
                    "type2=>", params.param("type2").asString(),
                    "type3=>", params.param("type3").asString(),
                    "author_name=>", params.param("author_name").asString(),
                    "author_email=>", params.param("author_email").asString(),
                    "author_phone=>", params.param("author_phone").asString(),
                    "author_google_id=>", params.param("author_google_id").asString(),
                    "author_website=>", params.param("author_website").asString(),
                    "author_phone=>", params.param("author_phone").asString(),
                    "logo_image=>", logoImage != null ? JsonUtils.toJson(logoImage, false) : null,
                    "cover_image=>", coverImage != null ? JsonUtils.toJson(coverImage, false) : null,
                    "default_lang=>", defLang,
                    "name=>", name != null ? JsonUtils.toJson(name, false) : null,
                    "description=>", desc != null ? JsonUtils.toJson(desc, false) : null,
                    "now=>", now
            ));
        }
        return getProduct(ctx, session, productId);
    }

    @Override
    public Record updateProduct(final ServiceContext ctx, final String productId, final Params params) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return updateProduct(ctx, session, productId, params);
            }
        });
    }

    protected Record publishVersion(ServiceContext ctx, SqlSession session, String productId, int version, Params params) throws ServiceException {
        JsonNode verName = params.param("version_name").asJson();
        JsonNode recentChange = params.param("recent_change").asJson();
        JsonNode s1 = params.param("screenshot1_image").asJson();
        JsonNode s2 = params.param("screenshot2_image").asJson();
        JsonNode s3 = params.param("screenshot3_image").asJson();
        JsonNode s4 = params.param("screenshot4_image").asJson();
        JsonNode s5 = params.param("screenshot5_image").asJson();
        checkMultipleLanguageValues(ctx, session, verName);
        checkMultipleLanguageValues(ctx, session, recentChange);
        checkImageValue(s1);
        checkImageValue(s2);
        checkImageValue(s3);
        checkImageValue(s4);
        checkImageValue(s5);
        long now = DateTimeUtils.nowMillis();

        if (!productExists(ctx, session, productId))
            throw new ServiceException(E_ILLEGAL_PRODUCT, "Illegal product");

        int maxVer = getMaxVersion(ctx, session, productId);
        if (version < 0 || version <= maxVer)
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal version");

        String authorId = getAuthorId(ctx, session, productId);
        if (StringUtils.equals(authorId, ctx.getClientId()))
            throw new ServiceException(E_PERMISSION, "Permission error");

        session.insert("market.publisher_publishVersion_1", CC.map(
                "pid=>", ObjectUtils.toString(productId),
                "now=>", now,
                "version_name=>", verName != null ? JsonUtils.toJson(verName, false) : "{}",
                "recent_change=>", recentChange != null ? JsonUtils.toJson(recentChange, false) : "{}",
                "min_app_version=>", params.param("min_app_version").asInt(0),
                "max_app_version=>", params.param("max_app_version").asInt(Integer.MAX_VALUE),
                "supported_mod=>", params.param("supported_mod").asString(""),
                "action=>", params.param("action").asString("download"),
                "action_url=>", params.param("action_url").asString(""),
                "file_size=>", params.param("file_size").asLong(0),
                "file_md5=>", params.param("file_md5").asString(""),
                "s1=>", s1 != null ? JsonUtils.toJson(s1, false) : "{}",
                "s2=>", s2 != null ? JsonUtils.toJson(s2, false) : "{}",
                "s3=>", s3 != null ? JsonUtils.toJson(s3, false) : "{}",
                "s4=>", s4 != null ? JsonUtils.toJson(s4, false) : "{}",
                "s5=>", s5 != null ? JsonUtils.toJson(s5, false) : "{}"
        ));

        session.update("market.publisher_publishVersion_2", CC.map(
                "pid=>", ObjectUtils.toString(productId),
                "now=>", now
        ));

        return getVersion(ctx, session, productId, version);
    }

    @Override
    public Record publishVersion(final ServiceContext ctx, final String productId, final int version, final Params params) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return publishVersion(ctx, session, productId, version, params);
            }
        });
    }

    protected Record updateVersion(ServiceContext ctx, SqlSession session, String productId, int version, Params params) throws ServiceException {
        JsonNode verName = params.param("version_name").asJson();
        JsonNode recentChange = params.param("recent_change").asJson();
        JsonNode s1 = params.param("screenshot1_image").asJson();
        JsonNode s2 = params.param("screenshot2_image").asJson();
        JsonNode s3 = params.param("screenshot3_image").asJson();
        JsonNode s4 = params.param("screenshot4_image").asJson();
        JsonNode s5 = params.param("screenshot5_image").asJson();
        checkMultipleLanguageValues(ctx, session, verName);
        checkMultipleLanguageValues(ctx, session, recentChange);
        checkImageValue(s1);
        checkImageValue(s2);
        checkImageValue(s3);
        checkImageValue(s4);
        checkImageValue(s5);
        long now = DateTimeUtils.nowMillis();

        String authorId = SqlSessionUtils2.selectValue(session, "market.publisher_updateVersion_2", CC.map(
                "pid=>", ObjectUtils.toString(productId),
                "version=>", version
        ));
        if (authorId == null)
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal product version");

        if (!StringUtils.equals(ctx.getClientId(), authorId))
            throw new ServiceException(E_PERMISSION, "Permission error");

        Integer minAppVer = params.paramExists("min_app_version")
                ? params.param("min_app_version").asInt()
                : null;
        Integer maxAppVer = params.paramExists("max_app_version")
                ? params.param("max_app_version").asInt(Integer.MAX_VALUE)
                : null;
        Integer fileSize = params.paramExists("file_size")
                ? params.param("file_size").asInt(0)
                : null;
        session.update("market.publisher_updateVersion_1", CC.map(
                "pid=>", ObjectUtils.toString(productId),
                "version=>", version,
                "now=>", now,
                "min_app_version=>", minAppVer,
                "max_app_version=>", maxAppVer,
                "supported_mod=>", params.param("supported_mod").asString(),
                "action=>", params.param("action").asString(),
                "action_url=>", params.param("action_url").asString(),
                "file_size=>", fileSize,
                "file_md5=>", params.param("file_md5").asString(),
                "s1=>", s1 != null ? JsonUtils.toJson(s1, false) : null,
                "s2=>", s2 != null ? JsonUtils.toJson(s2, false) : null,
                "s3=>", s3 != null ? JsonUtils.toJson(s3, false) : null,
                "s4=>", s4 != null ? JsonUtils.toJson(s4, false) : null,
                "s5=>", s5 != null ? JsonUtils.toJson(s5, false) : null,
                "version_name=>", verName != null ? JsonUtils.toJson(verName, false) : null,
                "recent_change=>", recentChange != null ? JsonUtils.toJson(recentChange, false) : null
        ));

        return getVersion(ctx, session, productId, version);
    }

    @Override
    public Record updateVersion(final ServiceContext ctx, final String productId, final int version, final Params params) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Record>() {
            @Override
            public Record handleSession(SqlSession session) throws Exception {
                return updateVersion(ctx, session, productId, version, params);
            }
        });
    }


    protected int activeVersion(ServiceContext ctx, SqlSession session, String productId, Integer version, int status) throws ServiceException {

        if (version != null) {
        Object b = SqlSessionUtils2.selectValue(session, "market.publisher_activeVersion_2", CC.map(
                "pid=>", productId,
                "version=>", version
        ));
        if (!PrimitiveTypeConverter.toBoolean(b))
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal product version");
        } else {
            if (!productExists(ctx, session, productId))
                throw new ServiceException(E_ILLEGAL_PRODUCT, "Illegal product");
        }

        session.update("market.publisher_activeVersion_2", CC.map(
                "pid=>", productId,
                "status=>", status,
                "version=>", version
        ));

        return status;
    }

    @Override
    public int activeVersion(final ServiceContext ctx, final String productId, final Integer version, final int status) throws ServiceException {
        checkAccount(ctx, UserId.ROLE_PUBLISHER);
        return openSession(new SqlSessionHandler<Integer>() {
            @Override
            public Integer handleSession(SqlSession session) throws Exception {
                return activeVersion(ctx, session, productId, version, status);
            }
        });
    }
}
