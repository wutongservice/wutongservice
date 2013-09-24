package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class AllowedIds {
    public static final int MODE_NORMAL = 1;
    public static final int MODE_EXCLUSION = 2;

    private static final AllowedIds ALL = exclusion(new long[0]);
    private static final AllowedIds NONE = normal(new long[0]);


    public final int mode;
    public final long[] ids;

    private AllowedIds(int mode, long[] ids) {
        this.mode = mode;
        this.ids = ids;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AllowedIds other = (AllowedIds) o;
        return mode == other.mode && Arrays.equals(ids, other.ids);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(mode, Arrays.hashCode(ids));
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        if (isAll()) {
            buff.append("A");
        } else if (isNone()) {
            buff.append("N");
        } else {
            buff.append(mode == MODE_NORMAL ? "+ " : "- ");
            buff.append(StringHelper.join(ids, ","));
        }
        return buff.toString();
    }

    public boolean isNormalMode() {
        return mode == MODE_NORMAL;
    }

    public boolean isExclusionMode() {
        return mode == MODE_EXCLUSION;
    }

    public int idCount() {
        return ids.length;
    }

    public boolean isAll() {
        return mode == MODE_EXCLUSION && ids.length == 0;
    }

    public boolean isNone() {
        return mode == MODE_NORMAL && ids.length == 0;
    }

    public static AllowedIds normal(long[] ids) {
        return new AllowedIds(MODE_NORMAL, ids != null ? ids : new long[0]);
    }

    public static AllowedIds normal(Collection c) {
        return normal(CollectionsHelper.toLongArray(c));
    }

    public static AllowedIds exclusion(long[] ids) {
        return new AllowedIds(MODE_EXCLUSION, ids != null ? ids : new long[0]);
    }

    public static AllowedIds exclusion(Collection c) {
        return exclusion(CollectionsHelper.toLongArray(c));
    }

    public static AllowedIds all() {
        return ALL;
    }

    public static AllowedIds none() {
        return NONE;
    }

    public boolean include(long userId) {
        if (mode == MODE_NORMAL) {
            return ArrayUtils.contains(ids, userId);
        } else if (mode == MODE_EXCLUSION) {
            return !ArrayUtils.contains(ids, userId);
        } else {
            throw new IllegalStateException("AllowIds mode error");
        }
    }
}
