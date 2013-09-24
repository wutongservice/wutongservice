package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.FileStorageUtils;
import com.borqs.server.market.models.Tags;
import com.borqs.server.market.models.ValidateUtils;
import com.borqs.server.market.resfile.ResourceFile;
import com.borqs.server.market.service.PublishService;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

@Service("service.defaultPublishService")
public class PublishImpl extends ServiceSupport implements PublishService {

    private boolean authorMustBeBorqsId = false;
    private int initVersionStatus = PV_STATUS_APPROVED;
    private FileStorage productStorage;
    private FileStorage imageStorage;


    public PublishImpl() {
    }

    public boolean isAuthorMustBeBorqsId() {
        return authorMustBeBorqsId;
    }

    public void setAuthorMustBeBorqsId(boolean authorMustBeBorqsId) {
        this.authorMustBeBorqsId = authorMustBeBorqsId;
    }

    public int getInitVersionStatus() {
        return initVersionStatus;
    }

    public void setInitVersionStatus(int initVersionStatus) {
        this.initVersionStatus = initVersionStatus;
    }

    public FileStorage getProductStorage() {
        return productStorage;
    }

    @Autowired
    @Qualifier("storage.product")
    public void setProductStorage(FileStorage productStorage) {
        this.productStorage = productStorage;
    }

    public FileStorage getImageStorage() {
        return imageStorage;
    }

    @Autowired
    @Qualifier("storage.image")
    public void setImageStorage(FileStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    Records listApps(RecordSession session, ServiceContext ctx) {
        checkAccountId(ctx, authorMustBeBorqsId);
        return session.selectList("market.listAppsForPublish", GenericMapper.get());
    }

    protected void checkPermission(RecordSession session, ServiceContext ctx, String id) {
        final String KEY = "productOwnerChecked";
        final String errorMsg = "You are not the owner for the product";
        if (!ctx.hasSession(KEY)) {
            Record productWithAuthorId = session.selectOne("market.getProductIdAndAuthorIdForPublish", CC.map("id=>", id));
            if (productWithAuthorId != null) {
                boolean b = ctx.hasAccountId() && ctx.getAccountId().equals(productWithAuthorId.asString("author_id"));
                ctx.setSession(KEY, b);
                if (!b)
                    throw new ServiceException(Errors.E_PERMISSION, errorMsg);
            }
        } else {
            boolean b = PrimitiveTypeConverter.toBoolean(ctx.getSession(KEY));
            if (!b)
                throw new ServiceException(Errors.E_PERMISSION, errorMsg);
        }
    }

    protected void checkPermission(final ServiceContext ctx, final String id) {
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                checkPermission(session, ctx, id);
                return null;
            }
        });
    }

    @Override
    public Records listApps(final ServiceContext ctx) {
        Validate.notNull(ctx);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listApps(session, ctx);
            }
        });
    }

    Record getApp(RecordSession session, ServiceContext ctx, String id) {
        return session.selectOne("market.findAppForPublish", CC.map(
                "id=>", id
        ), GenericMapper.get());
    }

    @Override
    public Record getApp(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getApp(session, ctx, id);
            }
        });
    }

    void attachPricetagsToCategory(RecordSession session, ServiceContext ctx, String appId, Record category) {
        if (category == null)
            return;

        Records pricetags = listPricetags(session, ctx, appId, category.asString("category_id"));
        category.put("pricetags", pricetags);
    }

    Records listCategories(RecordSession session, ServiceContext ctx, String appId, boolean withPricetags) {
        Records categories = session.selectList("market.listCategoriesForPublish",
                CC.map("app_id=>", appId), GenericMapper.get());

        if (withPricetags) {
            for (Record category : categories)
                attachPricetagsToCategory(session, ctx, appId, category);
        }

        return categories;
    }

    @Override
    public Records listCategories(final ServiceContext ctx, final String appId, final boolean withPricetags) {
        Validate.notNull(ctx);
        Validate.notNull(appId);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listCategories(session, ctx, appId, withPricetags);
            }
        });
    }

    Record getCategory(RecordSession session, ServiceContext ctx, String appId, String categoryId, boolean withPricetags) {
        Record category = session.selectOne("market.getCategoryForPublish", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId
        ), GenericMapper.get());
        attachPricetagsToCategory(session, ctx, appId, category);
        return category;
    }

    @Override
    public Record getCategory(final ServiceContext ctx, final String appId, final String categoryId, final boolean withPricetags) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(categoryId);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getCategory(session, ctx, appId, categoryId, withPricetags);
            }
        });
    }

    Records listPricetags(RecordSession session, ServiceContext ctx, String appId, String categoryId) {
        return session.selectList("market.listPricetagsForPublish", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId
        ), GenericMapper.get());
    }

    @Override
    public Records listPricetags(final ServiceContext ctx, final String appId, final String categoryId) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(categoryId);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listPricetags(session, ctx, appId, categoryId);
            }
        });
    }

    boolean pricetagExists(RecordSession session, ServiceContext ctx, String appId, String categoryId, String pricetagId) {
        return session.selectBooleanValue("market.pricetagIdExistsForCategory",
                CC.map("app_id=>", appId, "category_id", categoryId, "pricetag_id=>", pricetagId), false);
    }

    void checkPricetagId(RecordSession session, ServiceContext ctx, String appId, String categoryId, String pricetagId) {
        if (pricetagId != null) {
            if (!pricetagExists(session, ctx, appId, categoryId, pricetagId))
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, String.format("Illegal pricetag_id (%s) for %s:%s", pricetagId, appId, categoryId));
        }
    }

    void createProductWithoutGet(RecordSession session, ServiceContext ctx, Record product) {
        String id = product.asString("id");
        String appId = product.asString("app_id");
        String categoryId = product.asString("category_id");

        long now = DateTimeUtils.nowMillis();

        String pricetagId = product.asString("pricetag_id");
        checkPricetagId(session, ctx, appId, categoryId, pricetagId);

        session.insert("market.createProductForPublish", CC.map(
                "id=>", id,
                "pricetag_id=>", pricetagId,
                "category_id=>", product.asString("category_id"),
                "app_id=>", product.asString("app_id"),
                "default_locale=>", product.asString("default_locale"),
                "available_locales=>", product.asString("available_locales"),
                "author_id=>", ctx.getAccountId(),
                "author_name=>", product.asString("author_name"),
                "author_email=>", product.asString("author_email"),
                "author_phone=>", product.asString("author_phone"),
                "author_website=>", product.asString("author_website"),
                "name=>", product.asString("name"),
                "description=>", product.asString("description"),
                "logo_image=>", product.asString("logo_image"),
                "cover_image=>", product.asString("cover_image"),
                "screenshot1_image=>", product.asString("screenshot1_image"),
                "screenshot2_image=>", product.asString("screenshot2_image"),
                "screenshot3_image=>", product.asString("screenshot3_image"),
                "screenshot4_image=>", product.asString("screenshot4_image"),
                "screenshot5_image=>", product.asString("screenshot5_image"),
                "type1=>", product.asString("type1"),
                "type2=>", product.asString("type2"),
                "type3=>", product.asString("type3"),
                "tags=>", product.asString("tags"),
                "page_s=>", product.asString("page_s"),
                "page_m=>", product.asString("page_m"),
                "page_b=>", product.asString("page_b"),
                "now=>", now
        ));
    }

    Record createProduct(RecordSession session, ServiceContext ctx, Record product) {
        createProductWithoutGet(session, ctx, product);
        return getProduct(session, ctx, product.asString("id"), true, true);
    }

    @Override
    public Record createProduct(final ServiceContext ctx, final Record product) {
        Validate.notNull(ctx);
        Validate.notNull(product);
        Validate.isTrue(product.hasField("id"));

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return createProduct(session, ctx, product);
            }
        });
    }

    void updateProductWithoutGet(RecordSession session, ServiceContext ctx, Record product) throws IOException {
        String id = product.asString("id");
        long now = DateTimeUtils.nowMillis();

        Record product0 = getProduct(session, ctx, id, false, false);
        if (product0 == null)
            throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product " + id);

        String pricetagId = product.asString("pricetag_id");
        checkPricetagId(session, ctx,
                product0.asString("app_id"), product0.asString("category_id"), pricetagId);

        String publishChannels = product.asString("publish_channels", null);
        if (publishChannels != null) {
            String[] publishChannelsArray = StringUtils2.splitArray(publishChannels, ',', true);
            for (String channel : publishChannelsArray) {
                if (!ArrayUtils.contains(ALL_PUBCHNLS, channel))
                    throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal publish_channels");
            }
        }

        String logoImageUrl = null;
        if (product.isType("logo_image", CharSequence.class)) {
            logoImageUrl = product.asString("logo_image");
        } else if (product.isType("logo_image", FileItem.class)) {
            logoImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "logo_image", id, product.asFileItem("logo_image"));
        }

        String coverImageUrl = null;
        if (product.isType("cover_image", CharSequence.class)) {
            coverImageUrl = product.asString("cover_image");
        } else if (product.isType("cover_image", FileItem.class)) {
            coverImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "cover_image", id, product.asFileItem("cover_image"));
        }

        String promotionImageUrl = null;
        if (product.isType("promotion_image", CharSequence.class)) {
            promotionImageUrl = product.asString("promotion_image");
        } else if (product.isType("promotion_image", FileItem.class)) {
            promotionImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "promotion_image", id, product.asFileItem("promotion_image"));
        }

        String screenshot1ImageUrl = null;
        if (product.isType("screenshot1_image", CharSequence.class)) {
            screenshot1ImageUrl = product.asString("screenshot1_image");
        } else if (product.isType("screenshot1_image", FileItem.class)) {
            screenshot1ImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "screenshot1_image", id, product.asFileItem("screenshot1_image"));
        }

        String screenshot2ImageUrl = null;
        if (product.isType("screenshot2_image", CharSequence.class)) {
            screenshot2ImageUrl = product.asString("screenshot2_image");
        } else if (product.isType("screenshot2_image", FileItem.class)) {
            screenshot2ImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "screenshot2_image", id, product.asFileItem("screenshot2_image"));
        }

        String screenshot3ImageUrl = null;
        if (product.isType("screenshot3_image", CharSequence.class)) {
            screenshot3ImageUrl = product.asString("screenshot3_image");
        } else if (product.isType("screenshot3_image", FileItem.class)) {
            screenshot3ImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "screenshot3_image", id, product.asFileItem("screenshot3_image"));
        }

        String screenshot4ImageUrl = null;
        if (product.isType("screenshot4_image", CharSequence.class)) {
            screenshot4ImageUrl = product.asString("screenshot4_image");
        } else if (product.isType("screenshot4_image", FileItem.class)) {
            screenshot4ImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "screenshot4_image", id, product.asFileItem("screenshot4_image"));
        }

        String screenshot5ImageUrl = null;
        if (product.isType("screenshot5_image", CharSequence.class)) {
            screenshot5ImageUrl = product.asString("screenshot5_image");
        } else if (product.isType("screenshot5_image", FileItem.class)) {
            screenshot5ImageUrl = FileStorageUtils.saveImageWithFileItem(imageStorage, "screenshot5_image", id, product.asFileItem("screenshot5_image"));
        }

        session.update("market.updateProductForPublish", CC.map(
                "pricetag_id=>", pricetagId,
                "default_locale=>", product.asString("default_locale"),
                "available_locales=>", product.asJoinedString("available_locales", ","),
                "author_name=>", product.asString("author_name"),
                "author_email=>", product.asString("author_email"),
                "author_phone=>", product.asString("author_product"),
                "author_website=>", product.asString("author_website"),
                "name=>", product.asString("name"),
                "description=>", product.asString("description"),
                "tags=>", Tags.trimTags(product.asString("tags")),
                "logo_image=>", logoImageUrl,
                "cover_image=>", coverImageUrl,
                "promotion_image=>", promotionImageUrl,
                "screenshot1_image=>", screenshot1ImageUrl,
                "screenshot2_image=>", screenshot2ImageUrl,
                "screenshot3_image=>", screenshot3ImageUrl,
                "screenshot4_image=>", screenshot4ImageUrl,
                "screenshot5_image=>", screenshot5ImageUrl,
                "type1=>", product.asString("type1"),
                "type2=>", product.asString("type2"),
                "type3=>", product.asString("type3"),
                "tags=>", product.asString("tags"),
                "publish_channels=>", product.asString("publish_channels"),
                "page_b=>", product.asString("page_b"),
                "page_m=>", product.asString("page_m"),
                "page_s=>", product.asString("page_s"),
                "id=>", id,
                "now=>", now
        ));
    }

    Record updateProduct(RecordSession session, ServiceContext ctx, Record product) throws IOException {
        String id = product.asString("id");
        updateProductWithoutGet(session, ctx, product);
        return getProduct(session, ctx, id, true, true);
    }

    @Override
    public Record updateProduct(final ServiceContext ctx, final Record product) {
        Validate.notNull(ctx);
        Validate.notNull(product);
        Validate.isTrue(product.hasField("id"));

        checkAccountId(ctx, authorMustBeBorqsId);
        String id = product.asString("id");
        checkPermission(ctx, id);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return updateProduct(session, ctx, product);
            }
        });
    }

    boolean orderExistsForProduct(RecordSession session, ServiceContext ctx, String id) {
        return session.selectBooleanValue("market.orderExistsForVersion", CC.map("id=>", id), false);
    }

    void deleteProduct(RecordSession session, ServiceContext ctx, String id) {
        boolean orderExists = orderExistsForProduct(session, ctx, id);
        if (orderExists)
            throw new ServiceException(Errors.E_PERMISSION, "Someone has purchased the product");

        session.delete("market.deleteVersionsForPublish", CC.map("id=>", id));
        session.delete("market.deleteProductForPublish", CC.map("id=>", id));
    }

    @Override
    public void deleteProduct(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                deleteProduct(session, ctx, id);
                return null;
            }
        });
    }

    private void attachVersions(RecordSession session, ServiceContext ctx, Record product) {
        if (product == null)
            return;

        String id = product.asString("id");
        Records versions = listVersions(session, ctx, id, true);
        product.set("versions", versions);
    }

    private void attachPricetagsToProduct(RecordSession session, ServiceContext ctx, Record product) {
        if (product == null)
            return;

        String appId = product.asString("app_id");
        String categoryId = product.asString("category_id");
        Records pricetags = listPricetags(session, ctx, appId, categoryId);
        product.set("pricetags", pricetags);
    }

    private void attachCanDeleteToProduct(RecordSession session, ServiceContext ctx, Record product) {
        if (product == null)
            return;

        String id = product.asString("id");
        boolean canDelete = !orderExistsForProduct(session, ctx, id);
        product.set("can_delete", canDelete);
    }

    Record getProduct(RecordSession session, ServiceContext ctx, String id, boolean withVersions, boolean withAvailablePricetags) {
        Record product = session.selectOne("market.findProductByIdForPublish", CC.map("id=>", id), GenericMapper.get());
        if (product != null) {
            if (withVersions)
                attachVersions(session, ctx, product);

            if (withAvailablePricetags)
                attachPricetagsToProduct(session, ctx, product);

            attachCanDeleteToProduct(session, ctx, product);
            urlCompleter.completeUrl(product);
        }
        return product;
    }

    @Override
    public Record getProduct(final ServiceContext ctx, final String id, final boolean withVersions, final boolean withAvailablePricetags) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getProduct(session, ctx, id, withVersions, withAvailablePricetags);
            }
        });
    }


    Records listProducts(RecordSession session, ServiceContext ctx, String appId, String categoryId, boolean withVersions, boolean withAvailablePricetags) {
        Records products = session.selectList("market.listProductsByAuthorForPublish",
                CC.map("app_id=>", appId, "category_id=>", categoryId, "author_id=>", ctx.getAccountId()),
                GenericMapper.get());

        for (Record product : products) {
            if (withVersions)
                attachVersions(session, ctx, product);

            if (withAvailablePricetags)
                attachPricetagsToProduct(session, ctx, product);

            attachCanDeleteToProduct(session, ctx, product);
        }

        urlCompleter.completeUrl(products);
        return products;
    }

    @Override
    public Records listProducts(final ServiceContext ctx, final String appId, final String categoryId, final boolean withVersions, final boolean withAvailablePricetags) {
        Validate.notNull(ctx);
        Validate.notNull(appId);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listProducts(session, ctx, appId, categoryId, withVersions, withAvailablePricetags);
            }
        });
    }

    void createVersionWithoutGet(RecordSession session, ServiceContext ctx, Record productVersion) {
        long now = DateTimeUtils.nowMillis();

        String id = productVersion.asString("id");
        int version = productVersion.asInt("version");

        Record productWithMaxVersion = session.selectOne("market.getMaxProductVersionForPublish", CC.map("id=>", id));
        if (productWithMaxVersion != null) {
            int maxVersion = productWithMaxVersion.asInt("max_version");
            if (version <= maxVersion)
                throw new ServiceException(Errors.E_ILLEGAL_VERSION, "Version must > " + maxVersion);
        }

        session.insert("market.createVersionForPublish", CC.map(
                "id=>", id,
                "version=>", version,
                "status=>", initVersionStatus,
                "min_app_version=>", productVersion.asInt("min_app_version"),
                "max_app_version=>", productVersion.asInt("max_app_version", Integer.MAX_VALUE),
                "supported_mod=>", productVersion.asJoinedString("supported_mod", ","),
                "action=>", productVersion.asInt("action", PV_ACTION_NONE),
                "url=>", productVersion.asString("url"),
                "file_size=>", productVersion.asLongObject("file_size"),
                "file_md5=>", productVersion.asString("file_md5", ""),
                "version_name=>", productVersion.asString("version_name"),
                "recent_change=>", productVersion.asString("recent_change"),
                "dependencies=>", productVersion.asString("dependencies"),
                "now=>", now,
                "as_beta=>", productVersion.asInt("as_beta")
        ));

        session.update("market.updateLastVersionCreatedAtForPublish", CC.map(
                "id=>", id,
                "now=>", now
        ));
    }

    Record createVersion(RecordSession session, ServiceContext ctx, Record productVersion) {
        createVersionWithoutGet(session, ctx, productVersion);
        return getVersion(session, ctx, productVersion.asString("id"), productVersion.asInt("version"));
    }

    @Override
    public Record createVersion(final ServiceContext ctx, final Record versionRec) {
        Validate.notNull(ctx);
        Validate.notNull(versionRec);
        Validate.isTrue(versionRec.hasField("id") && versionRec.hasField("version"));

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return createVersion(session, ctx, versionRec);
            }
        });
    }

    void updateVersionWithoutGet(RecordSession session, ServiceContext ctx, Record versionRec) {
        String id = versionRec.asString("id");
        int version = versionRec.asInt("version");
        long now = DateTimeUtils.nowMillis();

        session.update("market.updateVersionForPublish", CC.map(
                "status=>", versionRec.asIntObject("status"),
                "min_app_version=>", versionRec.asIntObject("min_app_version"),
                "max_app_version=>", versionRec.asIntObject("max_app_version"),
                "supported_mod=>", versionRec.asString("supported_mod"),
                "action=>", versionRec.asIntObject("action"),
                "url=>", versionRec.asString("url"),
                "file_size=>", versionRec.asLongObject("file_size"),
                "file_md5=>", versionRec.asString("file_md5"),
                "version_name=>", versionRec.asString("version_name"),
                "recent_change=>", versionRec.asString("recent_change"),
                "dependencies=>", versionRec.asString("dependencies"),
                "id=>", id,
                "version=>", version,
                "now=>", now
        ));
    }

    Record updateVersion(RecordSession session, ServiceContext ctx, Record versionRec) {
        String id = versionRec.asString("id");
        updateVersionWithoutGet(session, ctx, versionRec);
        return getVersion(session, ctx, id, versionRec.asInt("version"));
    }

    @Override
    public Record updateVersion(final ServiceContext ctx, final Record versionRec) {
        Validate.notNull(ctx);
        Validate.notNull(versionRec);
        Validate.isTrue(versionRec.hasField("id") && versionRec.hasField("version"));

        checkAccountId(ctx, authorMustBeBorqsId);
        String id = versionRec.asString("id");
        checkPermission(ctx, id);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return updateVersion(session, ctx, versionRec);
            }
        });
    }

    boolean orderExistsForVersion(RecordSession session, ServiceContext ctx, String id, int version) {
        return session.selectBooleanValue("market.orderExistsForVersion",
                CC.map("id=>", id, "version=>", version), false);
    }

    void deleteVersion(RecordSession session, ServiceContext ctx, String id, int version) {
        boolean orderExists = orderExistsForVersion(session, ctx, id, version);
        if (orderExists)
            throw new ServiceException(Errors.E_PERMISSION, "Someone has purchased the version of product");

        session.delete("market.deleteVersionForPublish", CC.map("id=>", id, "version=>", version));
    }


    @Override
    public void deleteVersion(final ServiceContext ctx, final String id, final int version) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                deleteVersion(session, ctx, id, version);
                return null;
            }
        });
    }

    Record getVersion(RecordSession session, ServiceContext ctx, String id, int version) {
        Record versionRec = session.selectOne("market.findVersionForPublish",
                CC.map("id=>", id, "version=>", version), GenericMapper.get());

        attachCanDeleteToVersion(session, ctx, id, versionRec);
        urlCompleter.completeUrl(versionRec);
        return versionRec;
    }

    @Override
    public Record getVersion(final ServiceContext ctx, final String id, final int version) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getVersion(session, ctx, id, version);
            }
        });
    }

    void attachCanDeleteToVersion(RecordSession session, ServiceContext ctx, String id, Record versionRec) {
        if (versionRec == null)
            return;

        int version = versionRec.asInt("version");
        boolean canDelete = !orderExistsForVersion(session, ctx, id, version);
        versionRec.set("can_delete", canDelete);
    }

    Records listVersions(RecordSession session, ServiceContext ctx, String id, boolean simple) {
        Records versions = session.selectList("market.listVersionsForPublish",
                CC.map("id=>", id, "simple=>", simple), GenericMapper.get());
        for (Record versionRec : versions) {
            attachCanDeleteToVersion(session, ctx, id, versionRec);
        }
        Collections.sort(versions, new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                Integer v1 = o1.asInt("version");
                Integer v2 = o2.asInt("version");
                return v2.compareTo(v1);
            }
        });
        urlCompleter.completeUrl(versions);
        return versions;
    }



    @Override
    public Records listVersions(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listVersions(session, ctx, id, false);
            }
        });
    }

    private boolean activeVersion(RecordSession session, ServiceContext ctx, String id, Integer version, boolean active) {
        session.update("market.activeVersionForPublish", CC.map(
                "product_id=>", id,
                "version=>", version,
                "active=>", active)
        );
        Record statusRec = session.selectOne("market.getVersionStatusForPublish", CC.map(
                "id=>", id,
                "version=>", version
        ));
        if (statusRec == null)
            return false;

        int status = statusRec.asInt("status");
        return (status & PV_STATUS_ACTIVE) != 0;
    }

    @Override
    public Record activeVersion(final ServiceContext ctx, final String id, final Integer version, final boolean active) {

        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, authorMustBeBorqsId);
        checkPermission(ctx, id);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                boolean r = activeVersion(session, ctx, id, version, active);
                return Record.of("id=>", id, "version=>", version, "active=>", r);
            }
        });
    }

    void releaseVersion(RecordSession session, ServiceContext ctx, String id, int version) {
        session.update("market.updateVersionForPublish", CC.map(
                "now=>", DateTimeUtils.nowMillis(),
                "id=>", id,
                "version=>", version,
                "as_beta=>", 0
        ));
    }

    @Override
    public void releaseVersion(final ServiceContext ctx, final String id, final int version) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                releaseVersion(session, ctx, id, version);
                return null;
            }
        });
    }

    Record uploadProduct(RecordSession session, ServiceContext ctx, ResourceFile productFile, boolean createProduct, Params params) throws IOException, NoSuchAlgorithmException {
        String id = productFile.getId();
        int version = productFile.getVersion();
        String appId = productFile.getAppId();
        String categoryId = productFile.getCategory();

        if (!ValidateUtils.validateProductId(id))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, String.format("Illegal id (%s) in manifest", id));
        if (version <= 0)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, String.format("Illegal version (%s) in manifest", version));
        if (!ValidateUtils.validateAppId(appId))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, String.format("Illegal appId (%s) in manifest", appId));
        if (!ValidateUtils.validateAppId(categoryId))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, String.format("Illegal category (%s) in manifest", categoryId));

        String qualifiedAppId = params.param("qualified_app_id").asString(null);
        if (qualifiedAppId != null && !appId.equals(qualifiedAppId))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "appId != " + qualifiedAppId);

        String qualifiedCategoryId = params.param("qualified_category_id").asString(null);
        if (qualifiedCategoryId != null && !categoryId.equals(qualifiedCategoryId))
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "category != " + qualifiedCategoryId);


        String logoImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "logo_image", id,
                params.param("logo_image").asFileItem(), productFile.readLogo(), productFile.getLogoPath());
        String coverImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "cover_image", id,
                params.param("cover_image").asFileItem(), productFile.readCover(), productFile.getCoverPath());
        String s1ImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "screenshot1_image", id,
                params.param("screenshot1_image").asFileItem(), productFile.readScreenshot1(), productFile.getScreenshot1Path());
        String s2ImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "screenshot2_image", id,
                params.param("screenshot2_image").asFileItem(), productFile.readScreenshot2(), productFile.getScreenshot2Path());
        String s3ImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "screenshot3_image", id,
                params.param("screenshot3_image").asFileItem(), productFile.readScreenshot3(), productFile.getScreenshot3Path());
        String s4ImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "screenshot4_image", id,
                params.param("screenshot4_image").asFileItem(), productFile.readScreenshot4(), productFile.getScreenshot4Path());
        String s5ImageUrl = FileStorageUtils.saveImageWithFileItemOrContent(imageStorage, "screenshot5_image", id,
                params.param("screenshot5_image").asFileItem(), productFile.readScreenshot5(), productFile.getScreenshot5Path());


        String productFileUrl = FileStorageUtils.saveProduct(productStorage, productFile.getFile(), id, version);


        Record productWithAuthor = session.selectOne("market.getProductAuthorIdForPublish", CC.map("id=>", id));
        String productAuthorId = productWithAuthor != null ? productWithAuthor.asString("author_id") : null;
        if (productWithAuthor == null) {
            if (!createProduct)
                throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Can't create product by upload file (create_product=0)");

            String name = JsonUtils.toJson(productFile.getName(), false);
            String desc = params.param("description").asString(JsonUtils.toJson(productFile.getDescription(), false));
            if (!ValidateUtils.validateMultipleLocaleText(name))
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal name " + name);
            if (!ValidateUtils.validateMultipleLocaleText(desc))
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal description " + desc);

            createProductWithoutGet(session, ctx, Record.of(
                    "id=>", id,
                    "pricetag_id=>", params.param("pricetag_id").asString(),
                    "category_id=>", categoryId,
                    "app_id=>", appId,
                    "default_locale=>", params.param("default_locale").asString(productFile.getDefaultLanguage()),
                    "available_locales=>", params.param("available_locales").asString(),
                    "author_name=>", params.param("author_name").asString(productFile.getAuthorName()),
                    "author_email=>", params.param("author_email").asString(productFile.getAuthorEmail()),
                    "author_phone=>", params.param("author_phone").asString(productFile.getAuthorPhone()),
                    "author_website=>", params.param("author_website").asString(productFile.getAuthorWebsite()),
                    "name=>", name,
                    "description=>", desc,
                    "type1=>", params.param("type1").asString(),
                    "type2=>", params.param("type2").asString(),
                    "type3=>", params.param("type3").asString(),
                    "tags=>", Tags.trimTags(productFile.getTags()),
                    "page_s=>", params.param("page_s").asString(),
                    "page_m=>", params.param("page_m").asString(),
                    "page_b=>", params.param("page_b").asString(),
                    "logo_image=>", logoImageUrl,
                    "cover_image=>", coverImageUrl,
                    "screenshot1_image=>", s1ImageUrl,
                    "screenshot2_image=>", s2ImageUrl,
                    "screenshot3_image=>", s3ImageUrl,
                    "screenshot4_image=>", s4ImageUrl,
                    "screenshot5_image=>", s5ImageUrl
            ));
            productAuthorId = ctx.getAccountId();
        }

        if (!ctx.getAccountId().equals(productAuthorId))
            throw new ServiceException(Errors.E_PERMISSION, "Can't publish a new version for product " + id);


        String versionName = params.param("version_name").asString(JsonUtils.toJson(productFile.getVersionName(), false));
        String recentChange = params.param("recent_change").asString(JsonUtils.toJson(productFile.getRecentChange(), false));
        String dependencies = JsonUtils.toJson(productFile.getDependencies(), false);
        int asBeta = params.param("as_beta").asInt(0) != 0 ? 1 : 0;
        createVersionWithoutGet(session, ctx, Record.of(
                "id=>", id,
                "version=>", version,
                "min_app_version=>", productFile.getMinAppVersion(),
                "max_app_version=>", productFile.getMaxAppVersion(),
                "supported_mod=>", productFile.getSupportedMod(),
                "action=>", PV_ACTION_DOWNLOAD,
                "url=>", productFileUrl,
                "file_size=>", FileUtils2.getFileSize(productFile.getPath()),
                "file_md5=>", FileUtils2.getFileMd5(productFile.getPath()),
                "version_name=>", versionName,
                "recent_change=>", recentChange,
                "dependencies=>", dependencies,
                "as_beta=>", asBeta
        ));

        // TODO: change product for screenshot, description...

        Record product = getProduct(session, ctx, id, true, true);
        Records versions = (Records) product.get("versions");
        for (Record pv : versions) {
            if (pv.asInt("version") == version)
                pv.put("just_uploaded", true);
        }
        return product;
    }

    @Override
    public Record uploadProduct(final ServiceContext ctx, final ResourceFile productFile, final boolean createProduct, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(productFile);
        Validate.notNull(params);
        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return uploadProduct(session, ctx, productFile, createProduct, params);
            }
        });
    }

    CheckResult checkProductForUpload(RecordSession session, ServiceContext ctx, ResourceFile productFile, boolean forCreateProduct, Params params) {
        String qualifiedAppId = params.param("qualified_app_id").asString("");
        String qualifiedProductId = params.param("qualified_product_id").asString("");

        CheckResult cr = new CheckResult();

        // share variables
        Record app = null;
        Record category = null;
        Record product = null;

        // app_id
        String appId = productFile.getAppId();
        cr.addField("app_id", appId);
        if (StringUtils.isNotBlank(appId)) {
            if (!qualifiedAppId.isEmpty() && !qualifiedAppId.equals(appId)) {
                cr.addFieldError("app_id", appId, String.format("Must be %s", qualifiedAppId));
            }
            app = getApp(session, ctx, appId);
            if (app == null) {
                cr.addFieldError("app_id", appId, "Illegal app_id");
            }
        } else {
            cr.addFieldError("app_id", appId, "Missing");
        }

        // category
        String categoryId = productFile.getCategory();
        cr.addField("category_id", categoryId);
        if (StringUtils.isNotBlank(categoryId)) {
            category = getCategory(session, ctx, appId, categoryId, false);
            if (category == null)
                cr.addFieldError("category_id", categoryId, "Illegal category");
        } else {
            cr.addFieldError("category_id", categoryId, "Missing");
        }


        // product id
        String id = productFile.getId();
        cr.addField("id", id);
        if (StringUtils.isNotBlank(id)) {
            if (ValidateUtils.validateProductId(id)) {
                if (!qualifiedProductId.isEmpty() && !qualifiedProductId.equals(id))
                    cr.addFieldError("id", id, String.format("id must be %s", qualifiedProductId));

                product = getProduct(session, ctx, id, true, false);
                if (forCreateProduct) {
                    if (product != null)
                        cr.addFieldError("id", id, "Product exists");
                } else {
                    if (product == null) {
                        cr.addFieldError("id", id, "Illegal product");
                    } else {
                        if (!product.asString("author_id").equals(ctx.getAccountId()))
                            cr.addFieldError("id", id, "Permission error");
                    }
                }
            } else {
                cr.addFieldError("id", id, "Illegal format");
            }
        } else {
            cr.addFieldError("id", id, "Missing");
        }

        // version
        int version = productFile.getVersion();
        cr.addField("version", version);
        if (version > 0) {
            if (!forCreateProduct && product != null) {
                Records versions = (Records) product.get("versions");
                if (CollectionUtils.isNotEmpty(versions)) {
                    int maxVersion = versions.get(0).asInt("version");
                    if (version <= maxVersion)
                        cr.addFieldError("version", version, String.format("Too small, must > %s", maxVersion));
                }
            }
        } else {
            cr.addFieldError("version", version, "Missing or too small");
        }

        // name
        JsonNode name = productFile.getName();
        cr.addField("name", name);
        if (name != null && name.isObject() && name.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(name))
                cr.addFieldError("name", name, "Illegal format");
        } else {
            cr.addFieldError("name", name, "Missing");
        }

        // version_name
        JsonNode versionName = productFile.getVersionName();
        cr.addField("version_name", versionName);
        if (name != null && name.isObject() && name.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(versionName))
                cr.addFieldError("version_name", versionName, "Illegal format");
        } else {
            cr.addFieldError("version_name", versionName, "Missing");
        }

        // author_name
        String authorName = productFile.getAuthorName();
        cr.addField("author_name", authorName);
        if (StringUtils.isBlank(authorName)) {
            cr.addFieldError("author_name", authorName, "Missing");
        }

        // tags
        String tags = productFile.getTags();
        cr.addField("tags", tags);

        // recent_change
        JsonNode recentChange = params.param("recent_change").asJson();
        cr.addField("recent_change", recentChange);
        if (recentChange != null && recentChange.isObject() && recentChange.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(recentChange))
                cr.addFieldError("recent_change", recentChange, "Illegal format");
        }


        // description
        JsonNode description = productFile.getDescription();
        cr.addField("description", description);
        if (description != null && description.isObject() && description.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(description))
                cr.addFieldError("description", name, "Illegal format");
        }


        // min_app_version
        int minAppVersion = productFile.getMinAppVersion();
        cr.addField("min_app_version", minAppVersion);
        if (minAppVersion < 0) {
            cr.addFieldError("min_app_version", minAppVersion, "> 0");
        }

        // max_app_version
        int maxAppVersion = productFile.getMaxAppVersion();
        cr.addField("max_app_version", maxAppVersion);
        if (maxAppVersion <= 0) {
            cr.addFieldError("max_app_version", maxAppVersion, "> 0");
        }
        if (minAppVersion > 0 && maxAppVersion > 0 && maxAppVersion < minAppVersion) {
            cr.addFieldError("max_app_version", maxAppVersion, "> " + minAppVersion);
        }


        // supported_mod
        String[] supportedMod = productFile.getSupportedMod();
        cr.addField("supported_mod", supportedMod);
        if (category != null) {
            String availableModsStr = category.asString("available_mods", "");
            Set<String> availableMods = StringUtils2.splitSet(availableModsStr, ',', true);

            if (!availableMods.isEmpty()) {
                if (ArrayUtils.isEmpty(supportedMod)) {
                    cr.addFieldError("supported_mod", supportedMod, "Missing");
                } else {
                    for (String mod : supportedMod) {
                        if (!availableMods.contains(mod)) {
                            cr.addFieldError("supported_mod", supportedMod, String.format("Illegal mod '%s'", mod));
                            break;
                        }
                    }
                }
            } else {
                if (ArrayUtils.isNotEmpty(supportedMod))
                    cr.addFieldError("supported_mod", supportedMod, "Must be empty");
            }
        }

        // logo_image
        String logoPath = productFile.getLogoPath();
        cr.addField("logo_image", logoPath);
        if (!productFile.logoExists()) {
            cr.addFieldError("logo_image", logoPath, "Missing file");
        }


        // cover_image
        String coverPath = productFile.getCoverPath();
        cr.addField("cover_image", coverPath);
        if (!productFile.coverExists()) {
            cr.addFieldError("cover_image", coverPath, "Missing file");
        }

        // screenshot1_image
        String screenshot1Path = productFile.getScreenshot1Path();
        cr.addField("screenshot1_image", screenshot1Path);
        if (StringUtils.isNotBlank(screenshot1Path) && !productFile.screenshot1Exists()) {
            cr.addFieldError("screenshot1_image", screenshot1Path, "Missing file");
        }

        // screenshot2_image
        String screenshot2Path = productFile.getScreenshot2Path();
        cr.addField("screenshot2_image", screenshot2Path);
        if (StringUtils.isNotBlank(screenshot2Path) && !productFile.screenshot2Exists()) {
            cr.addFieldError("screenshot2_image", screenshot2Path, "Missing file");
        }

        // screenshot3_image
        String screenshot3Path = productFile.getScreenshot3Path();
        cr.addField("screenshot3_image", screenshot3Path);
        if (StringUtils.isNotBlank(screenshot3Path) && !productFile.screenshot3Exists()) {
            cr.addFieldError("screenshot3_image", screenshot3Path, "Missing file");
        }

        // screenshot4_image
        String screenshot4Path = productFile.getScreenshot4Path();
        cr.addField("screenshot4_image", screenshot4Path);
        if (StringUtils.isNotBlank(screenshot4Path) && !productFile.screenshot4Exists()) {
            cr.addFieldError("screenshot4_image", screenshot4Path, "Missing file");
        }

        // screenshot5_image
        String screenshot5Path = productFile.getScreenshot5Path();
        cr.addField("screenshot5_image", screenshot5Path);
        if (StringUtils.isNotBlank(screenshot5Path) && !productFile.screenshot5Exists()) {
            cr.addFieldError("screenshot5_image", screenshot5Path, "Missing file");
        }

        // author_phone
        String authorPhone = productFile.getAuthorPhone();
        cr.addField("author_phone", authorPhone);

        // author_email
        String authorEmail = productFile.getAuthorEmail();
        cr.addField("author_email", authorEmail);

        // author_website
        String authorWebsite = productFile.getAuthorWebsite();
        cr.addField("author_website", authorWebsite);

        // TODO: dependencies
        return cr;
    }


    @Override
    public CheckResult checkProductForUpload(final ServiceContext ctx, final ResourceFile productFile, final boolean forCreateProduct, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(productFile);
        Validate.notNull(params);

        checkAccountId(ctx, authorMustBeBorqsId);
        return openSession(new RecordSessionHandler<CheckResult>() {
            @Override
            public CheckResult handle(RecordSession session) throws Exception {
                return checkProductForUpload(session, ctx, productFile, forCreateProduct, params);
            }
        });
    }

    public static Object getFileParam(Params params, String field) {
        if (params.param("delimg_" + field).asString("").equalsIgnoreCase("delete")) {
            return "";
        } else {
            return params.param(field).asFileItem();
        }
    }

    private void checkImage(CheckResult cr, Params params, String field, String current) {
        Object val = getFileParam(params, field);
        if ("".equals(val)) {
            if ("logo_image".equals(field) || "cover_image".equals(field))
                cr.addFieldError(field, val, "Missing");
        } else if (val instanceof FileItem) {
            FileItem fi = (FileItem) val;
            String err = null;
            if ("logo_image".equals(field)) {
                err = checkLogoImageSize(fi);
            } else if ("cover_image".equals(field)) {
                err = checkCoverImageSize(fi);
            } else if ("promotion_image".equals(field)) {
                err = checkPromotionImageSize(fi);
            } else if (field.startsWith("screenshot")) {
                err = checkScreenshotImageSize(fi);
            }
            if (err != null)
                cr.addFieldError(field, current, err);
        }
    }

    private String checkLogoImageSize(FileItem fi) {
        // TODO: add limit for size
        return null;
    }

    private String checkCoverImageSize(FileItem fi) {
        // TODO: add limit for size
        return null;
    }

    private String checkPromotionImageSize(FileItem fi) {
        // TODO: add limit for size
        return null;
    }

    private String checkScreenshotImageSize(FileItem fi) {
        // TODO: add limit for size
        return null;
    }


    CheckResult checkProductForUpdate(RecordSession session, ServiceContext ctx, String id, Params params, Record current) {
        CheckResult cr = new CheckResult();
        // id
        cr.addField("id", id);

        // app_id
        cr.addField("app_id", params.param("app_id").asString());

        // category_id
        cr.addField("category_id", params.param("category_id").asString());

        // name
        JsonNode name = params.param("name").asJson();
        cr.addField("name", name);
        if (name != null && name.isObject() && name.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(name))
                cr.addFieldError("name", name, "Illegal format");
        } else {
            cr.addFieldError("name", name, "Missing");
        }

        // description
        JsonNode description = params.param("description").asJson();
        cr.addField("description", description);
        if (description != null && description.isObject() && description.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(description))
                cr.addFieldError("description", name, "Illegal format");
        }

        // author_name
        String authorName = params.param("author_name").asString();
        cr.addField("author_name", authorName);
        if (StringUtils.isBlank(authorName)) {
            cr.addFieldError("author_name", authorName, "Missing");
        }

        // author_email
        String authorEmail = params.param("author_email").asString();
        cr.addField("author_email", authorEmail);

        // author_phone
        String authorPhone = params.param("author_phone").asString();
        cr.addField("author_phone", authorPhone);

        // author_website
        String authorWebsite = params.param("author_website").asString();
        cr.addField("author_website", authorWebsite);

        // tags
        String tags = params.param("tags").asString();
        cr.addField("tags", tags);

        final String[] imageFields = {
                "logo_image",
                "cover_image",
                "promotion_image",
                "screenshot1_image",
                "screenshot2_image",
                "screenshot3_image",
                "screenshot4_image",
                "screenshot5_image",
        };

        for (String imageField : imageFields) {
            cr.addField(imageField, current.asString(imageField));
            checkImage(cr, params, imageField, current.asString(imageField));
        }

        return cr;
    }


    @Override
    public CheckResult checkProductForUpdate(final ServiceContext ctx, final String id, final Params params, final Record current) {
        Validate.notNull(ctx);
        Validate.notNull(id);
        Validate.notNull(params);

        return openSession(new RecordSessionHandler<CheckResult>() {
            @Override
            public CheckResult handle(RecordSession session) throws Exception {
                return checkProductForUpdate(session, ctx, id, params, current);
            }
        });
    }

    CheckResult checkVersionForUpdate(RecordSession session, ServiceContext ctx, String id, int version, Params params, Record current) {
        CheckResult cr = new CheckResult();

        // id
        cr.addField("id", id);

        // version
        cr.addField("version", version);

        // min_app_version
        int minAppVersion = params.param("min_app_version").asInt(0);
        cr.addField("min_app_version", minAppVersion);

        // max_app_version
        int maxAppVersion = params.param("max_app_version").asInt(Integer.MAX_VALUE);
        cr.addField("max_app_version", maxAppVersion);

        // supported_mod
        String supportedMod = params.param("supported_mod").asString();
        cr.addField("supported_mod", supportedMod);

        // version_name
        JsonNode versionName = params.param("version_name").asJson();
        cr.addField("version_name", versionName);
        if (versionName != null && versionName.isObject() && versionName.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(versionName))
                cr.addFieldError("version_name", versionName, "Illegal format");
        } else {
            cr.addFieldError("version_name", versionName, "Missing");
        }

        // recent_change
        JsonNode recentChange = params.param("recent_change").asJson();
        cr.addField("recent_change", recentChange);
        if (recentChange != null && recentChange.isObject() && recentChange.size() > 0) {
            if (!ValidateUtils.validateMultipleLocaleText(recentChange))
                cr.addFieldError("recent_change", recentChange, "Illegal format");
        }
        return cr;
    }

    @Override
    public CheckResult checkVersionForUpdate(final ServiceContext ctx, final String id, final int version, final Params params, final Record current) {
        Validate.notNull(ctx);
        Validate.notNull(id);
        Validate.notNull(params);

        return openSession(new RecordSessionHandler<CheckResult>() {
            @Override
            public CheckResult handle(RecordSession session) throws Exception {
                return checkVersionForUpdate(session, ctx, id, version, params, current);
            }
        });
    }
}
