package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.Errors;
import com.borqs.server.market.log.Logger;
import com.borqs.server.market.utils.JsonResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;

@Component
public class UnifiedExceptionResolver implements HandlerExceptionResolver {
    private static final Logger L = Logger.get(UnifiedExceptionResolver.class);

    public UnifiedExceptionResolver() {
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {
        int code;
        String message;
        String[] details;
        if (e instanceof ServiceException) {
            code = ((ServiceException) e).getCode();
            message = e.getMessage();
            details = ((ServiceException) e).getDetails();
        } else {
            L.error(null, "Unknown exception", e);
            code = Errors.E_UNKNOWN;
            message = "Unknown exception: " + e.getClass().getName();
            String errorMsg = e.getMessage();
            details = errorMsg != null ? new String[] {errorMsg} : null;
        }
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("code", code);
        m.put("error_msg", message);
        if (details != null) {
            m.put("error_details", details);
        }
        return JsonResponse.of(m);
    }
}
