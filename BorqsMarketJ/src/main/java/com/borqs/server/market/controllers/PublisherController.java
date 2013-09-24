package com.borqs.server.market.controllers;

import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.models.FieldTrimmer;
import com.borqs.server.market.utils.record.Records;
import com.borqs.server.market.services.PublisherService;
import com.borqs.server.market.utils.JsonResponse;
import com.borqs.server.market.utils.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class PublisherController extends AbstractController {

    private PublisherService publisherService;

    public PublisherController() {
    }

    public PublisherService getPublisherService() {
        return publisherService;
    }

    @Autowired
    @Qualifier("service.defaultPublisherService")
    public void setPublisherService(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @RequestMapping(value = "/v1/api/publisher/apps/list", method = RequestMethod.GET)
    public JsonResponse listBorqsApps(Params params, ServiceContext ctx) throws ServiceException {
        Records apps = publisherService.listBorqsApps(ctx);
        return APIResponse.of(apps);
    }
}
