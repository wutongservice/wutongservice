package com.borqs.server.wutong.messagecenter;

public class MessageCenter {
    private String messageId;
    private String title;
    private String to;
    private String username;
    private String fromUsername;
    private String fromId;
    private String content;
    private String targetType;
    private String targetId;
    private String targetName;
    private String sendKey;
    private String emailCombine;
    private String delayType;
    private String createdTime;
    private String updatedTime;
    private String destroyedTime;

    public static String EMAIL_DELAY_TYPE_MINUTES = "email.delay.minutes";
    public static String EMAIL_DELAY_TYPE_DAYS = "email.delay.days";

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSendKey() {
        return sendKey;
    }

    public void setSendKey(String sendKey) {
        this.sendKey = sendKey;
    }

    public String getDelayType() {
        return delayType;
    }

    public void setDelayType(String delayType) {
        this.delayType = delayType;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(String destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public String getEmailCombine() {
        return emailCombine;
    }

    public void setEmailCombine(String emailCombine) {
        this.emailCombine = emailCombine;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
}
