package com.borqs.server.platform.feature;


import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.TextEnum;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class Target implements Copyable<Target> {


    public static final int NONE = 0;
    public static Target NONE_ID = new Target(NONE, "");

    public static final int USER = 1;
    public static final int POST = 2;
    public static final int VIDEO = 3;
    public static final int APK = 4;
    public static final int MUSIC = 5;
    public static final int BOOK = 6;
    public static final int COMMENT = 7;
    public static final int LIKE = 8;
    public static final int LINK = 9;
    public static final int PHOTO = 10;
    public static final int CONTACT = 11;
    public static final int ALBUM = 12;
    public static final int REQUEST = 13;
    public static final int APK_LINK = 14;

    public static final TextEnum TYPES_ENUM = TextEnum.of(new Object[][] {
            {"user", USER},
            {"contact", CONTACT},
            {"post", POST},
            {"video", VIDEO},
            {"apk", APK},
            {"music", MUSIC},
            {"book", BOOK},
            {"comment", COMMENT},
            {"like", LIKE},
            {"link", LINK},
            {"photo", PHOTO},
            {"album", ALBUM},
            {"request", REQUEST},
    });


    public final int type;
    public final String id;

    public Target(int type, String id) {
        this.type = type;
        this.id = ObjectUtils.toString(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Target other = (Target) o;
        return type == other.type && StringUtils.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31 * type + ObjectUtils.hashCode(id);
    }

    @Override
    public Target copy() {
        return new Target(type, id);
    }

    @Override
    public String toString() {
        return typeToStr(type) + "." + ObjectUtils.toString(id, "<null>");
    }

    public long getIdAsLong() {
        return Long.parseLong(id);
    }

    public String toCompatibleString() {
        return String.valueOf(type) + ":" + ObjectUtils.toString(id);
    }

    public static Target parseCompatibleString(String s) {
        return new Target(Integer.parseInt(StringUtils.substringBefore(s, ":")), StringUtils.substringAfter(s, ":"));
    }

    public static Target of(int type, long id) {
        return new Target(type, Long.toString(id));
    }

    public static Target of(int type, String id) {
        return new Target(type, id);
    }

    public static Target[] array(int type, long... ids) {
        Target[] targets = new Target[ids.length];
        for (int i = 0; i < ids.length; i++)
            targets[i] = new Target(type, Long.toString(ids[i]));
        return targets;
    }

    public static Target[] array(int type, String... ids) {
        Target[] targets = new Target[ids.length];
        for (int i = 0; i < ids.length; i++)
            targets[i] = new Target(type, ids[i]);
        return targets;
    }

    public static Target[] array(int type, Object[] ids) {
        Target[] targets = new Target[ids.length];
        for (int i = 0; i < ids.length; i++)
            targets[i] = new Target(type, ObjectUtils.toString(ids[i]));
        return targets;
    }

    public static String typeToStr(int type) {
        return TYPES_ENUM.getText(type, "unknown");
    }

    public static String toCompatibleString(Target[] targets, String sep) {
        StringBuilder buff = new StringBuilder();
        if (ArrayUtils.isNotEmpty(targets)) {
            for (Target target : targets) {
                if (target != null)
                    buff.append(target.toCompatibleString()).append(sep);
            }
        }
        return buff.toString();
    }

    public static Target[] parseCompatibleStringToArray(String s, String sep) {
        ArrayList<Target> l = new ArrayList<Target>();
        for (String ss : StringHelper.splitArray(s, sep, true)) {
            l.add(parseCompatibleString(ss));
        }
        return l.toArray(new Target[l.size()]);
    }

    public static Target[] fromCompatibleStringArray(String... ss) {
        if (ArrayUtils.isEmpty(ss))
            return new Target[0];
        ArrayList<Target> l = new ArrayList<Target>(ss.length);
        for (String s : ss)
            l.add(Target.parseCompatibleString(s));
        return l.toArray(new Target[l.size()]);
    }

    public static String[] getIds(Target[] targets, int type) {
        ArrayList<String> ids = new ArrayList<String>(targets.length);
        for (Target t : targets) {
            if (t != null && t.type == type)
                ids.add(t.id);
        }
        return ids.toArray(new String[ids.size()]);
    }

    public static long[] getIdsAsLong(Target[] targets, int type) {
        ArrayList<Long> ids = new ArrayList<Long>(targets.length);
        for (Target t : targets) {
            if (t != null && t.type == type)
                ids.add(t.getIdAsLong());
        }
        return CollectionsHelper.toLongArray(ids);
    }


    // creator

    public static Target forComment(long commentId) {
        return Target.of(COMMENT, commentId);
    }

    public static Target[] forComments(long... commentIds) {
        Target[] targets = new Target[commentIds.length];
        for (int i = 0; i < targets.length; i++)
            targets[i] = forComment(commentIds[i]);
        return targets;
    }

    public static Target forPost(long postId) {
        return Target.of(POST, postId);
    }

    public static Target[] forPosts(long... postIds) {
        Target[] targets = new Target[postIds.length];
        for (int i = 0; i < targets.length; i++)
            targets[i] = forPost(postIds[i]);
        return targets;
    }

    public static Target forContact(String contactId) {
        return Target.of(CONTACT, contactId);
    }

    public static Target[] forContacts(String... contactIds) {
        Target[] targets = new Target[contactIds.length];
        for (int i = 0; i < targets.length; i++)
            targets[i] = forContact(contactIds[i]);
        return targets;
    }

    public static Target forRequest(long reqId) {
        return Target.of(REQUEST, reqId);
    }

    public static Target[] forRequests(long... reqIds) {
        Target[] targets = new Target[reqIds.length];
        for (int i = 0; i < targets.length; i++)
            targets[i] = forRequest(reqIds[i]);
        return targets;
    }

    public static Target forApk(String apkId) {
        return Target.of(APK, apkId);
    }

    public static Target forApkLink(String apkUrl) {
        return Target.of(APK_LINK, ObjectUtils.toString(apkUrl));
    }

    public static boolean equalsIgnoreClass(Target t1, Target t2) {
        if (t1 == null && t2 == null)
            return true;
        if (t1 == null || t2 == null)
            return false;

        return t1.type == t2.type && ObjectUtils.toString(t1.id).equals(ObjectUtils.toString(t2.id));
    }
}
