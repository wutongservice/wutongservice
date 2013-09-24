package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatibleContact;
import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.feature.friend.*;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import com.borqs.server.platform.feature.setting.SettingHelper;
import com.borqs.server.platform.feature.setting.SettingKeys;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.feature.setting.SettingValues;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import java.util.LinkedHashMap;
import java.util.Map;

@IgnoreDocument
public class Contact1Api extends PeopleSuggest1Api {
    private AccountLogic account;
    private ContactLogic contactLogic;
    private CibindLogic cibind;
    private FriendLogic friend;
    private SettingLogic setting;
    private PeopleSuggestLogic psuggest;

    public SettingLogic getSetting() {
        return setting;
    }

    public void setSetting(SettingLogic setting) {
        this.setting = setting;
    }

    public PeopleSuggestLogic getPsuggest() {
        return psuggest;
    }

    public void setPsuggest(PeopleSuggestLogic psuggest) {
        this.psuggest = psuggest;
    }

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public Contact1Api() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public ContactLogic getContactLogic() {
        return contactLogic;
    }

    public void setContactLogic(ContactLogic contactLogic) {
        this.contactLogic = contactLogic;
    }

    private String[] V1_USER_COLS = new String[] {
            CompatibleContact.V1COL_USER_ID,
            CompatibleContact.V1COL_DISPLAY_NAME,
            CompatibleContact.V1COL_IMAGE_URL
    };
    
    @Route(url = "/socialcontact/upload")
    public void uploadContacts(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long viewerId = ctx.getViewer();
        AccountHelper.checkUser(account, ctx, viewerId);

        String json = Encoders.fromBase64String(req.checkString("contactinfo"));
        Contacts contacts = CompatibleContact.jsonToContacts(json, null, ctx.getViewer(), DateHelper.nowMillis());

        contactLogic.fullUpdate(ctx, contacts.toContactArray());

        LinkedHashMap<String, Contact> m = new LinkedHashMap<String, Contact>();
        for (Contact contact : contacts) {
            m.put(contact.getContent(), contact);
        }

        LinkedHashMap<Contact, User> cuMap = new LinkedHashMap<Contact, User>();
        LinkedHashMap<Contact, Boolean> cfMap = new LinkedHashMap<Contact, Boolean>();
        PeopleSuggests peopleSuggests = new PeopleSuggests();
        PeopleIds peopleIds = new PeopleIds();
        Map<String, Long> bindMap = cibind.whoBinding(ctx, contacts.getContents());
        for (Map.Entry<String, Long> entry : bindMap.entrySet()) {
            long userId = entry.getValue();
            if ((userId > 0) && (userId != viewerId)) {
                Contact contact = m.get(entry.getKey());
                User user = account.getUser(ctx, CompatibleUser.v1ToV2Columns(V1_USER_COLS), userId);
                boolean isFriend = friend.hasFriend(ctx, viewerId, PeopleId.fromId(userId));
                cuMap.put(contact, user);
                cfMap.put(contact, isFriend);
                peopleSuggests.add(PeopleSuggest.of(viewerId, PeopleId.fromId(userId), SuggestionReasons.FROM_ADDRESS_HAVE_BORQSID, ""));
                peopleIds.add(PeopleId.fromId(userId));
            }
        }

        String autoAdd = setting.get(ctx, viewerId, SettingKeys.SOCIAL_CONTACT_AUTO_ADD, SettingValues.CONTACTS_AUTO_ADD_FRIEND_INIT);
        if (StringUtils.equals(autoAdd, SettingValues.OFF)) {
            psuggest.create(ctx, peopleSuggests.toArray(new PeopleSuggest[peopleSuggests.size()]));
        } else {
            friend.addFriendsIntoCircle(ctx, FriendReasons.SOCIAL_CONTACT, peopleIds, Circle.CIRCLE_ADDRESS_BOOK);
            friend.addFriendsIntoCircle(ctx, FriendReasons.SOCIAL_CONTACT, peopleIds, Circle.CIRCLE_ACQUAINTANCE);
            
            if (StringUtils.equals(autoAdd, SettingValues.CONTACTS_AUTO_ADD_FRIEND_INIT))
                SettingHelper.toggle(ctx, setting, SettingKeys.SOCIAL_CONTACT_AUTO_ADD, false);
        }

        resp.body(CompatibleContact.contactsToJson(cuMap, cfMap, true));
    }
}
