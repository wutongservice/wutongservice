package com.borqs.server.service.platform;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.app.AppLogic;
import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.feature.login.LoginLogic;
import org.apache.avro.AvroRemoteException;

public class Platform extends ConfigurableBase {
    private AppLogic app;
    private LoginLogic login;

    public Platform(AppLogic app, LoginLogic login) {
        this.app = app;
        this.login = login;
    }

    public String checkSignAndTicket(QueryParams qp) throws AvroRemoteException {
        Context ctx = Context.createDummy();

        String ticket = qp.getString("ticket", null);
        if (ticket == null)
            throw new ServerException(E.INVALID_TICKET, "Missing ticket");

        long viewerId;
        viewerId = login.whoLogined(ctx, ticket);
        if (viewerId <= 0)
            throw new ServerException(E.INVALID_TICKET, "Illegal ticket");

        int appId = (int)qp.getInt("appid", App.APP_NONE);
        if (appId > 0) {
            ctx.setApp(appId);
            String signMethod = qp.getString("sign_method", "md5");
            String sign = qp.getString("sign", null);
            if (sign != null) {
                String appSecret = app.getApp(ctx, appId).getSecret();
                String actualSign = AppSign.sign(signMethod, appSecret, qp.keySet());
                if (!actualSign.equals(sign))
                    throw new ServerException(E.INVALID_SIGN);
            }
        }

        return Long.toString(viewerId);
    }

}
