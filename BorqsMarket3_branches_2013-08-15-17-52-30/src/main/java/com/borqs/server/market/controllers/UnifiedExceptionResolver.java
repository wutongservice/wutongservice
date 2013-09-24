package com.borqs.server.market.controllers;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.Errors;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.log.Logger;
import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonResponse;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import com.borqs.server.market.utils.WebUtils2;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UnifiedExceptionResolver implements HandlerExceptionResolver {
    private static final Logger L = Logger.get(UnifiedExceptionResolver.class);

    public UnifiedExceptionResolver() {
    }

    public static Map<String, Object> toErrorModel(Exception e, HttpServletRequest req) {
        int code;
        String message;
        String[] details;
        if (e instanceof ServiceException) {
            code = ((ServiceException) e).getCode();
            message = e.getMessage();
            details = ((ServiceException) e).getDetails();
        } else {
            code = Errors.E_UNKNOWN;
            message = ObjectUtils.toString(e.getMessage());
            String errorMsg = e.getMessage();
            details = errorMsg != null ? new String[]{errorMsg} : null;
        }
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("code", code);
        m.put("error_msg", message);
        if (details != null) {
            m.put("error_details", details);
        }

        if (req != null) {
            String showErrorStack = req.getParameter("show_error_stack");
            if (showErrorStack != null && PrimitiveTypeConverter.toBoolean(showErrorStack, false)) {
                m.put("error_stack", getErrorStack(e));
            }
        }
        return m;
    }

    private static String[] getErrorStack(Exception e) {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        String[] ss = StringUtils.split(w.toString(), "\n");
        for (int i = 0; i < ss.length; i++) {
            ss[i] = StringUtils.replace(ss[i], "\t", "    ");
        }
        return ss;
    }

    public static ModelAndView displayErrorPage(Exception e, HttpServletResponse resp) {
        if (e instanceof ServiceException) {
            int errorCode = ((ServiceException) e).getCode();
            if (errorCode == Errors.E_ILLEGAL_TICKET || errorCode == Errors.E_ILLEGAL_ROLE) {
                WebUtils2.deleteCookie(resp, "ticket");
                return new ModelAndView("redirect:/signin");
            } else {
                return new ModelAndView("error", UnifiedExceptionResolver.toErrorModel(e, null));
            }
        } else {
            return new ModelAndView("error", UnifiedExceptionResolver.toErrorModel(e, null));
        }
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {
        String uri = httpServletRequest.getRequestURI();
        String q = httpServletRequest.getQueryString();
        if (StringUtils.isNotEmpty(q)) {
            q = "?" + q;
        } else {
            q = "";
        }
        ServiceContext ctx = Attributes.getServiceContext(httpServletRequest);
        L.error(ctx, "[" + uri + q + "] Exception occurs", e);

        if (StringUtils.contains(uri, "/api/")) {
            return JsonResponse.of(toErrorModel(e, httpServletRequest));
        } else {
            return displayErrorPage(e, httpServletResponse);
        }
    }
}
