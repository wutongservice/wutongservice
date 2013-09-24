package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class PrivacyTarget {
    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_FRIEND = 2;
    public static final int SCOPE_CIRCLE = 3;
    public static final int SCOPE_USER = 4;

    public final int scope;
    public final String id;

    public PrivacyTarget(int scope, String id) {
        this.scope = scope;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PrivacyTarget other = (PrivacyTarget) o;
        return scope == other.scope && StringUtils.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(scope, id);
    }

    @Override
    public String toString() {
        switch (scope) {
            case SCOPE_ALL:
                return "all:" + id;
            case SCOPE_FRIEND:
                return "friend:" + id;
            case SCOPE_CIRCLE:
                return "circle:" + id;
            case SCOPE_USER:
                return "user:" + id;
        }
        return "unknown scope(" + scope + "):" + id;
    }

    public static PrivacyTarget all() {
        return new PrivacyTarget(SCOPE_ALL, "");
    }

    public static PrivacyTarget friend() {
        return new PrivacyTarget(SCOPE_FRIEND, "");
    }

    public static PrivacyTarget circle(int circleId) {
        return new PrivacyTarget(SCOPE_CIRCLE, Integer.toString(circleId));
    }

    public static PrivacyTarget user(long userId) {
        return new PrivacyTarget(SCOPE_USER, Long.toString(userId));
    }

    public static PrivacyTarget parse(String s) {
        if ("all".equalsIgnoreCase(s)) {
            return all();
        } else if ("friend".equalsIgnoreCase(s)) {
            return friend();
        } else if (StringUtils.startsWithIgnoreCase(s, "circle:")) {
            return circle(Integer.parseInt(StringUtils.removeStartIgnoreCase(s, "circle:")));
        } else {
            return user(Long.parseLong(StringUtils.removeStartIgnoreCase(s, "user:")));
        }
    }

    public static PrivacyTarget[] parseArray(String s, String sep) {
        ArrayList<PrivacyTarget> l = new ArrayList<PrivacyTarget>();
        for (String e : StringHelper.splitList(s, sep, true))
            l.add(parse(e));
        return l.toArray(new PrivacyTarget[l.size()]);
    }

    public boolean isUser(long userId) {
        return scope == SCOPE_USER && Long.toString(userId).equals(id);
    }
}
