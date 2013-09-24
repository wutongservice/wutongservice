package com.borqs.server.pubapi.v1;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.codehaus.jackson.JsonNode;

import java.util.Iterator;
import java.util.Map;

@IgnoreDocument
public class PhoneBook1Api extends PublicApiSupport {
    private AccountLogic account;
    private CibindLogic cibind;
    private ContactLogic contact;

    public static String bucketName = "wutong-data";
    public static String bucketName_video_key = "media/video/";
    public static String bucketName_audio_key = "media/audio/";
    public static String bucketName_static_file_key = "files/";


    public void setAccount(AccountLogic account) {
        this.account = account;
    }


    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    public void setContact(ContactLogic contact) {
        this.contact = contact;
    }

    public PhoneBook1Api() {
    }


    @Route(url = "/phonebook/look_up")
    public void getConfigration(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        Users users = new Users();

        String contactInfo = req.checkString("contact_info");
        JsonNode js = JsonHelper.parse(contactInfo);

        Iterator<JsonNode> iterator = js.iterator();
        for (; iterator.hasNext(); ) {
            JsonNode j = iterator.next();
            if (j.has("email")) {
                long userId = cibind.whoBinding(ctx, j.get("email").getValueAsText());
                if (userId > 0) {
                    User user = account.getUser(ctx, User.STANDARD_COLUMNS, userId);
                    users.add(user);
                    continue;
                }

            }
            if (j.has("phone")) {
                long userId = cibind.whoBinding(ctx, j.get("phone").getValueAsText());
                if (userId > 0) {
                    User user = account.getUser(ctx, User.STANDARD_COLUMNS, userId);
                    users.add(user);
                    continue;
                }
            }
            if (j.has("name")) {
                // TODO getUser by DisplayName;
            }
        }

        resp.body(RawText.of(users.toJson(User.STANDARD_COLUMNS, false)));
    }

    @Route(url = "/phonebook/all")
    public void findMyAllPhoneBook(Request req, Response resp) {
        Context ctx = checkContext(req, false);

        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        Users users = new Users();
        String info = req.checkString("contact_info");
        //info = new String(Encoders.fromBase64(info));
        JsonNode js = JsonHelper.parse(info);


        Contacts contacts = new Contacts();
        Iterator<JsonNode> iterator = js.iterator();
        for (; iterator.hasNext(); ) {
            JsonNode j = iterator.next();
            String username = j.get("username").getValueAsText();
            int type = j.get("type").getIntValue();
            String content = j.get("content").getTextValue();

            long userId = cibind.whoBinding(ctx, content);
            Contact contact1 = new Contact();

            contact1.setContent(content);
            contact1.setOwner(ctx.getViewer());
            contact1.setCreatedTime(DateHelper.nowMillis());
            contact1.setName(username);
            contact1.setType(type);
            contact1.setReason(Reasons.UPLOAD_CONTACTS);
            contacts.add(contact1);
        }
        Contact[] cons = contacts.toContactArray();
        //save the contact what user upload
        contact.mergeUpdate(ctx, cons);

        //get all contact of current user
        Contacts conts = contact.getContacts(ctx, Reasons.UPLOAD_CONTACTS, ctx.getViewer());
        Map<String, Long> map = cibind.whoBinding(ctx, conts.getContents());

        Contacts c_is = new Contacts();
        Contacts c_no = new Contacts();

        // divide the contact
        for (Contact c : conts) {
            if (map.containsKey(c.getContent())) {
                c_is.add(c);
            } else {
                c_no.add(c);
            }
        }

        Record r = new Record();
        r.put("in_borqs", c_is);
        r.put("social_contacts", c_no);

        resp.body(RawText.of(JsonHelper.toJson(r, false)));
    }


}
