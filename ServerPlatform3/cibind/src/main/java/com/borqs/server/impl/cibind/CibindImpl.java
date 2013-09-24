package com.borqs.server.impl.cibind;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindHook;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class CibindImpl implements CibindLogic {
    private static final Logger L = Logger.get(CibindImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final CibindDb db = new CibindDb();

    // hooks
    private List<CibindHook> bindHooks;
    private List<CibindHook> unbindHooks;

    // limits
    private Map<String, Integer> limits;

    public CibindImpl() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public SqlExecutor getSqlExecutor() {
        return db.getSqlExecutor();
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        db.setSqlExecutor(sqlExecutor);
    }

    public Table getCibindTable() {
        return db.getCibindTable();
    }

    public void setCibindTable(Table cibindTable) {
        db.setCibindTable(cibindTable);
    }

    public List<CibindHook> getBindHooks() {
        return bindHooks;
    }

    public void setBindHooks(List<CibindHook> bindHooks) {
        this.bindHooks = bindHooks;
    }

    public List<CibindHook> getUnbindHooks() {
        return unbindHooks;
    }

    public void setUnbindHooks(List<CibindHook> unbindHooks) {
        this.unbindHooks = unbindHooks;
    }

    public Map<String, Integer> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Integer> limits) {
        this.limits = limits;
    }


    private int getBindingLimit(String type) {
        if (MapUtils.isEmpty(limits))
            return 0;

        Integer limit = limits.get(type);
        if (limit == null)
            return 0;

        return limit > 0 ? limit : 0;
    }

    @Override
    public long whoBinding(Context ctx, String info) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "whoBinding", ctx, "info", info);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("info", info);

            long r = db.whoBinding(ctx, info);
            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<String, Long> whoBinding(Context ctx, String... infos) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "whoBinding", ctx, "infos", infos);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("infos", infos);

            Map<String, Long> m = new LinkedHashMap<String, Long>();
            if (infos.length == 0)
                return m;

            m = db.whoBinding(ctx, infos);
            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasBinding(Context ctx, String info) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "hasBinding", ctx, "info", info);
        try {
            long userId = whoBinding(ctx, info);
            LC.endCall();
            return userId > 0;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean hasBinding(Context ctx, long userId, String info) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "hasBinding", ctx, "info", info);
        try {
            long userId2 = whoBinding(ctx, info);
            LC.endCall();
            return userId2 > 0 && userId2 == userId;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void bind(Context ctx, BindingInfo info) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "bind", ctx, "info", info);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("info", info);
            ParamChecker.notBlank("info.info", info.getInfo());

            String type = info.getType();

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            HookHelper.before(bindHooks, ctx, new CibindHook.Info(userId, info.getType(), info.getInfo()));

            int limit = getBindingLimit(type);
            if (limit <= 0)
                throw new ServerException(E.INVALID_BINDING_TYPE, "Max binding count");

            int bindingCount = getBindingCount(ctx, userId, type);
            if (bindingCount >= limit)
                throw new ServerException(E.TOO_MANY_BINDING, "Max binding count");

            db.bind(ctx, info);
            OpLine.append(ctx, Actions.CI_BIND, info.getInfo(), ctx.getViewerAsPeople());

            HookHelper.after(bindHooks, ctx, new CibindHook.Info(userId, info.getType(), info.getInfo()));
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void bind(Context ctx, String type, String info) {
        bind(ctx, new BindingInfo(type, info));
    }

    @Override
    public boolean unbind(Context ctx, String info) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "unbind", ctx, "info", info);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("info", info);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            HookHelper.before(unbindHooks, ctx, new CibindHook.Info(userId, null, info));

            boolean r = db.unbind(ctx, info);
            OpLine.append(ctx, Actions.CI_UNBIND, info, ctx.getViewerAsPeople());

            HookHelper.after(unbindHooks, ctx, new CibindHook.Info(userId, null, info));
            LC.endCall();
            return r;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<Long, BindingInfo[]> getBindings(Context ctx, long[] userIds) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "bind", ctx, "userIds", userIds);
        try {
            ParamChecker.notNull("ctx", ctx);

            if (ArrayUtils.isEmpty(userIds)) {
                LC.endCall();
                return new HashMap<Long, BindingInfo[]>();
            } else {
                Map<Long, BindingInfo[]> m = db.getBindingInfo(ctx, userIds);
                for (long userId : userIds) {
                    if (!m.containsKey(userId))
                        m.put(userId, new BindingInfo[0]);
                }
                LC.endCall();
                return m;
            }

        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public BindingInfo[] getBindings(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "bind", ctx, "userId", userId);
        try {
            Map<Long, BindingInfo[]> m = getBindings(ctx, new long[]{userId});
            BindingInfo[] bis = m.get(userId);
            LC.endCall();
            return bis != null ? bis : new BindingInfo[0];
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private int getBindingCount(Context ctx, long userId, String type) {
        BindingInfo[] bis = getBindings(ctx, userId);
        if (ArrayUtils.isEmpty(bis))
            return 0;

        int n = 0;
        for (BindingInfo bi : bis) {
            if (StringUtils.equals(type, bi.getType()))
                n++;
        }
        return n;
    }

    @Override
    public String[] getBindings(Context ctx, long userId, String type) {
        final LogCall LC = LogCall.startCall(L, CibindImpl.class, "bind", ctx, "userId", userId, "type", type);
        try {
            BindingInfo[] cis = getBindings(ctx, userId);
            if (ArrayUtils.isEmpty(cis)) {
                LC.endCall();
                return new String[0];
            } else {
                ArrayList<String> l = new ArrayList<String>();
                if (type != null) {
                    for (BindingInfo bi : cis) {
                        if (StringUtils.equals(type, bi.getType()))
                            l.add(bi.getInfo());
                    }
                } else {
                    for (BindingInfo bi : cis)
                        l.add(bi.getInfo());
                }
                LC.endCall();
                return l.toArray(new String[l.size()]);
            }
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
