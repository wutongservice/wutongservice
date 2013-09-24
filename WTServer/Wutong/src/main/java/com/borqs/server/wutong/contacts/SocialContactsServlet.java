package com.borqs.server.wutong.contacts;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.avro.AvroRemoteException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

public class SocialContactsServlet extends WebMethodServlet {


    @WebMethod("socialcontact/upload")
    public RecordSet socialContactUpload(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        SocialContactsLogic socialContactsLogic = GlobalLogics.getSocialContacts();
        try {
            String s = qp.getString("contactinfo", "");
            if (s.length() <= 0)
                return new RecordSet();
            String ua = ctx.getUa();
            String loc = ctx.getLocation();
            return socialContactsLogic.createSocialContacts(ctx, viewerId, qp.checkGetString("contactinfo"), ua, loc);
        } catch (Exception e) {
            return new RecordSet();
        }
    }

    @WebMethod("phonebook/look_up")
    public RecordSet findBorqsId(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
        String contact_info = qp.getString("contact_info", "");
        Context ctx = WutongContext.getContext(qp, false);
        RecordSet recs = RecordSet.fromJson(contact_info);
        if (recs.size() == 0)
            throw new ServerException(WutongErrors.SYSTEM_PARAMETER_TYPE_ERROR, "Contact info error!");
        RecordSet outRecs = GlobalLogics.getSocialContacts().findBorqsIdFromContactInfo(ctx, recs);
        return outRecs;
    }

        @WebMethod("phonebook/all")
        public Record findMyAllPhoneBook(QueryParams qp, HttpServletResponse resp) throws AvroRemoteException {
            String contact_info = qp.getString("contact_info", "");
            Context ctx = WutongContext.getContext(qp, true);
           String viewerId = ctx.getViewerIdString();
            Record outRec = GlobalLogics.getSocialContacts().findMyAllPhoneBookP(ctx,viewerId, contact_info);
            return outRec;
        }
}
