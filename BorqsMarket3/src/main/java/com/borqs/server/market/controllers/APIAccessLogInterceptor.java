package com.borqs.server.market.controllers;


import com.borqs.server.market.log.Logger;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class APIAccessLogInterceptor extends HandlerInterceptorAdapter {

    private static final Logger L = Logger.get(APIAccessLogInterceptor.class).setWriteContext(false);

    public APIAccessLogInterceptor() {
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        String uri = request.getRequestURI();
        String q = request.getQueryString();
        if (StringUtils.isNotEmpty(q)) {
            q = "?" + q;
        } else {
            q = "";
        }

        StringBuilder buff = new StringBuilder();
        buff.append("[").append(request.getRemoteAddr()).append("]");
        buff.append("[").append(ObjectUtils.toString(request.getHeader("User-Agent"))).append("]");
        buff.append(uri).append(q);

        L.info(null, buff);
    }
}
