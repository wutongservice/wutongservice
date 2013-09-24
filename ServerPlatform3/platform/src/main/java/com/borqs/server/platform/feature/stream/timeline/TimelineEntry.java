package com.borqs.server.platform.feature.stream.timeline;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.util.Copyable;
import org.apache.commons.lang.BooleanUtils;

import java.io.*;

public class TimelineEntry implements Copyable<TimelineEntry> {

    public static final short FLAG_PRIVATE_POST = 1;

    public long postId;
    public int appId;
    public int postType;
    private short flag;

    public TimelineEntry() {
    }

    public TimelineEntry(long postId, int appId, int postType, short flag) {
        this.postId = postId;
        this.appId = appId;
        this.postType = postType;
        this.flag = flag;
    }

    public short getFlag() {
        return flag;
    }

    public void setPrivatePost(boolean b) {
        if (b)
            flag |= FLAG_PRIVATE_POST;
        else
            flag &= ~FLAG_PRIVATE_POST;
    }

    public boolean isPrivatePost() {
        return (flag & FLAG_PRIVATE_POST) != 0;
    }


    public byte[] toBytes() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(20);
            DataOutput out = new DataOutputStream(bytes);
            out.writeLong(postId);
            out.writeInt(appId);
            out.writeShort(postType);
            out.writeShort(flag);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new ServerException(E.IO, e);
        }
    }

    public static TimelineEntry fromBytes(byte[] bytes) {
        try {
            DataInput in = new DataInputStream(new ByteArrayInputStream(bytes));
            TimelineEntry entry = new TimelineEntry();
            entry.postId = in.readLong();
            entry.appId = in.readInt();
            entry.postType = in.readShort();
            entry.flag = in.readShort();
            return entry;
        } catch (IOException e) {
            throw new ServerException(E.IO, e);
        }
    }

    @Override
    public TimelineEntry copy() {
        return new TimelineEntry(postId, appId, postType, flag);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(40);
        buff.append(postId).append(":").append(appId).append(":").append(postType).append(":").append(flag);
        return buff.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TimelineEntry other = (TimelineEntry) o;
        return postId == other.postId
                && appId == other.appId
                && postType == other.postType
                && flag == other.flag;
    }

    @Override
    public int hashCode() {
        int result = (int) (postId ^ (postId >>> 32));
        result = 31 * result + appId;
        result = 31 * result + postType;
        result = 31 * result + (int) flag;
        return result;
    }

    public static TimelineEntry create(Post post) {
        TimelineEntry entry = new TimelineEntry();
        entry.postId = post.getPostId();
        entry.postType = post.getType();
        entry.appId = post.getApp();
        entry.setPrivatePost(BooleanUtils.isTrue(post.getPrivate()));
        return entry;
    }
}
