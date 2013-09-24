package com.borqs.server.market.controllers;

import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
@RequestMapping("/")
public class ErrorsController  {

    private static boolean isAPI(HttpServletRequest req) {
        String uri = (String) req.getAttribute("javax.servlet.forward.request_uri");
        return StringUtils.startsWith(uri, "/api/");
    }

    @RequestMapping("/error/404")
    public static ModelAndView error404(HttpServletRequest req) {
        if (isAPI(req)) {
            return JsonResponse.of(CC.map("message", "API not found"));
        } else {
            return new ModelAndView("404");
        }
    }
}
