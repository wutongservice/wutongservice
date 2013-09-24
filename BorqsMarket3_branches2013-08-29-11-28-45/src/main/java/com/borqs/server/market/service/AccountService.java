package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.record.Record;

import java.util.Map;

public interface AccountService extends ServiceConsts {

    Record signup(ServiceContext ctx, Record account);

    Record setPassword(ServiceContext ctx, String id, String password, boolean includeDisabled);

    Record updateAccount(ServiceContext ctx, Record account, boolean includeDisabled);

    Record disableAccount(ServiceContext ctx, String id, boolean disabled);

    Record signin(ServiceContext ctx, String signinId, String password);

    Record activeAccount(ServiceContext ctx, Params params);

    Record signout(ServiceContext ctx, String ticket);

    Record getAccount(ServiceContext ctx, String id, boolean includeDisabled);

    Record getAccountIdByTicket(ServiceContext ctx, String ticket, boolean includeDisabled);

    Record getAccountByEmail(ServiceContext ctx, String email, boolean includeDisabled);

    int getRoles(ServiceContext ctx);

    Map<String, String> getAccountDisplayNames(ServiceContext ctx, String[] ids, String defaultName);

    long getAccountsCount(ServiceContext ctx);


    //----------------find record from wutong_user----------------
    Record getWutongUserByEmail(ServiceContext ctx, String email, boolean includeDisabled);

    Record getWutongUserByUserId(ServiceContext ctx, String email, boolean includeDisabled);
}
