package com.borqs.information.rest.bean;

import java.io.Serializable;

public class Information implements Serializable {
	public static final Integer DEFAULT_PROCESSMETHOD = 1;
	public static final Integer DEFAULT_IMPORTANCE = 30;

	public static final String INFO_ID = "ID";
	public static final String INFO_APP_ID = "appId";
	public static final String INFO_SENDER_ID = "senderId";
	public static final String INFO_RECEIVER_ID = "receiverId";
	public static final String INFO_TYPE = "type";
	public static final String INFO_ACTION = "action";
	public static final String INFO_TITLE = "title";
	public static final String INFO_DATA = "data";
	public static final String INFO_URI = "uri";
	public static final String INFO_PROCESSED = "processed";
	public static final String INFO_READED = "read";
	public static final String INFO_DATE = "date";
	public static final String INFO_PROCESS_METHOD = "process_method";
	public static final String INFO_IMPORTANCE = "importance";
	
	public static final String INFO_GUID = "guid";
	public static final String INFO_BODY = "body";
	public static final String INFO_TITLE_HTML = "title_html";
	public static final String INFO_BODY_HTML = "body_html";
	
	public static final String INFO_OBJECT_ID = "object_id";
	
	public static final String INFO_LAST_MODIFIED = "last_modified";
	public static final String INFO_SCENE = "scene";
	public static final String INFO_IMAGE_URL = "imageUrl";

	private long id;
	
	private String appId;
	private String senderId;
	private String receiverId;
	private String type;
	
	private String uri;
	private String title;
	private String data;

	private int processMethod = 1;
	private boolean processed = false;
	private boolean read = false;

	private int importance = 30;

	private String body;
	private String bodyHtml;
	private String titleHtml;
	// Object ID
	private String objectId;
	
	private long date;
	// last modified time
	private long lastModified;

	// the following have been deprecated.
	private String action;
	// attain only information if GUID isn't null
	private String guid;

	// control whether push a message to Push server
	private boolean push;

    //scene
    private String scene;

    //image url
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Information() {
		super();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String atcion) {
		this.action = atcion;
	}

	public String getSenderId() {
		return senderId;
	}

	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}

	public String getReceiverId() {
		return receiverId;
	}

	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public int getProcessMethod() {
		return processMethod;
	}

	public void setProcessMethod(int processMathod) {
		this.processMethod = processMathod;
	}

	public int getImportance() {
		return importance;
	}

	public void setImportance(int importance) {
		this.importance = importance;
	}

	public String getGuid() {
		return guid;
	}

	public void setGuid(String mergeId) {
		this.guid = mergeId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public void setBodyHtml(String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}

	public String getTitleHtml() {
		return titleHtml;
	}

	public void setTitleHtml(String titleHtml) {
		this.titleHtml = titleHtml;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public boolean isPush() {
		return push;
	}

	public void setPush(boolean push) {
		this.push = push;
	}

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    @Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Information id = ").append(id).append("\n")
			.append("from = ").append(senderId).append("\n")
			.append("to = ").append(receiverId).append("\n")
			.append("appid = ").append(appId).append("\n")
			.append("type = ").append(type).append("\n")
			.append("action = ").append(action).append("\n")
			.append("uri = ").append(uri).append("\n")
			.append("date = ").append(date).append("\n")
			.append("processed = ").append(processed).append("\n")
			.append("read = ").append(read).append("\n")
			.append("process method = ").append(processMethod).append("\n")
			.append("importance = ").append(importance).append("\n")
			.append("guid = ").append(guid).append("\n")
			.append("title = ").append(title).append("\n")
			.append("titleHtml = ").append(titleHtml).append("\n")
			.append("data = ").append(data).append("\n")
			.append("body = ").append(body).append("\n")
			.append("bodyHtml = ").append(bodyHtml).append("\n")
			.append("objectId = ").append(objectId).append("\n")
			.append("last modified = ").append(lastModified).append("\n")
			.append("scen = ").append(scene).append("\n")
			.append("imageUrl = ").append(imageUrl).append("\n");
		return buffer.toString();
	}
}
