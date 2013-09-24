package com.borqs.server.market.controllers;


import com.borqs.server.market.utils.Params;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;


public class HttpParamsResolver implements HandlerMethodArgumentResolver {
    public HttpParamsResolver() {
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class paramType = parameter.getParameterType();
        return paramType.equals(HttpParams.class)
                || paramType.equals(Params.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return HttpParams.create((HttpServletRequest) webRequest.getNativeRequest());
    }
}
