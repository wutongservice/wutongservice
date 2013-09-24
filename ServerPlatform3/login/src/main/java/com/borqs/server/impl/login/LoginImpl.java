package com.borqs.server.impl.login;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.cache.Cache;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.login.LoginHook;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.feature.login.LoginResult;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Random;

public class LoginImpl implements LoginLogic {

    // logic
    private AccountLogic account;
    private CibindLogic cibind;

    // db
    private final TicketDb db = new TicketDb();

    // cache
    private final TicketCache cache = new TicketCache();

    // hook
    private List<LoginHook> loginHooks;
    private List<LoginHook> logoutHooks;

    public LoginImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getTicketTable() {
        return db.getTicketTable();
    }

    public void setTicketTable(Table ticketTable) {
        this.db.setTicketTable(ticketTable);
    }

    public void setTicketCache(Cache ticketCache) {
        this.cache.cache = ticketCache;
    }

    public Cache getTicketCache() {
        return this.cache.cache;
    }

    public boolean isTicketCacheEnabled() {
        return cache.flag;
    }

    public void setTicketCacheEnabled(boolean ticketCacheEnabled) {
        cache.flag = ticketCacheEnabled;
    }

    public List<LoginHook> getLoginHooks() {
        return loginHooks;
    }

    public void setLoginHooks(List<LoginHook> loginHooks) {
        this.loginHooks = loginHooks;
    }

    public List<LoginHook> getLogoutHooks() {
        return logoutHooks;
    }

    public void setLogoutHooks(List<LoginHook> logoutHooks) {
        this.logoutHooks = logoutHooks;
    }

    @Override
    public long whoLogined(Context ctx, String ticket) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("ticket", ticket);

        if (cache.enabled()) {
            long userId = cache.getUserId(ticket);
            if (userId == 0) {
                userId = db.who(ctx, ticket);
                if (userId != 0)
                    cache.setUserId(ticket, userId);
            }
            return userId;
        } else {
            return db.who(ctx, ticket);
        }
    }

    @Override
    public boolean hasTicket(Context ctx, String ticket) {
        return whoLogined(ctx, ticket) != 0L;
    }

    private long getUserIdByName(Context ctx, String name) {
        long userId = cibind.whoBinding(ctx, name);
        if (userId > 0)
            return userId;

        if (!StringUtils.isNumeric(name))
            return 0;

        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String generateTicket(String loginName) {
        return Encoders.toBase64(loginName + "_" + DateHelper.nowMillis() + "_" + new Random().nextInt(10000));
    }


    @Override
    public long validatePassword(Context ctx, String name, String password) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("name", name);
        ParamChecker.notNull("password", password);

        return validatePassword0(ctx, name, password);
    }

    private long validatePassword0(Context ctx, String name, String password) {
        long userId = getUserIdByName(ctx, name);
        if (userId <= 0)
            return 0;

        String pwd = account.getPassword(ctx, userId);
        if (pwd == null)
            return 0;

        if (!StringUtils.equals(password, pwd))
            return 0;

        return userId;
    }

    @Override
    public LoginResult login(Context ctx, String name, String password, int app) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("name", name);
        ParamChecker.notNull("password", password);

        HookHelper.before(loginHooks, ctx, new LoginHook.Info(name, 0L, null));

        long userId = validatePassword0(ctx, name, password);
        if (userId <= 0)
            throw new ServerException(E.INVALID_USER_OR_PASSWORD, "User name or password error");

        String ticket = generateTicket(name);
        db.createTicket(ctx, userId, ticket, app);
        OpLine.append(ctx, Actions.LOGIN, name, ctx.getViewerAsPeople());
        if (cache.enabled())
            cache.setUserId(ticket, userId);

        HookHelper.after(loginHooks, ctx, new LoginHook.Info(name, userId, ticket));

        return new LoginResult(userId, ticket);
    }

    @Override
    public boolean logout(Context ctx, String ticket) {
        ParamChecker.notNull("ctx", ctx);
        ParamChecker.notNull("ticket", ticket);

        HookHelper.before(logoutHooks, ctx, new LoginHook.Info(null, 0L, ticket));

        long userId = whoLogined(ctx, ticket);
        boolean r = db.deleteTicket(ctx, ticket);
        OpLine.append(ctx, Actions.LOGOUT, ticket, ctx.getViewerAsPeople());
        if (cache.enabled())
            cache.deleteUserId(ticket);

        HookHelper.after(logoutHooks, ctx, new LoginHook.Info(null, userId, ticket));

        return r;
    }
}
