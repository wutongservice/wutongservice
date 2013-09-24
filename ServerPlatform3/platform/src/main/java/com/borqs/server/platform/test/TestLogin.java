package com.borqs.server.platform.test;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.feature.login.LoginResult;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestLogin implements LoginLogic {

    private AccountLogic account;
    private final Map<String, TicketEntry> entries = new HashMap<String, TicketEntry>();

    public TestLogin() {
        reset();
    }

    public void reset() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    @Override
    public long whoLogined(Context ctx, String ticket) {
        TicketEntry entry = entries.get(ticket);
        return entry != null ? entry.userId : 0L;
    }

    @Override
    public boolean hasTicket(Context ctx, String ticket) {
        return entries.containsKey(ticket);
    }

    private String getPassword(Context ctx, String name) {
        return StringUtils.isNumeric(name) ? account.getPassword(ctx, Long.parseLong(name)) : "";
    }

    @Override
    public long validatePassword(Context ctx, String name, String password) {
        String pwd = getPassword(ctx, name);
        return StringUtils.equals(pwd, password) ? Long.parseLong(name) : 0L;
    }

    private static String generateTicket(String loginName) {
        return Encoders.toBase64(loginName + "_" + DateHelper.nowMillis() + "_" + new Random().nextInt(10000));
    }

    @Override
    public LoginResult login(Context ctx, String name, String password, int app) {
        long userId = validatePassword(ctx, name, password);
        if (userId <= 0)
            throw new ServerException(E.INVALID_USER_OR_PASSWORD);
        String ticket = generateTicket(name);
        entries.put(ticket, new TicketEntry(ticket, userId, app));
        return new LoginResult(userId, ticket);
    }

    @Override
    public boolean logout(Context ctx, String ticket) {
        if (entries.containsKey(ticket)) {
            entries.remove(ticket);
            return true;
        } else {
            return false;
        }
    }

    private static class TicketEntry {
        public final String ticket;
        public final long userId;
        public final int app;

        private TicketEntry(String ticket, long userId, int app) {
            this.ticket = ticket;
            this.userId = userId;
            this.app = app;
        }
    }
}
