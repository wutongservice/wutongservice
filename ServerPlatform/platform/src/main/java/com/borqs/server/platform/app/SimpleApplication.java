package com.borqs.server.platform.app;


import java.util.HashMap;
import java.util.Map;

public class SimpleApplication extends ApplicationBase {
    private final Map<Integer, String> appSecrets = new HashMap<Integer, String>();

    public SimpleApplication() {
    }

    @Override
    public void init() {
        appSecrets.put(1, "appSecret1");
        appSecrets.put(2, "appSecret2");
        appSecrets.put(3, "appSecret3");
        appSecrets.put(4, "appSecret4");
        appSecrets.put(9, "appSecret9");
        appSecrets.put(10, "appSecret10");

        // 其他，临时的
        appSecrets.put(10001, "thO;deVA50");
    }

    @Override
    public void destroy() {
        appSecrets.clear();
    }

    @Override
    protected String findAppSecret(String appId) {
        int appId0 = Integer.parseInt(appId);
        String r = appSecrets.get(appId0);
        if (r == null)
            throw new AppException("Can't find app secret for application '%s'", appId);
        return r;
    }
    
    @Override
    protected String findQiupuMinVersion0() {
        return "283";
    }
}
