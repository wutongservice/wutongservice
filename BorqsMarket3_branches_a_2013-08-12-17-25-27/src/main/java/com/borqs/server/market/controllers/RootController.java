package com.borqs.server.market.controllers;

import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import org.apache.commons.lang.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/")
public class RootController extends AbstractController {

    protected LocaleResolver localeResolver;

    public RootController() {
    }

    @Autowired
    @Qualifier("localeResolver")
    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @RequestMapping(value = "/")
    public ModelAndView index(ServiceContext ctx, HttpServletRequest req) {
        if (!ctx.hasAccountId()) {
            return redirect("/signin");
        } else {
            return redirectToModule(loadCurrentModule(req));
        }
    }
}
