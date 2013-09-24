package com.borqs.server.wutong.search;


import com.borqs.server.base.util.PinyinUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DocSupport {
    public static final String FIELD_ID = "id";
    public static final String FIELD_CREATED_TIME = "created_time";
    public static final String FIELD_UPDATED_TIME = "updated_time";
    public static final String FIELD_OBJECT_TYPE = "object_type";
    public static final String FIELD_FROM_ID = "from_id";
    public static final String FIELD_FROM = "from";
    public static final String FIELD_TO_ID = "to_id";
    public static final String FIELD_TO = "to";
    public static final String FIELD_ADDTO_ID = "addto_id";
    public static final String FIELD_ADDTO = "addto";
    public static final String FIELD_GROUP_ID = "group_id";
    public static final String FIELD_GROUP = "group";
    public static final String FIELD_CATEGORY_ID = "category_id";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_PRIVATE = "private";
    public static final String FIELD_TEXT = "text";


    public static String withPinyin(String s) {
        if (s == null)
            return null;
        if (s.isEmpty())
            return "";

        return s + " " + PinyinUtils.toFullPinyin(s) + " " + PinyinUtils.toShortPinyin(s);
    }

    public static String join(String[] a) {
        return StringUtils.join(a, " ");
    }

    public static String joinWithPinyin(String[] a) {
        String[] a1 = new String[a.length];
        for (int i = 0; i < a1.length; i++) {
            a1[i] = withPinyin(a[i]);
        }
        return StringUtils.join(a1, " ");
    }


    public static String makePostId(long postId) {
        return postId + "@post";
    }

    public static List<String> makePostIds(long[] postIds) {
        ArrayList<String> l = new ArrayList<String>();
        if (ArrayUtils.isNotEmpty(postIds)) {
            for (long postId : postIds)
                l.add(makePostId(postId));
        }
        return l;
    }
}
