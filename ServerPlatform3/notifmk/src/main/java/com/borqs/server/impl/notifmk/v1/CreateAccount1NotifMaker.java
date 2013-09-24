package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class CreateAccount1NotifMaker {
    private CibindLogic cibind;
    private ContactLogic contact;

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public void setContact(ContactLogic contact) {
        this.contact = contact;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        User user = (User) opts.get("user");
        Notification n = Notification.forSend(MakerTemplates.NOTIF_CREATE_ACCOUNT, AppIds.WUTONG, "");
        String gender = user.getProfile().getGender();
        n.setReplace(true);
        String callName = "";
        if ("m".equals(gender)) {
            callName = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.profile.sex.man");
        } else if ("f".equals(gender)) {
            callName = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.profile.sex.woman");
        } else {
            callName = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.profile.sex.man");
        }

        String title = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "platform.create.accountcreate.notification");
        MessageFormat.format(title, user.getDisplayName(), callName);
        n.setTitle(title);
        n.setTitleHtml(title);

        n.setData(getData(ctx, user));

        n.setSenderId(String.valueOf(user.getUserId()));

        n.setUri("borqs://profile/details?uid=" + user.getUserId() + "&tab=" + 2);
        n.setBody("");
        n.setBodyHtml("");
        n.setReceiverId(getScope(ctx,user));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    private String getScope(Context ctx, User user) {
        BindingInfo[] bis = cibind.getBindings(ctx, user.getUserId());
        List<Long> list = new ArrayList<Long>();
        for (BindingInfo b : bis) {
            Contacts contacts_ = contact.searchContacts(ctx, Reasons.UPLOAD_CONTACTS, b.getInfo());
            filterList(list, contacts_);
        }

        return StringUtils.join(list, ",");
    }

    private void filterList(List<Long> list, Contacts contacts_) {
        if (contacts_.size() > 0) {
            long[] longs = contacts_.getOwners();
            if (longs.length > 0) {
                for(Long l:longs){
                    if(!list.contains(l))
                        list.add(l);
                }
            }
        }
    }

    private String getData(Context ctx, User user) {
        Record r = new Record();
        r.set("user_id", user.getUserId());
        BindingInfo[] bindingInfos = cibind.getBindings(ctx, user.getUserId());
        for (BindingInfo b : bindingInfos) {
            r.set(b.getType(), b.getInfo());
        }
        return r.toJson(false, false);

    }

    /*private String nCreateAccountGetUser(Context ctx) {
        Record r = new Record();
        r.set("user_id", ctx.getViewer());
        BindingInfo[] bi = cibind.getBindings(ctx, ctx.getViewer());
        for (BindingInfo b : bi) {
            if (BindingInfo.EMAIL.equals(b.getType())) {
                r.set("login_email1", b.getInfo());
            } else if (BindingInfo.MOBILE_TEL.equals(b.getType())) {
                r.set("login_phone1", b.getInfo());
            }
        }
        return r.toString();
    }*/
}
