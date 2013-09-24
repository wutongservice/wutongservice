package com.borqs.server.platform.util.sender.notif;


import com.borqs.information.rpc.service.IInformationsService;
import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.util.sender.AbstractAsyncSender;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.net.URI;

public class AsyncNotifSender extends AbstractAsyncSender<Notification> {
    private String server;

    public AsyncNotifSender() {
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @Override
    public void asyncSend(final Notification n) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                syncSend(n);
            }
        });
    }

    public String syncSend(Notification n) {
        /*
         * Send a notification message
         * Name	        Type	Range	Remark
         * type	        String	64c	    Notification type
         * appId	    String	64c	    To app id
         * senderId	    String	64c     Sender BID
         * receiverId	String	64c	    Receiver BID
         * uri	        String	1024	Feedback url
         * action	    String	64c	    Action
         * data	        String	2048c	Content max length 2048
         * @param message Notification message
         * @return if success notification id else empty string
         */
        URI uri = URI.create("avro://" + StringUtils.removeStart(server, "avro://"));
        Transceiver trans = null;
        try {
            trans = Ipc.createTransceiver(uri);
            IInformationsService service = SpecificRequestor.getClient(IInformationsService.class, trans);

            String notifJson = n.toJson(false);
            String resultJson;
            if (n.isReplace())
                resultJson = ObjectUtils.toString(service.replace(notifJson), "{}");
            else
                resultJson = ObjectUtils.toString(service.send(notifJson), "{}");

            JsonNode resultNode = JsonHelper.parse(resultJson);

            return StringUtils.equals(resultNode.path("status").getValueAsText(), "success")
                    ? ObjectUtils.toString(resultNode.path("mid").getValueAsText(), "") : "";
        } catch (Exception e) {
            throw new ServerException(E.SEND_NOTIF_ERROR, e, "Send notif error");
        } finally {
            if (trans != null) {
                try {
                    trans.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
