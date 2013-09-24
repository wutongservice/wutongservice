package com.borqs.server.wutong.verif;


import com.borqs.server.base.context.Context;

public interface PhoneVerificationLogic {
    int OK = 0;
    int REQUEST_TOO_FREQUENT = 1;
    int VERIFICATION_ERROR = 2;
    int VERIFY_TOO_FREQUENT = 3;
    int MAX_VERIFICATION_COUNT = 5;
    long VERIFICATION_TIMEOUT = (1 + 3 + 30) * 60L * 1000;

    int getNextRequestSpan(Context ctx, String phone);

    int request(Context ctx, String phone);

    int feedback(Context ctx, String phone, String code);

    void sendSms(Context ctx, String phone, String text);

    void sendCodeSms(Context ctx, String phone, String code);
}
