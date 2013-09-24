package com.borqs.server.platform.util.sender.notif;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public class Notification {
    private String type = "";
    private int appId = App.APP_NONE;
    private String senderId = "";
    private String receiverId = "";
    private String uri = "";
    private String action = "";
    private String data = "";

    private String title = "";
    private String titleHtml = "";
    private String body = "";
    private String bodyHtml = "";
    private String targetId = "";

    private boolean replace = false;

    private Notification() {

    }

    public static Notification forReplace(String type, int appId, String action) {
        Notification n = new Notification();
        n.setReplace(true);
        n.setType(type);
        n.setAppId(appId);
        n.setAction(action);
        return n;
    }


    public static Notification forSend(String type, int appId, String action) {
        Notification n = new Notification();
        n.setReplace(false);
        n.setType(type);
        n.setAppId(appId);
        n.setAction(action);
        return n;
    }

    public Notification senderAndReceiver(String senderId, String receiverId) {
        setSenderId(senderId);
        setReceiverId(receiverId);
        return this;
    }

    public Notification title(String title, String titleHtml) {
        setTitle(title);
        setTitleHtml(titleHtml);
        return this;
    }

    public Notification body(String body, String bodyHtml) {
        setBody(body);
        setBodyHtml(bodyHtml);
        return this;
    }

    public Notification withTarget(String targetId) {
        setTargetId(targetId);
        return this;
    }

    public Notification withTargetId(Target target) {
        setTargetId(target.toCompatibleString());
        return this;
    }

    public String getType() {
        return type;
    }

    public Notification setType(String type) {
        this.type = type;
        return this;
    }

    public int getAppId() {
        return appId;
    }

    public Notification setAppId(int appId) {
        this.appId = appId;
        return this;
    }

    public String getSenderId() {
        return senderId;
    }

    public Notification setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public Notification setReceiverId(String receiverId) {
        this.receiverId = receiverId;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public Notification setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getAction() {
        return action;
    }

    public Notification setAction(String action) {
        this.action = action;
        return this;
    }

    public String getData() {
        return data;
    }

    public Notification setData(String data) {
        this.data = data;
        return this;
    }

    public boolean isReplace() {
        return replace;
    }

    public Notification setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Notification setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitleHtml() {
        return titleHtml;
    }

    public Notification setTitleHtml(String titleHtml) {
        this.titleHtml = titleHtml;
        return this;
    }

    public String getBody() {
        return body;
    }

    public Notification setBody(String body) {
        this.body = body;
        return this;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public Notification setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
        return this;
    }

    public String getTargetId() {
        return targetId;
    }

    public Notification setTargetId(String targetId) {
        this.targetId = targetId;
        return this;
    }

    public String toJson(boolean human) {
        /*
         * Name	        Type	Range	Remark
         * -----------------------------------------------------
         * type	        String	64c	    Notification type
         * appId	    String	64c	    To app id
         * senderId	    String	64c     Sender BID
         * receiverId	String	64c	    Receiver BID
         * uri	        String	1024	Feedback url
         * action	    String	64c	    Action
         * data	        String	2048c	Content max length 2048
         * title
         * titleHtml
         * body
         * bodyHtml
         * objectId
         */
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jg.writeStartObject();
                jg.writeStringField("type", ObjectUtils.toString(type));
                jg.writeStringField("appId", Integer.toString(appId));
                jg.writeStringField("senderId", ObjectUtils.toString(senderId));
                jg.writeStringField("receiverId", ObjectUtils.toString(receiverId));
                jg.writeStringField("uri", ObjectUtils.toString(uri));
                jg.writeStringField("action", ObjectUtils.toString(action));
                jg.writeStringField("data", ObjectUtils.toString(data));
                jg.writeStringField("title", ObjectUtils.toString(title));
                jg.writeStringField("titleHtml", ObjectUtils.toString(titleHtml));
                jg.writeStringField("body", ObjectUtils.toString(body));
                jg.writeStringField("bodyHtml", ObjectUtils.toString(bodyHtml));
                jg.writeStringField("objectId", ObjectUtils.toString(targetId));
                jg.writeEndObject();
            }
        }, human) ;
    }

    @Override
    public String toString() {
        return toJson(true);
    }
}
