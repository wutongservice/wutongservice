package com.borqs.server.impl.setting;


import com.borqs.server.platform.cache.Cache;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.opline.OpLine;
import com.borqs.server.platform.feature.setting.DeleteSettingHook;
import com.borqs.server.platform.feature.setting.SetSettingHook;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.hook.HookHelper;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ParamChecker;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class SettingImpl implements SettingLogic {

    private static final Logger L = Logger.get(SettingImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final SettingDb db = new SettingDb();

    // cache
    private final SettingCache cache = new SettingCache();

    // hook
    private List<SetSettingHook> setSettingHooks;
    private List<DeleteSettingHook> deleteSettingHooks;


    public SettingImpl() {
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

    public Table getSettingTable() {
        return db.getSettingTable();
    }

    public void setSettingTable(Table settingTable) {
        db.setSettingTable(settingTable);
    }

    public Cache getSettingCache() {
        return cache.cache;
    }

    public void setSettingCache(Cache settingCache) {
        this.cache.cache = settingCache;
    }

    public boolean isSettingCacheEnabled() {
        return cache.flag;
    }

    public void setSettingCacheEnabled(boolean settingCacheEnabled) {
        this.cache.flag = settingCacheEnabled;
    }

    public List<SetSettingHook> getSetSettingHooks() {
        return setSettingHooks;
    }

    public void setSetSettingHooks(List<SetSettingHook> setSettingHooks) {
        this.setSettingHooks = setSettingHooks;
    }

    public List<DeleteSettingHook> getDeleteSettingHooks() {
        return deleteSettingHooks;
    }

    public void setDeleteSettingHooks(List<DeleteSettingHook> deleteSettingHooks) {
        this.deleteSettingHooks = deleteSettingHooks;
    }

    @Override
    public void sets(Context ctx, Map<String, String> setting) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "sets", ctx,
                "setting", setting);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("setting", setting);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            HookHelper.before(setSettingHooks, ctx, new SetSettingHook.Info(userId, setting));

            if (setting.isEmpty())
                return;

            db.sets(ctx, setting);
            OpLine.append(ctx, Actions.UPDATE, JsonHelper.toJson(setting, false), ctx.getViewerAsPeople());

            if (cache.enabled()) {
                cache.sets(userId, setting);
            }

            HookHelper.after(setSettingHooks, ctx, new SetSettingHook.Info(userId, setting));
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<String, String> gets(Context ctx, long userId, String[] keys, Map<String, String> def) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "gets", ctx,
                "userId", userId, "keys", keys, "def", def);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("keys", keys);

            if (userId <= 0 || !account.hasUser(ctx, userId)) {
                LC.endCall();
                return null;
            }

            if (keys.length == 0) {
                LC.endCall();
                return new LinkedHashMap<String, String>();
            }

            Map<String, String> m;
            if (cache.enabled()) {
                ArrayList<String> missingKeys = new ArrayList<String>();
                m = cache.gets(userId, keys, missingKeys);
                if (!missingKeys.isEmpty()) {
                    Map<String, String> mm = db.gets(ctx, userId, missingKeys.toArray(new String[missingKeys.size()]));
                    if (MapUtils.isNotEmpty(mm)) {
                        cache.sets(userId, mm);
                        m.putAll(mm);
                    }
                }
            } else {
                m = db.gets(ctx, userId, keys);
            }

            fillDefault(m, keys, def);
            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public Map<String, String> getsByStartsWith(Context ctx, long userId, String keyStartsWith, Map<String, String> def) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "getsByStartsWith", ctx,
                "userId", userId, "keyStartsWith", keyStartsWith, "def", def);

        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("keyStartsWith", keyStartsWith);

            if (userId <= 0 || !account.hasUser(ctx, userId)) {
                LC.endCall();
                return null;
            }

            if (StringUtils.isBlank(keyStartsWith)) {
                LC.endCall();
                return new LinkedHashMap<String, String>();
            }

            Map<String, String> m;
            m = db.getsByStartsWith(ctx, userId, keyStartsWith);

            Set<String> set = m.keySet();
            fillDefault(m, set.toArray(new String[set.size()]), def);
            LC.endCall();
            return m;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    private void fillDefault(Map<String, String> r, String[] keys, Map<String, String> def) {
        if (MapUtils.isEmpty(def))
            return;

        for (String key : keys) {
            if (!r.containsKey(key)) {
                if (def.containsKey(key))
                    r.put(key, def.get(key));
            }
        }
    }

    @Override
    public void set(Context ctx, String key, String value) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "set", ctx,
                "key", key, "value", value);

        try {
            sets(ctx, CollectionsHelper.of(key, value));
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public String get(Context ctx, long userId, String key, String def) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "get", ctx,
                "key", key, "def", def);
        try {
            Map<String, String> m = gets(ctx, userId, new String[]{key}, CollectionsHelper.of(key, def));
            String v = MapUtils.getString(m, key, def);
            LC.endCall();
            return v;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public void delete(Context ctx, String... keys) {
        final LogCall LC = LogCall.startCall(L, SettingImpl.class, "delete", ctx,
                "keys", keys);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("keys", keys);

            long userId = ctx.getViewer();
            AccountHelper.checkUser(account, ctx, userId);

            HookHelper.before(deleteSettingHooks, ctx, keys);

            if (keys.length > 0) {
                db.delete(ctx, keys);
                OpLine.append(ctx, Actions.DESTROY, StringUtils.join(keys, ","), ctx.getViewerAsPeople());
                if (cache.enabled()) {
                    cache.delete(userId, keys);
                }

                HookHelper.after(deleteSettingHooks, ctx, keys);
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }
}
