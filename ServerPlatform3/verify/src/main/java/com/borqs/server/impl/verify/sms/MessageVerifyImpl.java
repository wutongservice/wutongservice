package com.borqs.server.impl.verify.sms;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.verify.MessageVerifyLogic;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.sender.sms.AsyncProxyMessageSender;
import com.borqs.server.platform.util.sender.sms.Message;


public class MessageVerifyImpl implements MessageVerifyLogic {

    public static final int[] SPANS = new int[]{60, 3 * 60, 30 * 60};

    private final MessageVerifyDb db = new MessageVerifyDb();
    private AsyncProxyMessageSender messageSender;


    public MessageVerifyImpl() {
    }

    private void sendMessage(String to, String content) {
        messageSender.asyncSend(Message.forSend(to, content));
    }

    public static String genRandomCode() {
        return RandomHelper.generateRandomNumberString(4);
    }

    @Override
    public int nextSendSpan(Context ctx, String phone) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendCode(Context ctx, String phone) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int verifyCode(Context ctx, String phone, String code) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private static int spanSeconds(int count) {
        int s = 0;
        for (int i = 0; i < count; i++) {
            if (i < count && i < SPANS.length)
                s += SPANS[i];
        }
        return s;
    }

    private static long calculateExpiry(long createdTime, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException();
        } else {
            int secs = spanSeconds(count <= SPANS.length ? count - 1 : SPANS.length);
            return secs <= 0 ? 0 : 1000L * secs;
        }
    }

    private static long calculateNextSpan(long createdTime, int count) {
        long r = calculateExpiry(createdTime, count + 1) - DateHelper.nowMillis();
        return r >= 0 ? r : 0;
    }
}
