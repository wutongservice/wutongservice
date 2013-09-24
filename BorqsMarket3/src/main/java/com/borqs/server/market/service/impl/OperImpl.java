package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.MLTexts;
import com.borqs.server.market.models.ValidateUtils;
import com.borqs.server.market.service.OperService;
import com.borqs.server.market.sfs.FileContent;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service("service.defaultOperService")
public class OperImpl extends ServiceSupport implements OperService {

    private AccountImpl account;
    private PublishImpl publish;
    private FileStorage partitionStorage;

    public OperImpl() {
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccount(AccountImpl account) {
        this.account = account;
    }

    @Autowired
    @Qualifier("service.defaultPublishService")
    public void setPublish(PublishImpl publish) {
        this.publish = publish;
    }

    @Autowired
    @Qualifier("storage.partition")
    public void setPartitionStorage(FileStorage partitionStorage) {
        this.partitionStorage = partitionStorage;
    }

    protected void checkPermission(RecordSession session, ServiceContext ctx, String appId) {
        Record appRec = session.selectOne("market.findAppOperIdForOper", CC.map("app_id=>", appId));
        if (appRec == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal app " + appId);

        String operId = appRec.asString("operator_id");
        if (StringUtils.isBlank(operId)) {
            operId = appRec.asString("creator_id", "");
        }
        if (!operId.equals(ctx.getAccountId()))
            throw new ServiceException(Errors.E_PERMISSION, "The current account is not the operator for the app " + appId);
    }

    protected void checkPermission(final ServiceContext ctx, final String appId) {
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                checkPermission(session, ctx, appId);
                return null;
            }
        });
    }

    Records listApps(RecordSession session, ServiceContext ctx) {
        Records apps = session.selectList("market.listAppsForOper", CC.map(
                "account_id=>", ctx.getAccountId()
        ), GenericMapper.get());
        //localeSelector.selectLocale(apps, ctx);
        return apps;
    }

    @Override
    public Records listCategories(final ServiceContext ctx, final String appId) {
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return publish.listCategories(session, ctx, appId, false);
            }
        });
    }

    @Override
    public Records listApps(final ServiceContext ctx) {
        Validate.notNull(ctx);

        checkAccountId(ctx, false);
        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listApps(session, ctx);
            }
        });
    }


    RecordsWithTotal listPartitions(RecordSession session, ServiceContext ctx, String appId, String categoryId, Params params) {
        Paging paging = params.getPaging(DEFAULT_PARTITION_COUNT_PER_PAGE);
        RecordsWithTotal partitions = session.selectListWithTotal("market.listPartitionsForOper", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId,
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ), GenericMapper.get());
        //localeSelector.selectLocale(partitions, ctx);
        urlCompleter.completeUrl(partitions);
        return partitions;
    }

    @Override
    public RecordsWithTotal listPartitions(final ServiceContext ctx, final String appId, final String categoryId, final Params options) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(categoryId);

        checkAccountId(ctx, false);
        checkPermission(ctx, appId);

        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listPartitions(session, ctx, appId, categoryId, options);
            }
        });
    }

    Record getPartition(RecordSession session, ServiceContext ctx, String id, boolean checkPermission) {
        Record partition = session.selectOne("market.findPartitionForOper", CC.map("id=>", id), GenericMapper.get());
        if (partition != null) {
            if (checkPermission) {
                checkPermission(session, ctx, partition.asString("app_id"));
            }
            //localeSelector.selectLocale(partition, ctx);
            urlCompleter.completeUrl(partition);
        }
        return partition;
    }

    Record getPartition(RecordSession session, ServiceContext ctx, String id) {
        return getPartition(session, ctx, id, true);
    }

    @Override
    public Record getPartition(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getPartition(session, ctx, id);
            }
        });
    }

    Record addPartition(RecordSession session, ServiceContext ctx, String appId, String categoryId, Params params) throws IOException {
        Record category = session.selectOne("market.findCategoryForOper", CC.map("app_id=>", appId, "category_id=>", categoryId));
        if (category == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal app_id with category_id");

        long now = DateTimeUtils.nowMillis();
        String partitionId = "pn_" + RandomUtils2.randomLongWith(now);
        String name = params.param("name").asString("{}");
        String desc = params.param("description").asString("{}");

        int type;
        String rule = null, list = null;
        if (params.hasParam("list")) {
            type = PTT_LIST;
            list = params.param("list").asString("");
        } else if (params.hasParam("rule")) {
            type = PTT_RULE;
            rule = params.param("rule").asString("");
        } else {
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing list or rule");
        }

        saveLogoImage(params, partitionId);

        int n = session.insert("market.insertPartitionForOper", CC.map(
                "id=>", partitionId,
                "now=>", now,
                "creator_id=>", ctx.getAccountId(),
                "app_id=>", appId,
                "category_id=>", categoryId,
                "name=>", name,
                "description=>", desc,
                "logo_image=>", params.param("logo_image").asString(""),
                "status=>", PTS_DEACTIVE,
                "type=>", type,
                "list=>", list,
                "rule=>", rule
        ));

        if (n > 0) {
            return getPartition(session, ctx, partitionId);
        } else {
            throw new ServiceException(Errors.E_PARTITION, "Create partition error");
        }
    }

    @Override
    public Record addPartition(final ServiceContext ctx, final String appId, final String categoryId, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(categoryId);
        Validate.notNull(params);

        checkAccountId(ctx, false);
        checkPermission(ctx, appId);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return addPartition(session, ctx, appId, categoryId, params);
            }
        });
    }

    void deletePartition(RecordSession session, ServiceContext ctx, String id) {
        Record partition = getPartition(session, ctx, id);
        if (partition == null)
            return;

        checkPermission(session, ctx, partition.asString("app_id"));
        session.delete("market.deletePartitionForOper", CC.map("id=>", id));
    }

    @Override
    public void deletePartition(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        checkAccountId(ctx, false);

        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                deletePartition(session, ctx, id);
                return null;
            }
        });
    }

    private void saveLogoImage(Params params, String partitionId) throws IOException {
        Object o = params.get("logo_image");
        if (params.param("delimg_logo_image").asBoolean(false)) {
            params.put("logo_image", "");
        } else {
            if (o instanceof FileItem) {
                FileItem fileItem = (FileItem) o;
                FileContent fc = FileContent.createWithFileItem(fileItem).withFilename(fileItem.getName());
                String fileId = "partition-" + partitionId + "-logo." + FilenameUtils.getExtension(fileItem.getName());
                fileId = partitionStorage.write(fileId, fc);
                params.put("logo_image", fileId);
            }
        }
    }

    Record updatePartition(final RecordSession session, final ServiceContext ctx, final String id, final Params params) throws IOException {
        Record partition = session.selectOne("market.findPartitionForOper", CC.map("id=>", id), GenericMapper.get());
        if (partition == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal partition");


        checkPermission(session, ctx, partition.asString("app_id"));

        saveLogoImage(params, id);

        session.update("market.updatePartitionForOper", CC.map(
                "id=>", id,
                "now=>", DateTimeUtils.nowMillis(),
                "name=>", params.param("name").asString(),
                "description=>", params.param("description").asString(),
                "logo_image=>", params.param("logo_image").asString()
        ));
        return getPartition(session, ctx, id, false);
    }

    @Override
    public Record updatePartition(final ServiceContext ctx, final String id, final Params params) {
        Validate.notNull(ctx);
        Validate.notNull(id);
        Validate.notNull(params);

        checkAccountId(ctx, false);
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return updatePartition(session, ctx, id, params);
            }
        });
    }

    Record setPartitionList(RecordSession session, ServiceContext ctx, String id, List<String> productIds) {
        Record partition = session.selectOne("market.findPartitionForOper", CC.map("id=>", id), GenericMapper.get());
        if (partition == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal partition");

        String joinedProductIds;
        if (productIds == null || productIds.isEmpty()) {
            joinedProductIds = "";
        } else {
            Records existedProducts = session.selectList("market.filterProductIdsForOper", CC.map(
                    "app_id=>", partition.asString("app_id"),
                    "category_id=>", partition.asString("category_id"),
                    "product_ids=>", productIds,
                    "joined_product_ids=>", StringUtils.join(productIds, ",")
            ));
            if (existedProducts.isEmpty()) {
                joinedProductIds = "";
            } else {
                joinedProductIds = existedProducts.join("id", ",");
            }
        }

        checkPermission(session, ctx, partition.asString("app_id"));
        session.update("market.updatePartitionForOper", CC.map(
                "id=>", id,
                "now=>", DateTimeUtils.nowMillis(),
                "type=>", PTT_LIST,
                "rule=>", "",
                "list=>", joinedProductIds
        ));
        return getPartition(session, ctx, id, false);
    }

    @Override
    public Record setPartitionList(final ServiceContext ctx, final String id, final List<String> productIds) {
        Validate.notNull(ctx);
        Validate.notNull(id);
        Validate.notNull(productIds);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return setPartitionList(session, ctx, id, productIds);
            }
        });
    }

    Record setPartitionRule(RecordSession session, ServiceContext ctx, String id, String rule) {
        Record partition = session.selectOne("market.findPartitionForOper", CC.map("id=>", id), GenericMapper.get());
        if (partition == null)
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal partition");

        checkPermission(session, ctx, partition.asString("app_id"));
        session.update("market.updatePartitionForOper", CC.map(
                "id=>", id,
                "now=>", DateTimeUtils.nowMillis(),
                "type=>", PTT_RULE,
                "rule=>", rule,
                "list=>", ""
        ));
        return getPartition(session, ctx, id, false);
    }

    void setOperator(RecordSession session, ServiceContext ctx, String appId, String accountId) {
        Record accountRec = account.findAccountIdAndPasswordBySigninId(session, ctx, accountId);
        if (accountRec == null) {
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Missing new account id");
        }

        if (StringUtils.equals(ctx.getAccountId(), accountId))
            return;

        session.update("market.changeAppOperIdForOper", CC.map(
                "oper_id=>", accountId,
                "app_id=>", appId
        ));
    }

    @Override
    public void setOperator(final ServiceContext ctx, final String appId, final String accountId) {
        Validate.notNull(appId);
        Validate.notNull(appId);
        Validate.notNull(accountId);

        checkAccountId(ctx, false);
        checkPermission(ctx, appId);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                setOperator(session, ctx, appId, accountId);
                return null;
            }
        });
    }

    private void checkLogoImage(CheckResult cr, Params params, String current) {
        Object val = PublishImpl.getFileParam(params, "logo_image");
        if ("".equals(val)) {
            cr.addFieldError("logo_image", val, "Missing");
        } else if (val instanceof FileItem) {
            FileItem fi = (FileItem) val;
            String err = null;
            // TODO: check logo_image size
            if (err != null)
                cr.addFieldError("logo_image", current, err);
        }
    }

    CheckResult checkPartitionForAdd(RecordSession session, ServiceContext ctx, String appId, String categoryId, Params params) {
        CheckResult cr = new CheckResult();

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

        // logo_image
        checkLogoImage(cr, params, null);

        return cr;
    }

    @Override
    public CheckResult checkPartition(final ServiceContext ctx, final String appId, final String categoryId, final Params params) {
        Validate.notNull(appId);
        Validate.notNull(categoryId);
        Validate.notNull(params);

        return openSession(new RecordSessionHandler<CheckResult>() {
            @Override
            public CheckResult handle(RecordSession session) throws Exception {
                return checkPartitionForAdd(session, ctx, appId, categoryId, params);
            }
        });
    }


    Records listPromotions(RecordSession session, ServiceContext ctx, String appId, String categoryId) throws Exception {
        String locale = ctx.getClientLocale("en_US");

        Record rec = session.selectOne("market.getPromotionsForOper", CC.map(
                "app_id=>", appId,
                "category_id=>", ObjectUtils.toString(categoryId)
        ));
        if (rec == null || rec.isEmpty())
            return new Records();

        String promotionsJson = rec.asString("list");
        if (StringUtils.isBlank(promotionsJson))
            return new Records();

        Records promotions = new Records();

        JsonNode promotionsNode = JsonUtils.parseJson(promotionsJson);
        for (int i = 0; i < promotionsNode.size(); i++) {
            JsonNode promotionNode = promotionsNode.get(i);
            if (!promotionNode.isObject())
                continue;

            int type = promotionNode.path("type").asInt(0);
            String target = promotionNode.path("target").asText();
            // TODO: Multi lang
            // JsonNode name = MLTexts.trimNode(promotionNode.get("name"), locale);
            // JsonNode desc = MLTexts.trimNode(promotionNode.get("description"), locale);
            String name = MLTexts.trimToText(promotionNode.path("name"));
            String desc = MLTexts.trimToText(promotionNode.path("description"));
            String logoImage = promotionNode.path("logo_image").asText();
            // TODO: save promotion image
            promotions.add(new Record().set("type", type).set("target", target).set("name", name)
                    .set("description", desc).set("logo_image", logoImage));
        }

        return promotions;
    }

    @Override
    public Records listPromotions(final ServiceContext ctx, final String appId, final String categoryId) {
        Validate.notNull(ctx);
        Validate.notNull(appId);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listPromotions(session, ctx, appId, categoryId);
            }
        });
    }

    Records setPromotions(RecordSession session, ServiceContext ctx, String appId, String categoryId, Records promotions) throws Exception {
        String promotionsJson = promotions != null ? JsonUtils.toJson(promotions, false) : "[]";
        session.update("market.setPromotionsForOper", CC.map(
                "app_id=>", appId,
                "category_id=>", ObjectUtils.toString(categoryId),
                "now=>", DateTimeUtils.nowMillis(),
                "list=>", promotionsJson
        ));
        return listPromotions(session, ctx, appId, categoryId);
    }

    @Override
    public Records setPromotions(final ServiceContext ctx, final String appId, final String categoryId, final Records promotions) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(promotions);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return setPromotions(session, ctx, appId, categoryId, promotions);
            }
        });
    }

    Records listAllProducts(RecordSession session, ServiceContext ctx, String appId, String categoryId) {
        Records products = session.selectList("market.listAllProductsForOper", CC.map("app_id=>", appId, "category_id=>", categoryId), GenericMapper.get());
        urlCompleter.completeUrl(products);
        return products;
    }

    @Override
    public Records listAllProducts(final ServiceContext ctx, final String appId, final String categoryId) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(categoryId);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listAllProducts(session, ctx, appId, categoryId);
            }
        });
    }
}
