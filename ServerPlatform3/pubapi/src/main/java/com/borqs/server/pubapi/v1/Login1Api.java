package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.feature.login.LoginResult;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

@IgnoreDocument
public class Login1Api extends PublicApiSupport {
    protected LoginLogic login;
    protected CibindLogic cibind;

    public Login1Api() {
    }

    public LoginLogic getLogin() {
        return login;
    }

    public void setLogin(LoginLogic login) {
        this.login = login;
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    // for sync
    @Route(url = "/sync/webagent/accountrequest/normallogin")
    public void syncLogic(Request req, Response resp) {
        Record rec = Record.fromJson(req.checkString("data"));
        String r = login0(rec.checkGetString("name"), rec.checkGetString("pass"), 0);
        resp.body(r);
    }

    @Route(url = "/account/login")
    public void login(Request req, Response resp) {
        String r = login0(req.checkString("login_name"), req.checkString("password"), req.getInt("appid", 0));
        resp.body(RawText.of(r));
    }

    private String login0(String loginName, String password, int appId) {
        Context ctx = Context.create();
        LoginResult lr = login.login(ctx, loginName, password, appId);
        if (lr.userId <= 0)
            throw new ServerException(E.INVALID_USER);

        Record rec = Record.of(new Object[][] {
                {"user_id", lr.userId},
                {"ticket", lr.ticket},
                {"display_name", ""},
                {"login_name", loginName}
        });
        return rec.toJson();
    }


    @Route(url = "/account/logout")
    public void logout(Request req, Response resp) {
        Context ctx = Context.create();
        String ticket = req.checkString("ticket");
        ctx.setViewer(login.whoLogined(ctx, ticket));
        ctx.checkLogined();

        login.logout(ctx, ticket);
        resp.body(true);
    }

    @Route(url = "/account/who")
    public void who(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        long userId = 0;
        if (req.has("ticket")) {
            userId = login.whoLogined(ctx, req.checkString("ticket"));
        } else if (req.has("login")) {
            userId = cibind.whoBinding(ctx, req.checkString("login"));
        } else {
            throw new ServerException(E.PARAM, "Missing 'ticket' or 'login'");
        }
        resp.body(userId);
    }
}
