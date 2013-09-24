package com.borqs.server.platform.verification;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.platform.ErrorCode;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;

import javax.servlet.ServletException;

public class VerificationServlet extends WebMethodServlet {

    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private PhoneVerification phoneVerification;


    @Override
    public void init() throws ServletException {
        Configuration conf = getConfiguration();

        phoneVerification = new PhoneVerification();
        phoneVerification.setConfig(conf);
        phoneVerification.init();

        transceiverFactory.setConfig(conf);
        transceiverFactory.init();
    }


    @Override
    public void destroy() {
        transceiverFactory.destroy();

        phoneVerification.destroy();
        phoneVerification = null;
    }

    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }

    @WebMethod("verify/phone")
    public String verifyPhone(QueryParams qp) throws AvroRemoteException {
        String phone = qp.checkGetString("phone");

        if (qp.getBoolean("next_span", false)) {
            return Integer.toString(phoneVerification.getNextRequestSpan(phone));
        } else {
            if (!qp.containsKey("code")) {
                if (qp.getBoolean("check", false)) {
                    Platform p = platform();
                    Record rec = p.getUserIds(phone).getFirstRecord();
                    if (rec != null) {
                        if (rec.getInt("user_id", 0) > 0)
                            throw new ServerException(ErrorCode.LOGIN_NAME_EXISTS, "Phone exists");
                    }
                }

                int pvr = phoneVerification.request(phone);
                return phoneVerificationResult(pvr);
            } else {
                int pvr = phoneVerification.feedback(phone, qp.checkGetString("code"));
                return phoneVerificationResult(pvr);
            }
        }
    }

    private static String phoneVerificationResult(int pvr) {
        switch (pvr) {
            case PhoneVerification.OK:
                return "OK";
            case PhoneVerification.REQUEST_TOO_FREQUENT:
                throw new ServerException(ErrorCode.REQUEST_TOO_FREQUENT, "Too many request");
            case PhoneVerification.VERIFICATION_ERROR:
                throw new ServerException(ErrorCode.VERIFICATION_ERROR, "Code error or timeout");
            case PhoneVerification.VERIFY_TOO_FREQUENT:
                throw new ServerException(ErrorCode.VERIFY_TOO_FREQUENT, "Too many verification");
        }
        throw new IllegalArgumentException();
    }
}
