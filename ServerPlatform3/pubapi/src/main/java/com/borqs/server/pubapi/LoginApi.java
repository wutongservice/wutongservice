package com.borqs.server.pubapi;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.feature.login.LoginResult;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;


@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class LoginApi {
    protected LoginLogic login;

    public LoginApi() {
    }

    public LoginLogic getLogin() {
        return login;
    }

    public void setLogin(LoginLogic login) {
        this.login = login;
    }

    /**
     * 使用用户名和密码进行登录
     *
     * @group Account/User
     * @login n
     * @http-param name 登录使用的用户名，可以为borqs_id，或者是已经绑定的电话或者邮件
     * @http-param password 登录用的密码，为进行md5后的大写格式
     * @http-param appid:0 登录使用的appid，为0表示未指定应用
     * @http-return 登录结果，里面包含user_id与登录后的ticket
     * @http-example {
     * "user_id" : 10001,
     * "ticket" : "MTAwMTJfMTMzODQ1MTA4NDQwNl8zNjQw"
     * }
     */
    @Route(url = {"/account/login"})
    public void login(Request req, Response resp) {
        Context ctx = Context.create();
        LoginResult lr = login.login(ctx, req.checkString("name"), req.checkString("password"), req.getInt("appid", 0));
        resp.body(lr);
    }

    /**
     * 使用一个ticket退出此次登录
     *
     * @group Account/User
     * @http-param ticket 要退出此次登录的ticket
     * @http-return true
     * @http-example {
     * "result":true
     * }
     */
    @Route(url = {"/account/logout"})
    public void logout(Request req, Response resp) {
        Context ctx = Context.create();
        String ticket = req.checkString("ticket");
        ctx.setViewer(login.whoLogined(ctx, ticket));
        ctx.checkLogined();

        login.logout(ctx, ticket);
        resp.body(true);
    }
}
