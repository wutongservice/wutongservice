package com.borqs.server.market.controllers;

import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.CompatibilityConverter;
import com.borqs.server.market.service.CommentService;
import com.borqs.server.market.service.PurchaseService;
import com.borqs.server.market.service.ShareService;
import com.borqs.server.market.utils.DateTimeUtils;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.RandomUtils2;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

import static com.borqs.server.market.Errors.E_ILLEGAL_VERSION;
import static com.borqs.server.market.utils.validation.Predicates.*;
import static com.borqs.server.market.utils.validation.Predicates.expression;

@Controller
@RequestMapping("/")
public class PurchaseController extends AbstractController {
    protected PurchaseService purchaseService;
    protected CommentService commentService;
    protected ShareService shareService;

    public PurchaseController() {
    }

    public PurchaseService getPurchaseService() {
        return purchaseService;
    }

    private static boolean isRenameDownloadCountToPurchaseCount(Params params) {
        return params.param("download_count_to_purchase_count").asBoolean(true);
    }

    @Autowired
    @Qualifier("service.defaultPurchaseService")
    public void setPurchaseService(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @Autowired
    @Qualifier("service.commentService")
    public void setCommentService(CommentService CommentService) {
        this.commentService = CommentService;
    }

    @Autowired
    @Qualifier("service.shareService")
    public void setShareService(ShareService shareService) {
        this.shareService = shareService;
    }

    private static ParamsSchema listProductsSchema = new ParamsSchema()
            .required("app_id", "string", notBlank())
            .required("app_version", "int >= 0", asInt(), expression("x >= 0"))
            .optional("app_mod", "string array")
            .optional("category_id", "string", notBlank())
            .optional("tag", "string", notBlank())
            .optional("order_by", "int in [1,2]", asInt(),
                    in(PurchaseService.PO_DOWNLOAD_COUNT, PurchaseService.PO_RATING, PurchaseService.PO_PURCHASE_COUNT))
            .optional("paid", PurchaseService.PT_PRICE_ALL, "int in [0,1,2]", asInt(),
                    in(PurchaseService.PT_PRICE_ALL, PurchaseService.PT_PRICE_FREE, PurchaseService.PT_PRICE_PAID));
    @RequestMapping(value = "/api/v2/purchase/products/list", method = RequestMethod.GET)
    public APIResponse listProducts(ServiceContext ctx, Params params)  {
        params = listProductsSchema.validate(params);
        RecordsWithTotal versionedProducts = purchaseService.listProducts(ctx, params);
        if (isRenameDownloadCountToPurchaseCount(params)) {
            CompatibilityConverter.renameDownloadCountToPurchaseCount(versionedProducts);
        }
        return APIResponse.of(versionedProducts);
    }

    private static final ParamsSchema getProductSchema = new ParamsSchema()
            .required("id", "string", notBlank())
            .optional("version", "int >= 0", asInt(), expression("x >= 0"))
            .optional("app_mod", "string array");
    @RequestMapping(value = "/api/v2/purchase/products/get", method = RequestMethod.GET)
    public APIResponse getProduct(ServiceContext ctx, Params params)  {
        params = getProductSchema.validate(params);
        Record versionedProduct = purchaseService.getProduct(ctx,
                params.param("id").asString(),
                params.param("version").asInt(Integer.MIN_VALUE),
                params.param("app_mod").asString(null));
        if (versionedProduct == null)
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal versioned product id");

        if (isRenameDownloadCountToPurchaseCount(params)) {
            CompatibilityConverter.renameDownloadCountToPurchaseCount(versionedProduct);
        }
        return APIResponse.of(versionedProduct);
    }


    private static final ParamsSchema purchaseSchema = new ParamsSchema()
            .required("id", "string", notBlank())
            .required("version", "int > 0", expression("x > 0"))
            .optional("google_iab_order", "string: not blank", notBlank())
            .optional("cmcc_mm_order", "string: not blank", notBlank())
            .optional("cmcc_mm_trade", "string: not blank", notBlank());
    @RequestMapping(value = "/api/v2/purchase/purchase", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse purchase(ServiceContext ctx, Params params) {
        params = purchaseSchema.validate(params);
        Record r = purchaseService.purchase(ctx, params);
        return APIResponse.of(r);
    }

    @RequestMapping(value = "/api/v2/purchase/purchased", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listPurchased(ServiceContext ctx, Params params) {
        RecordsWithTotal recsWithTotal = purchaseService.listPurchased(ctx, params);
        return APIResponse.of(recsWithTotal);
    }


    private static final ParamsSchema listPartitionsSchema = new ParamsSchema()
            .required("app_id", "string", notBlank())
            .required("category_id", "string", notBlank());
    @RequestMapping(value = "/api/v2/purchase/partitions/list", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listPartitions(ServiceContext ctx, Params params) {
        params = listPartitionsSchema.validate(params);
        //Records r = purchaseService.listPartitions(ctx, params.param("app_id").asString(), params.param("category_id").asString());
        return APIResponse.of(new Records());
    }

    private static final ParamsSchema listPartitionProductsSchema = new ParamsSchema()
            .required("id", "string", notBlank())
            .required("app_version", "int >= 0", asInt(), expression("x >= 0"))
            .optional("app_mod", "string array");
    @RequestMapping(value = "/api/v2/purchase/partitions/products", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listPartitionProducts(ServiceContext ctx, Params params) {
        params = listPartitionProductsSchema.validate(params);
//        RecordsWithTotal r = purchaseService.listPartitionProducts(ctx, params.param("id").asString(), params);
//        if (isRenameDownloadCountToPurchaseCount(params)) {
//            CompatibilityConverter.renameDownloadCountToPurchaseCount(r);
//        }
        return APIResponse.of(null);
    }

    private static final ParamsSchema hasPurchasedSchema = new ParamsSchema()
            .required("ids", "string", notBlank())
            ;
    @RequestMapping(value = "/api/v2/purchase/has_purchased", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse hasPurchased(ServiceContext ctx, Params params) {
        params = hasPurchasedSchema.validate(params);
        Records purchasedRecs = purchaseService.hasPurchased(ctx, params.param("ids").asArray(",", true, new String[]{}));
        if (params.param("for_one").asBoolean(false)) {
            boolean b = purchasedRecs.get(0).asBoolean("purchased", false);
            return APIResponse.of(b);
        } else {
            return APIResponse.of(purchasedRecs);
        }
    }

    // comments api
    private static final ParamsSchema updateCommentSchema = new ParamsSchema()
            .required("product_id", "product_id: string not blank", notBlank())
            .required("version", "int not blank", notBlank())
            .required("message", "string not blank", notBlank())
            .required("rating", "double value between 0.0 and 1.0", notBlank(), expression("x >= 0.0 && x <= 1.0"))
            ;
    @RequestMapping(value = "/api/v2/purchase/products/comments/update", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse updateComment(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = updateCommentSchema.validate(params);

        String productId = params.param("product_id").asString();
        int version = params.param("version").asInt();
        String msg = params.param("message").asString("");
        double rating = params.param("rating").asDouble(0.6);

        Record commentRec = commentService.updateComment(ctx, productId, version, msg, rating);
        return APIResponse.of(commentRec);
    }

    private static final ParamsSchema listCommentSchema = new ParamsSchema()
            .required("product_id", "product_id: string not blank", notBlank())
            .optional("version", "int not blank", notBlank())
            ;
    @RequestMapping(value = "/api/v2/purchase/products/comments/list", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listComments(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = listCommentSchema.validate(params);

        Paging paging = params.getPaging(20);
        String productId = params.param("product_id").asString();
        Integer version = params.param("version").asIntObject();

        RecordsWithTotal rs = commentService.listComments(ctx, productId, version, paging);
        return APIResponse.of(rs);
    }

    private static final ParamsSchema getCommentForProductSchema = new ParamsSchema()
            .required("product_id", "product_id: string not blank", notBlank())
            .optional("version", "int not blank", notBlank())
            ;
    @RequestMapping(value = "/api/v2/purchase/products/comments/my", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse getMyCommentForProduct(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = getCommentForProductSchema.validate(params);

        String productId = params.param("product_id").asString();
        Integer version = params.param("version").asIntObject();
        Record commentRec = commentService.getMyCommentForProduct(ctx, productId, version);
        return APIResponse.of(commentRec);
    }

    // share
    private static final ParamsSchema updateShareSchema = new ParamsSchema()
            .required("category_id", "string not blank", notBlank())
            .required("app_id", "string not blank", notBlank())
            .required("app_version", "int >= 0", notBlank(), asInt(), expression("x >= 0"));

    @RequestMapping(value = "/api/v2/purchase/shares/update", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse createShare(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = updateShareSchema.validate(params);

        Record share = params.asRecord(null,
                "app_id", "category_id",
                "name", "description", "content", "app_data_1", "app_data_2",
                "file", "logo_image", "cover_image",
                "screenshot1_image", "screenshot2_image", "screenshot3_image", "screenshot4_image", "screenshot5_image",
                "type1", "type2", "type3",
                "tags", "app_version", "supported_mod"
        );
        Record r = shareService.createShare(ctx, share);
        return APIResponse.of(r);
    }

    private static final ParamsSchema deleteShareSchema = new ParamsSchema()
            .required("id", "string not blank", notBlank());

    @RequestMapping(value = "/api/v2/purchase/shares/delete", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse deleteShare(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = deleteShareSchema.validate(params);
        shareService.deleteShare(ctx, params.param("id").asString());
        return APIResponse.of(true);
    }

    private static final ParamsSchema listSharesSchema = new ParamsSchema()
            .optional("category_id", "string not blank", notBlank())
            .required("app_id", "string not blank", notBlank())
            .optional("since", "long int value >= 0", notBlank(), asLong(), expression("x >= 0"));
    @RequestMapping(value = "/api/v2/purchase/shares/list", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listShares(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = listSharesSchema.validate(params);

        Paging paging = params.getPaging(20);
        String appId = params.param("app_id").asString();
        String categoryId = params.param("category_id").asString();
        Long since = params.param("since").asLongObject();
        if (since != null && since == 0L) {
            since = null;
        }
        Params opts = new Params(params).retainsParams("order", "tag", "app_mod", "min_app_version", "max_app_version");

        Records rs = shareService.listShares(ctx, appId, categoryId, since, opts, paging);

        return APIResponse.of(rs);
    }

    private static final ParamsSchema downloadShareSchema = new ParamsSchema()
            .required("id", "string not blank", notBlank());

    @RequestMapping(value = "/api/v2/purchase/shares/download", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse downloadShareFile(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = downloadShareSchema.validate(params);
        Record r = shareService.downloadFile(ctx, params.param("id").asString());
        return APIResponse.of(r);
    }
}
