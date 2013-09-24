package com.borqs.server.market.controllers;

import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.CompatibilityConverter;
import com.borqs.server.market.service.PurchaseService;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.borqs.server.market.Errors.E_ILLEGAL_VERSION;
import static com.borqs.server.market.utils.validation.Predicates.*;
import static com.borqs.server.market.utils.validation.Predicates.expression;

@Controller
@RequestMapping("/")
public class PurchaseController extends AbstractController {
    private PurchaseService purchaseService;

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
            .required("version", "int >= 0", asInt(), expression("x >= 0"));
    @RequestMapping(value = "/api/v2/purchase/products/get", method = RequestMethod.GET)
    public APIResponse getProduct(ServiceContext ctx, Params params)  {
        params = getProductSchema.validate(params);
        Record versionedProduct = purchaseService.getProduct(ctx, params.param("id").asString(), params.param("version").asInt());
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
        Records r = purchaseService.listPartitions(ctx, params.param("app_id").asString(), params.param("category_id").asString());
        return APIResponse.of(r);
    }

    private static final ParamsSchema listPartitionProductsSchema = new ParamsSchema()
            .required("id", "string", notBlank())
            .required("app_version", "int >= 0", asInt(), expression("x >= 0"))
            .optional("app_mod", "string array");
    @RequestMapping(value = "/api/v2/purchase/partitions/products", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse listPartitionProducts(ServiceContext ctx, Params params) {
        params = listPartitionProductsSchema.validate(params);
        RecordsWithTotal r = purchaseService.listPartitionProducts(ctx, params.param("id").asString(), params);
        if (isRenameDownloadCountToPurchaseCount(params)) {
            CompatibilityConverter.renameDownloadCountToPurchaseCount(r);
        }
        return APIResponse.of(r);
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
}
