package com.borqs.server.market.services.impl;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.services.AccountProviderService;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DummyAccountProvider implements AccountProviderService {

    private Map<String, String> userAndPassword = new ConcurrentHashMap<String, String>();

    public DummyAccountProvider() {
        userAndPassword.put("10012", "123456");
    }

    @Override
    public String signUp(ServiceContext ctx, String id, String password) throws ServiceException {
        userAndPassword.put(id, password);
        return id;
    }

    @Override
    public String deleteUser(ServiceContext ctx, String id) throws ServiceException {
        userAndPassword.remove(id);
        return id;
    }

    @Override
    public String signIn(ServiceContext ctx, String id, String password) throws ServiceException {
        if (StringUtils.equals(password, userAndPassword.get(id))) {
            return id;
        } else {
            return null;
        }
    }

    @Override
    public String signOut(ServiceContext ctx, String ticket) throws ServiceException {
        return ticket;
    }

    @Override
    public String getUserId(ServiceContext ctx, String ticket) throws ServiceException {
        return ticket;
    }
}
