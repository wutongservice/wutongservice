package com.borqs.server.wutong.search;


public class PostDoc {

    private long id;

    private long createdTime;
    private long updatedTime;

    private long fromId;
    private String from;

    private long[] toIds;
    private String[] to;

    private long[] addToIds;
    private String[] addTos;

    private long[] groupIds;
    private String[] groups;

    private long categoryId;
    private String category;

    private String[] tags;

    private boolean private_;

    private String message;

    public PostDoc() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public long getFromId() {
        return fromId;
    }

    public void setFromId(long fromId) {
        this.fromId = fromId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long[] getToIds() {
        return toIds;
    }

    public void setToIds(long[] toIds) {
        this.toIds = toIds;
    }

    public String[] getTo() {
        return to;
    }

    public void setTo(String[] to) {
        this.to = to;
    }

    public long[] getAddToIds() {
        return addToIds;
    }

    public void setAddToIds(long[] addToIds) {
        this.addToIds = addToIds;
    }

    public String[] getAddTos() {
        return addTos;
    }

    public void setAddTos(String[] addTos) {
        this.addTos = addTos;
    }

    public long[] getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(long[] groupIds) {
        this.groupIds = groupIds;
    }

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public boolean isPrivate_() {
        return private_;
    }

    public void setPrivate_(boolean private_) {
        this.private_ = private_;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
