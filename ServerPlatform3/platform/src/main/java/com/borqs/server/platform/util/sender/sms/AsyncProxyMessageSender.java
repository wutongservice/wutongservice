package com.borqs.server.platform.util.sender.sms;


import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.sender.AbstractAsyncSender;
import com.borqs.server.platform.web.AbstractHttpClient;
import com.borqs.server.platform.web.HttpClient;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.Executor;

public class AsyncProxyMessageSender extends AbstractAsyncSender<Message> {
    private static final Logger L = Logger.get(AsyncProxyMessageSender.class);

    private final HttpClient httpClient = new HttpClient();

    public AsyncProxyMessageSender() {
    }

    public AsyncProxyMessageSender(Executor executor) {
        super(executor);
    }

    public void setHost(String host) {
        httpClient.setHost(host);
    }

    public String getHost(String host) {
        return httpClient.getHost();
    }

    @Override
    public void asyncSend(final Message msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                syncSend(msg);
            }
        });
    }

    public boolean syncSend(Message msg) {
        String to = ObjectUtils.toString(msg.getTo());
        String content = ObjectUtils.toString(msg.getContent());
        String data = String.format("{\"to\":\"%s\",\"subject\":\"%s\"}", to, StringEscapeUtils.escapeJavaScript(content));
        try {
            AbstractHttpClient.Response resp = httpClient.get("", new Object[][] {
                    {"appname", "qiupu"},
                    {"data", data},
            });
            String text = resp.getText();
            L.debug(null, "Send SMS to %s: %s, resp: %s", to, content, text);
            return StringUtils.trimToEmpty(text).equalsIgnoreCase("ok");
        } catch (Exception e) {
            L.error(null, e, "Send SMS to %s error: %s", to, content);
            return false;
        }
    }
}
