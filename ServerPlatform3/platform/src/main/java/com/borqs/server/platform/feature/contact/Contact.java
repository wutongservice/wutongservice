package com.borqs.server.platform.feature.contact;

import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.StringUtils;

public class Contact extends Addons{
    protected long owner;
    protected String name;
    protected int type;
    protected String content;
    protected int reason;

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    protected String localId;

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    protected long createdTime;

    public long getOwner() {
        return owner;
    }

    public void setOwner(long owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public Contact() {
    }

    public Contact(long owner, String name, int type, String content, int reason, long createdTime) {
        this.owner = owner;
        this.name = name;
        this.type = type;
        this.content = content;
        this.reason = reason;
        this.createdTime = createdTime;
    }
    
    public String getId() {
        return owner + "-" + type + "-" + content;
    }
    
    public static Contact of(String id, String name, int reason) {
        String[] arr = StringUtils.splitPreserveAllTokens(id, "-", 3);
        return new Contact(Long.parseLong(arr[0]), name, Integer.parseInt(arr[1]), arr[2], reason, DateHelper.nowMillis());
    }
    
    public static Contact of(String id) {
        return Contact.of(id, "", Reasons.CONTACTS_FRIEND);
    }
    
    public static Contact of(long owner, int type, String content) {
        return  new Contact(owner, "", type, content, Reasons.CONTACTS_FRIEND, DateHelper.nowMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Contact other = (Contact) o;
        return type == other.type && content.equals(other.content)
                && reason == other.reason;
    }
}
