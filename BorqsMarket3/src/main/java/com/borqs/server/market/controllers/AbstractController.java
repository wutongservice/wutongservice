package com.borqs.server.market.controllers;


import com.borqs.server.market.models.LocaleSelector;
import com.borqs.server.market.service.ServiceConsts;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.WebUtils2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractController {

    protected LocaleSelector localeSelector;

    protected AbstractController() {
    }

    public LocaleSelector getLocaleSelector() {
        return localeSelector;
    }

    @Autowired
    @Qualifier("helper.localeSelector")
    public void setLocaleSelector(LocaleSelector localeSelector) {
        this.localeSelector = localeSelector;
    }

    public static ModelAndView redirect(String to) {
        return new ModelAndView("redirect:" + to);
    }

    public static ModelAndView forward(String to) {
        return new ModelAndView("forward:" + to);
    }

    protected static void saveCurrentModule(HttpServletRequest req, HttpServletResponse resp, String module) {
        if (req != null) {
            Attributes.setCurrentModule(req, module);
        }
        WebUtils2.setCookie(resp, CookieNames.CURRENT_MODULE, module);
    }

    protected static String loadCurrentModule(HttpServletRequest req) {
        return WebUtils2.getCookie(req, CookieNames.CURRENT_MODULE, null);
    }

    protected static ModelAndView redirectToModule(String module) {
        module = module != null ? module : ServiceConsts.MODULE_PUBLISH;
        return redirect("/" + module);
    }


    protected static int getStatMonthsWithParams(Params params, String name, int def) {
        String m = params.param(name).asString(Integer.toString(def)).trim();
        if ("all".equalsIgnoreCase(m)) {
            return 0;
        } else {
            try {
                return Integer.parseInt(m);
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }

}
