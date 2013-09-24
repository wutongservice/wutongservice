package com.borqs.server.market.services.impl;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.services.AccountProviderService;

public class UnimplementedAccountProvider implements AccountProviderService {
    private static final UnimplementedAccountProvider instance = new UnimplementedAccountProvider();

    private UnimplementedAccountProvider() {
    }

    public static UnimplementedAccountProvider getInstance() {
        return instance;
    }

    @Override
    public String signUp(ServiceContext ctx, String id, String password) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String deleteUser(ServiceContext ctx, String id) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String signIn(ServiceContext ctx, String id, String password) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String signOut(ServiceContext ctx, String ticket) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUserId(ServiceContext ctx, String ticket) throws ServiceException {
        throw new UnsupportedOperationException();
    }
}
