package com.borqs.server.market.controllers;

import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.impl.ServiceSupport;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.borqs.server.market.utils.validation.Predicates.asJson;
import static com.borqs.server.market.utils.validation.Predicates.notBlank;

@Controller
@RequestMapping("/")
public class ToolsController extends ServiceSupport {
    public ToolsController() {
    }

    protected AccountService accountService;

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
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
        account.put("name", (String) user.get("display_name"));
        account.put("password", (String) user.get("password"));
        account.put("phone", (String) user.get("login_phone1"));
        String email1 = (String) user.get("login_email1");
        if (StringUtils.isBlank(email1))
            email1 = (String) user.get("login_email2");
        if (StringUtils.isBlank(email1))
            email1 = (String) user.get("login_email3");

        account.put("email", email1);
        return account;
    }
}
