package com.borqs.server.wutong.verif;


import com.borqs.server.ServerException;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.avro.AvroRemoteException;

public class VerificationServlet extends WebMethodServlet {

    public VerificationServlet() {
    }

    @WebMethod("verify/phone")
    public String verifyPhone(QueryParams qp) throws AvroRemoteException {
        String phone = qp.checkGetString("phone");

        Context ctx = WutongContext.getContext(qp, false);
        PhoneVerificationLogic phoneVerification = GlobalLogics.getPhoneVerification();
        AccountLogic account = GlobalLogics.getAccount();

        if (qp.getBoolean("next_span", false)) {
            return Integer.toString(phoneVerification.getNextRequestSpan(ctx, phone));
        } else {
            if (!qp.containsKey("code")) {
                if (qp.getBoolean("check", false)) {
                    Record rec = account.getUserIds(ctx, phone).getFirstRecord();
                    if (rec != null) {
                        if (rec.getInt("user_id", 0) > 0)
                            throw new ServerException(WutongErrors.USER_LOGIN_NAME_EXISTED, "Phone exists");
                    }
                }

                int pvr = phoneVerification.request(ctx, phone);
                return phoneVerificationResult(pvr);
            } else {
                int pvr = phoneVerification.feedback(ctx, phone, qp.checkGetString("code"));
                return phoneVerificationResult(pvr);
            }
        }
    }

    private static String phoneVerificationResult(int pvr) {
        switch (pvr) {
            case PhoneVerificationLogic.OK:
                return "OK";
            case PhoneVerificationLogic.REQUEST_TOO_FREQUENT:
                throw new ServerException(WutongErrors.AUTH_REQUEST_TOO_FREQUENT, "Too many request");
            case PhoneVerificationLogic.VERIFICATION_ERROR:
                throw new ServerException(WutongErrors.AUTH_VERIFICATION_ERROR, "Code error or timeout");
            case PhoneVerificationLogic.VERIFY_TOO_FREQUENT:
                throw new ServerException(WutongErrors.AUTH_VERIFY_TOO_FREQUENT, "Too many verification");
        }
        throw new IllegalArgumentException();
    }
}
