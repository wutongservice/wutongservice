package com.borqs.server.wutong.app;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.wutong.WutongErrors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppLogic implements Initializable {
    public static final Logger L = Logger.getLogger(AppLogic.class);

    private final Map<Integer, String> appSecrets = new ConcurrentHashMap<Integer, String>();

    @Override
    public void init() {
        appSecrets.put(1, "appSecret1");
        appSecrets.put(2, "appSecret2");
        appSecrets.put(3, "appSecret3");
        appSecrets.put(9, "appSecret9");
        appSecrets.put(10, "appSecret10");
        appSecrets.put(11, "appSecret11");

        // 其他，临时的
        appSecrets.put(10001, "thO;deVA50");
    }

    @Override
    public void destroy() {
        appSecrets.clear();
    }

    public String getAppSecret(Context ctx, int appId) {
        String r = appSecrets.get(appId);
        if (r == null)
            throw new ServerException(WutongErrors.APPLICATION_NOT_EXISTS, "Can't find app secret for application '%s'", appId);
        return r;
    }
}
