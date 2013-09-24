package com.borqs.server.pubapi.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreDocument
public class Setting1Api extends PublicApiSupport {
    private AccountLogic account;

    public SettingLogic getSettingLogic() {
        return settingLogic;
    }

    public void setSettingLogic(SettingLogic settingLogic) {
        this.settingLogic = settingLogic;
    }

    private SettingLogic settingLogic;

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    public Setting1Api() {
    }

    @Route(url = "/preferences/set")
    public void sets(Request req, Response resp) {
        Context ctx = checkContext(req, true);

        Map<String, String> setting = req.getParams();
        setting.remove("sign_method");
        setting.remove("sign");
        setting.remove("appid");
        setting.remove("ticket");

        settingLogic.sets(ctx, setting);
        resp.body(true);
    }

    @Route(url = "/preferences/get_by_starts")
    public void getsByStartsWith(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        
        Map<String, String> setting = settingLogic.getsByStartsWith(ctx, ctx.getViewer(),
                req.checkString("starts"), new LinkedHashMap<String, String>());

        resp.body(RawText.of(JsonHelper.toJson(setting, true)));
    }
}
