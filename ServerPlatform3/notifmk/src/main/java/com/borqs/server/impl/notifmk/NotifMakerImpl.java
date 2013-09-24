package com.borqs.server.impl.notifmk;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.maker.AbstractMaker;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

public class NotifMakerImpl extends AbstractMaker<Notification> {
    @Override
    public String[] getTemplates() {
        return new String[] {
                MakerTemplates.NOTIF_CREATE_ACCOUNT
        };
    }

    @Override
    public Notification make(Context ctx, String template, Record opts) {
        if (StringUtils.equals(template, MakerTemplates.NOTIF_CREATE_ACCOUNT))
            return CreateAccountNotifMaker.make(ctx, opts);
        else
            throw new IllegalArgumentException();
    }
}
