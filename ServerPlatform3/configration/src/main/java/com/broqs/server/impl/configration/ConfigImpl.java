package com.broqs.server.impl.configration;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.configuration.Config;
import com.borqs.server.platform.feature.configuration.ConfigHook;
import com.borqs.server.platform.feature.configuration.ConfigLogic;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sfs.SFSHelper;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;
import com.borqs.server.platform.util.ParamChecker;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public class ConfigImpl implements ConfigLogic {
    private static final Logger L = Logger.get(ConfigImpl.class);

    // logic
    private AccountLogic account;

    // db
    private final ConfigDb db = new ConfigDb();

    // hooks
    private List<ConfigHook> configHooks;


    public ConfigImpl() {
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


    public void setConfigTable(Table configTable) {
        db.setConfigTable(configTable);
    }


    @Override
    public boolean saveConfig(Context ctx, Config config) {
        final LogCall LC = LogCall.startCall(L, ConfigImpl.class, "saveConfig", ctx, "audio", config);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("audio", config);

            db.saveConfig(ctx, config);
            LC.endCall();
            return true;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Config> getConfig(Context ctx, long userId, String key, int version_code) {
        final LogCall LC = LogCall.startCall(L, ConfigImpl.class, "getConfig", ctx, "userId", userId, "key", key, "version_code", version_code);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userId", userId);
            ParamChecker.notNull("key", key);
            ParamChecker.notNull("version_code", version_code);

            List<Config> configList = db.getConfigs(ctx, userId, key, version_code);
            LC.endCall();
            return configList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public List<Config> getConfigsById(Context ctx, long userId) {
        final LogCall LC = LogCall.startCall(L, ConfigImpl.class, "getConfig", ctx, "userId", userId);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userId", userId);

            List<Config> configList = db.getConfigByUserIds(ctx, userId);
            LC.endCall();
            return configList;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean deleteConfig(Context ctx, long userId, String key, int version_code) {
        final LogCall LC = LogCall.startCall(L, ConfigImpl.class, "saveConfig", ctx, "userId", userId, "key", key, "version_code", version_code);
        try {
            ParamChecker.notNull("ctx", ctx);
            ParamChecker.notNull("userId", userId);
            ParamChecker.notNull("key", key);
            ParamChecker.notNull("version_code", version_code);

            boolean b = db.deleteConfig(ctx, userId, key, version_code);
            LC.endCall();
            return b;
        } catch (RuntimeException e) {
            LC.endCall(e);
            throw e;
        }
    }

    @Override
    public boolean uploadConfig(Context ctx, SFS sfs, FileItem fileItem, String name) {
        try {
            SFSHelper.saveUpload(sfs, fileItem, name);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("upload file error");
        }
    }
}
