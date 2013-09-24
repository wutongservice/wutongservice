package com.borqs.server.wutong.notif;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.net.URI;

public class Notification {
    private final String address;

    public Notification(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    /**
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

    synchronized public String send(Record message, boolean isReplace) {
        URI uri = URI.create("avro://" + StringUtils.removeStart(address, "avro://"));
        Transceiver trans = null;
        try {
            trans = Ipc.createTransceiver(uri);
            IInformationsService service = SpecificRequestor.getClient(IInformationsService.class, trans);
            String json = message.toString();
            JsonNode r;
            if(isReplace)
            {
            	r = JsonUtils.parse(ObjectUtils.toString(service.replace(json), "{}"));
            }
            else
            {
            	r = JsonUtils.parse(ObjectUtils.toString(service.send(json), "{}"));
            }
            return StringUtils.equals(r.path("status").getValueAsText(), "success")
                    ? ObjectUtils.toString(r.path("mid").getValueAsText(), "") : "";
        } catch (Exception e) {
            throw new NotificationException(e);
        } finally {
            if (trans != null) {
                try {
                    trans.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
    
    public JsonNode query(String appId, String type, String receiverId, String objectId)
    {
    	URI uri = URI.create("avro://" + StringUtils.removeStart(address, "avro://"));
        Transceiver trans = null;
        try {
            trans = Ipc.createTransceiver(uri);
            IInformationsService service = SpecificRequestor.getClient(IInformationsService.class, trans);
            JsonNode r;
            r = JsonUtils.parse(ObjectUtils.toString(service.query(appId, type, receiverId, objectId)));
            
            return r.get("informations");            
        } catch (Exception e) {
            throw new NotificationException(e);
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
