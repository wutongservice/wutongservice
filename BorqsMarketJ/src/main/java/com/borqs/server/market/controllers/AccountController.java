package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.services.AccountService;
import com.borqs.server.market.utils.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/")
public class AccountController extends AbstractController {

    protected ServiceContextFactory serviceContextFactory;

    public AccountController() {
    }

    public ServiceContextFactory getServiceContextFactory() {
        return serviceContextFactory;
    }

    @Autowired
    public void setServiceContextFactory(ServiceContextFactory serviceContextFactory) {
        this.serviceContextFactory = serviceContextFactory;
    }

    @RequestMapping(value = "/api/v1/account/signin", method = {RequestMethod.GET, RequestMethod.POST})
    public APIResponse signin(Params params, ServiceContext ctx) throws ServiceException {
        AccountService accountService = serviceContextFactory.getAccountService();
        // TODO: xx
        return APIResponse.of("hello");
    }

}
