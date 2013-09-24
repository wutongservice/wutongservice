package com.borqs.server.platform.web;


import com.borqs.server.platform.app.GlobalSpringAppContext;

import javax.servlet.*;
import java.io.IOException;

public class GlobalApplicationContextDelegateServlet extends GenericServlet {
    private Servlet proxy;

    @Override
    public void init() throws ServletException {
        String beanId = getInitParameter("bean");
        proxy = (Servlet) GlobalSpringAppContext.getBean(beanId);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        proxy.service(req, res);
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
