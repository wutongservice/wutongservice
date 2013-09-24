package com.borqs.server.impl.psuggest.sched;


import com.borqs.server.platform.app.AppMain;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;

import java.util.Calendar;
import java.util.Date;

public abstract class PeopleSuggestionBuilder implements AppMain {
    private final long MIN_USER_ID = 10001;

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    // logic
    private AccountLogic account;

    protected abstract void handle(long userId);

    @Override
    public void run(String[] args) throws Exception {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(System.currentTimeMillis()));
        int day = c.get(Calendar.DAY_OF_WEEK) - 1;

        long userId = MIN_USER_ID;
        while (account.hasUser(Context.create(), userId)) {
            if ((userId % 7) == day)
                handle(userId);
            userId++;
        }
    }
}
