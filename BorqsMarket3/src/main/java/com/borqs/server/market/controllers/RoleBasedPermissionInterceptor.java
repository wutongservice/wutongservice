package com.borqs.server.market.controllers;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.ServiceConsts;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RoleBasedPermissionInterceptor implements HandlerInterceptor {

    private static final UriPrefixWithRoles[] URI_PREFIX_WITH_ROLES_TABLE = {
            new UriPrefixWithRoles("/api/v2/purchase", ServiceConsts.ROLE_PURCHASER),
            new UriPrefixWithRoles("/purchase", ServiceConsts.ROLE_PURCHASER),

            new UriPrefixWithRoles("/api/v2/publish", ServiceConsts.ROLE_PUBLISHER),
            new UriPrefixWithRoles("/publish", ServiceConsts.ROLE_PUBLISHER),

            new UriPrefixWithRoles("/api/v2/develop", ServiceConsts.ROLE_DEVELOPER),
            new UriPrefixWithRoles("/develop", ServiceConsts.ROLE_DEVELOPER),

            new UriPrefixWithRoles("/api/v2/oper", ServiceConsts.ROLE_OPERATOR),
            new UriPrefixWithRoles("/oper", ServiceConsts.ROLE_OPERATOR),

            new UriPrefixWithRoles("/api/v2/gstat", ServiceConsts.ROLE_BOSS),
            new UriPrefixWithRoles("/gstat", ServiceConsts.ROLE_BOSS),
    };

    private ServiceContextFactory serviceContextFactory;

    public RoleBasedPermissionInterceptor() {
    }

    public ServiceContextFactory getServiceContextFactory() {
        return serviceContextFactory;
    }

    @Autowired
    public void setServiceContextFactory(ServiceContextFactory serviceContextFactory) {
        this.serviceContextFactory = serviceContextFactory;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ServiceContext ctx = serviceContextFactory.create(request);
        checkRolePermission(ctx, request.getRequestURI());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

    private void checkRolePermission(ServiceContext ctx, String uri) {
        int roles = ctx.getRoles();
        for (UriPrefixWithRoles prefixWithRoles : URI_PREFIX_WITH_ROLES_TABLE) {
            if (StringUtils.startsWith(uri, prefixWithRoles.uriPrefix)) {
                if ((roles & prefixWithRoles.roles) == 0)
                    throw new ServiceException(Errors.E_ILLEGAL_ROLE, "Illegal role permission");
            }
        }
    }

    private static class UriPrefixWithRoles {
        final String uriPrefix;
        final int roles;

        private UriPrefixWithRoles(String uriPrefix, int roles) {
            this.uriPrefix = uriPrefix;
            this.roles = roles;
        }
    }
}
