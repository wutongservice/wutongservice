package com.borqs.server.impl.notifmk.v1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.DateInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.app.AppIds;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.sender.notif.Notification;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.List;


public class UpdateAccount1NotifMaker {

    private FriendLogic friend;
    private SettingLogic setting;
    private AccountLogic account;

    private String body;

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public void setSetting(SettingLogic setting) {
        this.setting = setting;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public Notification make(Context ctx, Record opts) {
        String locale = ctx.getLocale();
        if (StringUtils.isEmpty(locale))
            locale = "zh";

        User user0 = (User) opts.get("user");
        getUpdateColumn(user0, locale);
        User user = account.getUser(ctx, User.STANDARD_COLUMNS, user0.getUserId());
        Notification n = Notification.forSend(MakerTemplates.NOTIF_UPDATE_PROFILE, AppIds.WUTONG, "");
        n.setSenderId(String.valueOf(user.getUserId()));
        n.setReplace(true);

        String titil = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.title");
        titil = MessageFormat.format(titil, user.getDisplayName());
        n.setTitle(titil);
        n.setUri("borqs://profile/details?uid=" + user.getUserId() + "&tab=" + 2);

        String titleHtml = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", locale, "profile.update.notif.title");
        n.setTitleHtml(MessageFormat.format(titleHtml, TargetInfoFormat.ANDROID_LINK.formatTargets(ctx, 3, Target.USER, new long[]{user.getUserId()})));
        n.setBody(body);
        n.setBodyHtml("");
        n.setReceiverId(getReceiveIds(ctx));
        n.setAction("android.intent.action.VIEW");
        return n;
    }

    public String getReceiveIds(Context ctx) {
        Page page = new Page(0, 1000);
        long[] followers = friend.getFollowers(ctx, PeopleId.user(ctx.getViewer()), page);
        List<Long> list = CollectionsHelper.toLongList(followers);
        for (long l : followers) {
            String value = setting.get(ctx, l, MakerTemplates.NOTIF_UPDATE_PROFILE, "");
            if ("1".equals(value))
                list.remove(l);
        }
        list.remove(ctx.getViewer());
        return StringUtils.join(list, ",");
    }


    private String getUpdateColumn(User user, String lang) {

        Record r = new Record();
        for (String s : user.getPropertyColumnsSet()) {
            r.set(s, user.getProperty(s, ""));
        }

        String str = "";
        body = "";

        if (r.has("name")) {
            String displayName = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.displayname");
            str += displayName + ", ";
            //                objectId = "display_name";
            body += displayName + ": " + r.getString("name") + ", ";
        }
        if (r.has("nick_name")) {
            String nickName = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.nickname");
            str += nickName + ", ";
            //                objectId = "nick_name";
            body += nickName + ": " + r.getString("nick_name") + ", ";
        }
        if (user.getProfile() != null) {
            String sex = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.gender");
            str += sex + ", ";
            //                objectId = "gender";
            String mGender = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.hidegender");
            String gender = user.getProfile().getGender();
            if (StringUtils.equals(gender, "f")) {
                mGender = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.female");
            } else if (StringUtils.equals(gender, "m")) {
                mGender = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.male");
            } else {
                mGender = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.hidegender");
            }
            body += sex + ": " + mGender + ", ";
        }
        if (user.getDate() != null && user.getDate().get(0).getType().equals(DateInfo.TYPE_BIRTHDAY)) {
            String birthday = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.birthday");
            str += birthday + ", ";
            //                objectId = "birthday";
            body += birthday + ": " + user.getDate().get(0).getInfo() + ", ";
        }
        /*if (r.has("interests")) {
            String interests = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.interests");
            str += interests + ", ";
            //                objectId = "interests";
            body += interests + ": " + r.getString("interests") + ", ";
        }
        if (r.has("marriage")) {
            String txtMarriage = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.marriage");
            String married = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.married");
            String unmarried = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.unmarried");
            str += txtMarriage + ", ";
            //                objectId = "marriage";
            String marriage = r.getString("marriage");
            body += txtMarriage + ": " + (StringUtils.equals(marriage, "n") ? unmarried : married) + ", ";
        }
        if (r.has("religion")) {
            String religion = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.religion");
            str += religion + ", ";
            //                objectId = "religion";
            body += religion + ": " + r.getString("religion") + ", ";
        }*/

        if (user.getOrganization() != null) {
            String company = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.company");
            str += company + ", ";
            //                objectId = "company";
            body += company + ": " + user.getOrganization().get(0).getCompany() + ", ";
        }
        if (user.getPhoto() != null) {
            String image = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.image");
            str += image + ", ";
            //                objectId = "image_url";
            body += image + ", ";
        }
        if (user.getTel() != null && user.getEmail() != null) {
            String contact = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.contact");
            str += contact + ", ";
            //                objectId = "contact_info"; 
            body += contact + ", ";
        }
        if (user.getAddress() != null) {
            String address = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.address");
            str += address + ", ";
            //                objectId = "address";
            String street = user.getAddress().get(0).getStreet();

            body += address + ": " + street + ", ";
        }
        if (StringUtils.isNotEmpty((String) (user.getAddon("status", "")))) {
            String status = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.status");
            str += status + ", ";
            //            	objectId = "status";
            //            	tab = "feed";

            body += status + ": " + (String) (user.getAddon("status", "")) + ", ";
        }

        if (StringUtils.isNotBlank(str))
            str = StringUtils.substringBeforeLast(str, ", ");
        else
            str = I18nHelper.getString("com.borqs.server.impl.notifmk.i18n.notif", lang, "profile.update.notif.other");

        if (StringUtils.isNotBlank(body))
            body = StringUtils.substringBeforeLast(body, ", ");
        else
            body = "";

        return str;
    }
}
