package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.services.PurchaserService;
import com.borqs.server.market.utils.JsonResponse;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import static com.borqs.server.market.Errors.*;

import static com.borqs.server.market.utils.validation.Predicates.*;

@Controller
@RequestMapping("/")
public class PurchaserController extends AbstractController {
    private PurchaserService purchaserService;

    public PurchaserController() {
    }

    public PurchaserService getPurchaserService() {
        return purchaserService;
    }

    @Autowired
    public void setPurchaserService(PurchaserService purchaserService) {
        this.purchaserService = purchaserService;
    }


    private static ParamsSchema LIST_PRODUCTS_SCHEMA = new ParamsSchema()
            .required("app", "string", notBlank())
            .required("version_code", "int >= 0", asInt(), expression("x >= 0"))
            .optional("mod", "string array")
            .optional("category", "string", notBlank())
            .optional("order", "string", notBlank())
            .optional("price", PurchaserService.PRICE_ALL, "int in [0,1,2]", asInt(),
                    in(PurchaserService.PRICE_ALL, PurchaserService.PRICE_FREE, PurchaserService.PRICE_PAID))
            .optional("page", 0, "int >= 0", expression("x >= 0"))
            .optional("count", 20, "0 < int <= 100", expression("x > 0 && x <= 100"));

    @RequestMapping(value = "/api/v1/purchaser/products/list", method = RequestMethod.GET)
    public JsonResponse listProducts(Params params, ServiceContext ctx) throws ServiceException {
        params = LIST_PRODUCTS_SCHEMA.validate(params);
        Records products = purchaserService.listProducts(ctx, params.param("app").asString(), Params.of(
                "category =>", params.param("category").asString(),
                "appVersion =>", params.param("version_code").asInt(0),
                "appMod =>", params.param("mod").asString(),
                "price =>", params.param("price").asInt(PurchaserService.PRICE_ALL),
                "orderBy =>", params.param("order").asString("updated"),
                "page =>", params.param("page").asInt(0),
                "count =>", params.param("count").asInt(20)
        ));
        return APIResponse.of(products);
    }


    private static final ParamsSchema GET_PRODUCT_SCHEMA = new ParamsSchema()
            .required("id", "string", notBlank())
            .required("version", "int >= 0", asInt(), expression("x >= 0"));

    @RequestMapping(value = "/api/v1/purchaser/products/get", method = RequestMethod.GET)
    public JsonResponse getProduct(Params params, ServiceContext ctx) throws ServiceException {
        params = GET_PRODUCT_SCHEMA.validate(params);
        Record rec = purchaserService.getProduct(ctx, params.param("id").asString(), params.param("version").asInt());
        if (rec == null)
            throw new ServiceException(E_ILLEGAL_VERSION, "Illegal versioned product id");

        return new APIResponse(rec);
    }


    private static final ParamsSchema PURCHASE_SCHEMA = new ParamsSchema()
            .required("id", "string", notBlank())
            .required("version", "int > 0", expression("x > 0"))
            .optional("google_iab_order_id", "string", "", notBlank());

    @RequestMapping(value = "/api/v1/purchaser/purchase", method = {RequestMethod.GET, RequestMethod.POST})
    public JsonResponse purchase(Params params, ServiceContext ctx) throws ServiceException {
        params = PURCHASE_SCHEMA.validate(params);
        Record r = purchaserService.purchase(ctx,
                params.param("id").asString(),
                params.param("version").asInt(),
                Params.of("google_iab_order_id=>", params.param("google_iab_order_id").asString()));

        return new APIResponse(r);
    }

}
