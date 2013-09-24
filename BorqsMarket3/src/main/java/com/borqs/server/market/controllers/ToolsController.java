package com.borqs.server.market.controllers;

import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.impl.ServiceSupport;
import com.borqs.server.market.sfs.FileContent;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

import static com.borqs.server.market.utils.validation.Predicates.*;

@Controller
@RequestMapping("/")
public class ToolsController extends ServiceSupport {

    private AccountService accountService;
    private FileStorage imageStorage;
    private FileStorage productStorage;
    private FileStorage partitionStorage;
    private FileStorage promotionStorage;
    private FileStorage accountStorage;
    private FileStorage sharesStorage;


    public ToolsController() {
    }



    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    @Autowired
    @Qualifier("storage.account")
    public void setAccountStorage(FileStorage accountStorage) {
        this.accountStorage = accountStorage;
    }

    @Autowired
    @Qualifier("storage.image")
    public void setImageStorage(FileStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @Autowired
    @Qualifier("storage.partition")
    public void setPartitionStorage(FileStorage partitionStorage) {
        this.partitionStorage = partitionStorage;
    }

    @Autowired
    @Qualifier("storage.product")
    public void setProductStorage(FileStorage productStorage) {
        this.productStorage = productStorage;
    }

    @Autowired
    @Qualifier("storage.promotion")
    public void setPromotionStorage(FileStorage promotionStorage) {
        this.promotionStorage = promotionStorage;
    }

    @Autowired
    @Qualifier("storage.shares")
    public void setSharesStorage(FileStorage sharesStorage) {
        this.sharesStorage = sharesStorage;
    }

    private static final ParamsSchema createAppSchema = new ParamsSchema()
            .required("id", "not blank string", notBlank())
            .required("name", "multi locale name", asJson());

    @RequestMapping(value = "/api/v2/tools/create_app")
    public APIResponse createApp(final ServiceContext ctx, final Params params) {
        createAppSchema.validate(params);
        checkAccountId(ctx, true);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                session.insert("market.createAppTool", CC.map(
                        "id=>", params.param("id").asString(),
                        "name=>", params.param("name").asString(),
                        "now=>", DateTimeUtils.nowMillis(),
                        "creator_id=>", ctx.getAccountId()
                ));
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema addCategorySchema = new ParamsSchema()
            .required("app_id", "not blank string", notBlank())
            .required("category_id", "not blank string", notBlank())
            .required("name", "multi locale name", asJson());

    @RequestMapping(value = "/api/v2/tools/add_category")
    public APIResponse addCategory(final ServiceContext ctx, final Params params) {
        addCategorySchema.validate(params);
        checkAccountId(ctx, true);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                session.insert("market.createCategoryTool", CC.map(
                        "category_id=>", params.param("category_id").asString(),
                        "app_id=>", params.param("app_id").asString(),
                        "name=>", params.param("name").asString(),
                        "now=>", DateTimeUtils.nowMillis()
                ));
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema addFreePricetagSchema = new ParamsSchema()
            .required("app_id", "not blank string", notBlank())
            .required("category_id", "not blank string", notBlank())
            .required("pricetag_id", "not blank string", notBlank());

    @RequestMapping(value = "/api/v2/tools/add_free_pricetag")
    public APIResponse addFreePricetag(final ServiceContext ctx, final Params params) {
        addFreePricetagSchema.validate(params);
        checkAccountId(ctx, true);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                session.insert("market.createPricetagTool", CC.map(
                        "pricetag_id=>", params.param("pricetag_id").asString(),
                        "category_id=>", params.param("category_id").asString(),
                        "app_id=>", params.param("app_id").asString(),
                        "now=>", DateTimeUtils.nowMillis()
                ));
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema addPaidPricetagSchema = new ParamsSchema()
            .required("app_id", "not blank string", notBlank())
            .required("category_id", "not blank string", notBlank())
            .required("pricetag_id", "not blank string", notBlank())
            .required("price", "not blank", notBlank());


    @RequestMapping(value = "/api/v2/tools/add_paid_pricetag")
    public APIResponse addPaidPricetag(final ServiceContext ctx, final Params params) {
        addPaidPricetagSchema.validate(params);
        checkAccountId(ctx, true);
        final String price = JsonUtils.toJson(params.param("price").asJson(), false);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                session.insert("market.createPricetagTool", CC.map(
                        "pricetag_id=>", params.param("pricetag_id").asString(),
                        "category_id=>", params.param("category_id").asString(),
                        "app_id=>", params.param("app_id").asString(),
                        "price=>", price,
                        "google_iab_sku=>", params.param("google_iab_sku").asString(null),
                        "cmcc_mm_paycode=>", params.param("cmcc_mm_paycode").asString(null),
                        "cmcc_mm_amount=>", params.param("cmcc_mm_amount").asIntObject(),
                        "now=>", DateTimeUtils.nowMillis()
                ));
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema deleteOrderSchema = new ParamsSchema()
            .required("id", "string", notBlank());

    @RequestMapping(value = "/api/v2/tools/delete_order")
    public APIResponse deleteOrder(final ServiceContext ctx, final Params params) {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_PERMISSION, "Need login");

        deleteOrderSchema.validate(params);
        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                session.delete("market.deleteOrderByProductIdTool", CC.map(
                        "account_id=>", ctx.getAccountId(),
                        "product_id=>", params.param("id").asString()
                ));
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    @RequestMapping(value = "/api/v2/tools/fill_supported_mod")
    public APIResponse fillSupportedForFreehdhomeWallpaper(final ServiceContext ctx) {
        final String appId = "com.borqs.freehdhome";
        final String categoryId = "wallpaper";
        final String defaultSupportedMod = "landscape";

        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                Records pvs = session.selectList("market.listAllNoSupportedModTool", CC.map(
                        "app_id=>", appId,
                        "category_id=>", categoryId
                ));
                for (Record pv : pvs) {
                    session.update("market.fillDefaultSupportedModTool", CC.map(
                            "id=>", pv.asString("id"),
                            "version=>", pv.asString("version"),
                            "supported_mod=>", defaultSupportedMod
                    ));
                }
                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema createOrdersSchema = new ParamsSchema()
            .required("product_id", "string", notBlank())
            .required("version", "int", notBlank(), asInt())
            .required("accounts", "'*' or account_id list", notBlank());
    @RequestMapping(value = "/api/v2/tools/create_orders")
    public APIResponse createOrders(final ServiceContext ctx, Params params) {
        params = createOrdersSchema.validate(params);
        final String productId = params.param("product_id").asString();
        final int version = params.param("version").asInt();
        final String accountIdsStr = params.param("accounts").asString();

        openSession(new RecordSessionHandler<Object>() {
            @Override
            public Object handle(RecordSession session) throws Exception {
                Record product = session.selectOne("market.getProductAppIdAndCategoryIdTool", CC.map("id=>", productId));
                if (product == null)
                    throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product");

                String appId = product.asString("app_id");
                String categoryId = product.asString("category_id");

                List<String> accountIds;
                if ("*".equals(accountIdsStr)) {
                    accountIds = session.selectList("market.listAllAccountIdsTool", CC.map("only_external=>", true)).asStringList("id");
                } else {
                    accountIds = new ArrayList<String>(StringUtils2.splitSet(accountIdsStr, ",", true));
                }

                long now = DateTimeUtils.nowMillis();

                for (String accountId : accountIds) {
                    String orderId = session.selectStringValue("market.findOrderIdForPurchase", CC.map(
                            "id=>", productId,
                            "purchaser_id=>", accountId
                    ), null);
                    if (orderId != null)
                        continue;


                    orderId = String.format("%s.%s", productId, RandomUtils2.randomLong());
                    session.insert("market.createOrderForPurchase", CC.map(
                            "order_id=>", orderId,
                            "now=>", now,
                            "purchaser_id=>", accountId,
                            "product_id=>", productId,
                            "product_version=>", version,
                            "product_category_id=>", categoryId,
                            "product_app_id=>", appId,
                            "purchaser_device_id=>", "",
                            "purchaser_locale=>", "en_US",
                            "purchaser_ip=>", "0.0.0.0",
                            "google_iab_order=>", "",
                            "purchaser_ua=>", "dummy"
                    ));
                }

                return null;
            }
        });
        return APIResponse.of("ok");
    }

    private static final ParamsSchema storeSchema = new ParamsSchema()
            .required("type", "string", notBlank())
            .required("id", "notBlank", notBlank());
    @RequestMapping(value = "/api/v2/tools/store", method = RequestMethod.POST)
    public APIResponse store(ServiceContext ctx, Params params) throws Exception {
        params = storeSchema.validate(params);

        String type = params.param("type").asString("").trim();
        String id = params.param("id").asString("").trim();
        FileItem fi = params.param("file").asFileItem();
        if (fi == null)
            throw new IllegalArgumentException("file: Missing");

        String fileId;
        FileContent fc = FileContent.createWithFileItem(fi);
        if ("promotion.logo".equals(type)) {
            String appId = StringUtils.substringBefore(id, "-").trim();
            String categoryId = StringUtils.substringAfter(id, "-").trim();
            if (appId.isEmpty() || categoryId.isEmpty())
                throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal id (app_id-category_id)");

            fileId = "promotion-" + appId + "-" + categoryId + "-logo-" + RandomUtils2.randomLong() + "." + FilenameUtils.getExtension(fi.getName());
            fileId = promotionStorage.write(fileId, fc);
        } else {
            // TODO: add more file type
            throw new ServiceException(Errors.E_ILLEGAL_PARAM, "Illegal type " + type);
        }

        return APIResponse.of(new Record().set("file_id", fileId));
    }

    /**
     * add new User
     *
     * @param ctx
     * @param params
     * @return
     */
    @RequestMapping(value = "/api/v2/tools/add_user")
    public APIResponse addUser(final ServiceContext ctx, final Params params) {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_PERMISSION, "Need login");
        String email = "";
        String borqs_id = "";
        Record record = new Record();
        if (params.hasParam("email")) {
            email = params.param("email").asString();
            record = accountService.getWutongUserByEmail(ctx, email, false);
        }
        if (params.hasParam("id")) {
            borqs_id = params.param("id").asString();
            record = accountService.getWutongUserByUserId(ctx, borqs_id, false);
        }
        //migration the result of query
        Record account = WutongUserToAccount(record);
        Record result = accountService.getAccountByEmail(ctx, (String) account.get("email"), false);
        if (result != null && StringUtils.isNotBlank((String) result.get("id"))) {
            throw new ServiceException(Errors.E_ACCOUNT, "duplicate email input!");
        }

        accountService.signup(ctx, account);
        return APIResponse.of("ok");
    }

    private Record WutongUserToAccount(Record user) {
        Record account = new Record();
        account.put("name", user.asString("display_name"));
        account.put("password", user.asString("password"));
        account.put("phone", user.asString("login_phone1"));
        String email1 = user.asString("login_email1");
        if (StringUtils.isBlank(email1))
            email1 = user.asString("login_email2");
        if (StringUtils.isBlank(email1))
            email1 = user.asString("login_email3");

        account.put("email", email1);
        return account;
    }
}
