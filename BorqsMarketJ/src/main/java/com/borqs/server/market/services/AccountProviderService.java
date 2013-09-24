package com.borqs.server.market.services;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;

public interface AccountProviderService {

    String signUp(ServiceContext ctx, String id, String password) throws ServiceException;

    String deleteUser(ServiceContext ctx, String id) throws ServiceException;

    String signIn(ServiceContext ctx, String id, String password) throws ServiceException;

    String signOut(ServiceContext ctx, String ticket) throws ServiceException;

    String getUserId(ServiceContext ctx, String ticket) throws ServiceException;
}
