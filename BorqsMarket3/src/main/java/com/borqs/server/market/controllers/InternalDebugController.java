package com.borqs.server.market.controllers;

import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.InternalService;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.utils.validation.ParamsSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;

import static com.borqs.server.market.utils.validation.Predicates.notBlank;

/**
 * Created with IntelliJ IDEA.
 * User: wutong
 * Date: 9/16/13
 * Time: 12:01 PM
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/")
public class InternalDebugController extends AbstractController {
    protected InternalService internalService;

    @Autowired
    @Qualifier("service.internalDebug")
    public void setInternalService(InternalService _internalService) {
        this.internalService = _internalService;
    }

    private static final ParamsSchema internalServiceSchema = new ParamsSchema()
            .optional("start", "start cursor", notBlank())
            .optional("end",   "end   cursor", notBlank())
            .required("pid", "string not blank", notBlank());


    @RequestMapping(value = "/api/v2/internal/show/count", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse showProductPurchaseCount(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = internalServiceSchema.validate(params);
        Record r = internalService.getProductPurchaseCount(ctx, params.param("pid").asString());
        return APIResponse.of(r);
    }

    @RequestMapping(value = "/api/v2/internal/show/purchases", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse showProductPurchases(ServiceContext ctx, HttpServletRequest req, HttpServletResponse resp, Params params) throws ParseException, IOException {
        params = internalServiceSchema.validate(params);
        Records r = internalService.getProductPurchaseRecords(ctx, params.param("pid").asString(), params.param("start").asInt(), params.param("end").asInt());
        return APIResponse.of(r);
    }
}
