package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.util.ObjectHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;

public class PrivacyEntry {
    public final long userId;
    public final String resource;
    public final PrivacyTarget target;
    public final boolean allow;

    public PrivacyEntry(long userId, String resource, PrivacyTarget target, boolean allow) {
        this.userId = userId;
        this.resource = resource;
        this.target = target;
        this.allow = allow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PrivacyEntry other = (PrivacyEntry) o;

        return  userId == other.userId
                && ObjectUtils.equals(resource, other.resource)
                && ObjectUtils.equals(target, other.target)
                && allow == other.allow;
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(userId, resource, target, allow);
    }

    @Override
    public String toString() {
        return String.format("user:%s-res:%s-target:%s-allow:%s", userId, resource, target, allow);
    }

    public static PrivacyEntry of(long userId, String res, PrivacyTarget target, boolean allow) {
        return new PrivacyEntry(userId, res, target, allow);
    }

    public static PrivacyEntry[] forUserTargets(long userId, String res, long[] targetUserIds, boolean allow) {
        ArrayList<PrivacyEntry> l = new ArrayList<PrivacyEntry>();
        for (long targetUserId : targetUserIds) {
            l.add(of(userId, res, PrivacyTarget.user(targetUserId), allow));
        }
        return l.toArray(new PrivacyEntry[l.size()]);
    }

    public static PrivacyEntry[] forUserResource(long userId, String[] resources, long targetId) {
        ArrayList<PrivacyEntry> l = new ArrayList<PrivacyEntry>();
        for (String res : PrivacyResources.RESOURCES) {
            boolean allow = ArrayUtils.contains(resources, res);
            l.add(of(userId, res, PrivacyTarget.user(targetId), allow));
        }
        return l.toArray(new PrivacyEntry[l.size()]);
    }
}
