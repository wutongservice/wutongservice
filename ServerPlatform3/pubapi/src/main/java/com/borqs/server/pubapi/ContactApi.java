package com.borqs.server.pubapi;

import com.borqs.server.compatible.CompatibleContact;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.ContactLogic;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class ContactApi extends PublicApiSupport {
    private AccountLogic account;
    private ContactLogic contactLogic;

    public ContactApi() {
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



    /**
     * 上传联系人列表
     *
     * @group Contact
     * @http-param contacts contacts是每个元素包含三个字段组成的JSON数组:username,type,content,username对应电话簿的名称，type的值1为电话，2为email，content为具体的内容
     * @http-param full:false 是否为全量上传，true-全量上传 false-增量上传
     * @http-return true
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/contact/upload")
    public void uploadContacts(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String json = Encoders.fromBase64String(req.checkString("contacts"));
        Contacts contacts = CompatibleContact.jsonToContacts(json, null, ctx.getViewer(), DateHelper.nowMillis());

        if (req.getBoolean("full", false))
            contactLogic.fullUpdate(ctx, contacts.toContactArray());
        else
            contactLogic.mergeUpdate(ctx, contacts.toContactArray());

        resp.body(true);
    }
}
