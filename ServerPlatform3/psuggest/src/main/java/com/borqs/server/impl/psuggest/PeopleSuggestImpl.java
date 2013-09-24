package com.borqs.server.impl.psuggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.psuggest.*;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ParamChecker;

import java.util.List;

public class PeopleSuggestImpl implements PeopleSuggestLogic {
    private static final Logger L = Logger.get(PeopleSuggestImpl.class);
    private List<SuggestHook> createSuggestHooks;
    private static final int REASON_SUGGEST_LIMIT = 10;

    // logic
    private AccountLogic account;
    private FriendLogic friend;

    public PeopleSuggestImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    // db
    private final PeopleSuggestDb db = new PeopleSuggestDb();

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getPsuggestTable() {
        return db.getPsuggestTable();
    }

    public void setPsuggestTable(Table psuggestTable) {
        db.setPsuggestTable(psuggestTable);
    }

    public void setCreateSuggestHooks(List<SuggestHook> createSuggestHooks) {
        this.createSuggestHooks = createSuggestHooks;
    }

    @Override
    public void create(Context ctx, PeopleSuggest... suggests) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "create",
                ctx, "suggests", suggests);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("suggests", suggests);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (suggests.length == 0)
                return;
            HookHelper.before(createSuggestHooks, ctx, suggests);
            db.create(ctx, suggests);
            HookHelper.after(createSuggestHooks, ctx, suggests);
            for (PeopleSuggest ps : suggests) {
                if (ps != null && ps.getReason() == SuggestionReasons.RECOMMENDER_USER)
                    OpLine.append(ctx, Actions.RECOMMEND, "", ps.getSuggested());
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void accept(Context ctx, PeopleId... suggested) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "accept",
                ctx, "suggested", suggested);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("suggested", suggested);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (suggested.length > 0) {
                db.deal(ctx, Status.ACCEPTED, DateHelper.nowMillis(), suggested);
                OpLine.append(ctx, Actions.ACCEPT, "", suggested);
            }

            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void reject(Context ctx, PeopleId... suggested) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "reject",
                ctx, "suggested", suggested);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("suggested", suggested);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            if (suggested.length > 0) {
                db.deal(ctx, Status.REJECTED, DateHelper.nowMillis(), suggested);
                OpLine.append(ctx, Actions.REJECT, "", suggested);
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleSuggests getSuggested(Context ctx, long userId, int limit) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "getSuggested",
                ctx, "userId", userId, "limit", limit);

        try {
            ParamChecker.notNull("ctx", ctx);

            PeopleSuggests suggests = new PeopleSuggests();

            for (int i = 0; i < SuggestionReasons.REASONS.length; i++) {
                int size = suggests.size();
                int limit1 = limit - size;
                if (limit1 > 0) {
                    // TODO: fix bug when limit = 1
                    PeopleSuggests suggs = new PeopleSuggests();
                    int limit2 = REASON_SUGGEST_LIMIT < limit1 ? REASON_SUGGEST_LIMIT : limit1;
                    int reason = SuggestionReasons.REASONS[i];
                    int limit0 = (reason == SuggestionReasons.RECOMMENDER_USER || reason == SuggestionReasons.REQUEST_ATTENTION)
                            ? limit1 : limit2;
                    suggs.addAll(db.gets(ctx, userId, reason, Status.INIT, limit0));

                    //exclude
                    for (PeopleSuggest suggest : suggs) {
                        PeopleId suggested = suggest.getSuggested();
                        if (!friend.hasFriend(ctx, userId, suggested)
                                && !friend.hasAllFriendsInCircles(ctx, userId, new int[]{Circle.CIRCLE_BLOCKED}, suggested)
                                && userId != Long.parseLong(suggested.id)) {
                            suggests.add(suggest);
                        }
                    }
                } else {
                    break;
                }
            }

            LC.endCall();
            return suggests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleSuggests getAccepted(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "getAccepted",
                ctx, "userId", userId);

        try {
            ParamChecker.notNull("ctx", ctx);

            PeopleSuggests suggests = db.gets(ctx, userId, SuggestionReasons.REASON_NONE, Status.ACCEPTED, 0);
            LC.endCall();
            return suggests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleSuggests getRejected(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "getRejected",
                ctx, "userId", userId);

        try {
            ParamChecker.notNull("ctx", ctx);

            PeopleSuggests suggests = db.gets(ctx, userId, SuggestionReasons.REASON_NONE, Status.REJECTED, 0);
            LC.endCall();
            return suggests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public PeopleSuggests getPeopleSource(Context ctx, long userId,long id) {
        final LogCall LC = LogCall.startCall(L, PeopleSuggestImpl.class, "getPeopleSource",
                ctx, "userId", userId,"id",id);

        try {
            ParamChecker.notNull("ctx", ctx);

            PeopleSuggests suggests = db.getPeopleSource(ctx, userId,id, SuggestionReasons.RECOMMENDER_USER, Status.ACCEPTED, 0);
            LC.endCall();
            return suggests;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
