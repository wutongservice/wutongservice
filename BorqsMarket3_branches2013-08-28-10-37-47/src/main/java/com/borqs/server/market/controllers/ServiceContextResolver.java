package com.borqs.server.market.controllers;


import com.borqs.server.market.context.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;


public class ServiceContextResolver implements HandlerMethodArgumentResolver {
    private ServiceContextFactory serviceContextFactory;

    public ServiceContextResolver() {
    }

    public ServiceContextFactory getServiceContextFactory() {
        return serviceContextFactory;
    }

    @Autowired
    public void setServiceContextFactory(ServiceContextFactory serviceContextFactory) {
        this.serviceContextFactory = serviceContextFactory;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ServiceContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return serviceContextFactory.create((HttpServletRequest) webRequest.getNativeRequest());
    }
}
