package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeopleIds extends ArrayList<PeopleId> {

    public PeopleIds() {
    }

    public PeopleIds(int initialCapacity) {
        super(initialCapacity);
    }

    public PeopleIds(Collection<? extends PeopleId> c) {
        super(c);
    }

    public PeopleIds(PeopleId... peopleIds) {
        Collections.addAll(this, peopleIds);
    }

    public static PeopleIds of(PeopleId... peopleIds) {
        return new PeopleIds(peopleIds);
    }

    public static PeopleIds forStringIds(int type, String... ids) {
        PeopleIds r = new PeopleIds(ids.length);
        for (String id : ids)
            r.add(new PeopleId(type, id));
        return r;
    }

    public static PeopleIds forLongIds(int type, long... ids) {
        PeopleIds r = new PeopleIds(ids.length);
        for (long id : ids)
            r.add(new PeopleId(type, Long.toString(id)));
        return r;
    }

    public static PeopleIds forUserIds(long... userIds) {
        return forLongIds(PeopleId.USER, userIds);
    }

    public static PeopleIds forContactIds(String... contactIds) {
        return forStringIds(PeopleId.CONTACT, contactIds);
    }

    public static Map<Integer, PeopleIds> group(Collection<PeopleId> peopleIds) {
        HashMap<Integer, PeopleIds> m = new HashMap<Integer, PeopleIds>();
        if (CollectionUtils.isNotEmpty(peopleIds)) {
            for (PeopleId peopleId : peopleIds) {
                if (peopleId == null)
                    continue;

                PeopleIds l = m.get(peopleId.type);
                if (l == null) {
                    l = new PeopleIds();
                    m.put(peopleId.type, l);
                }
                l.add(peopleId);
            }
        }
        return m;
    }

    public static Map<Integer, PeopleIds> group(PeopleId... peopleIds) {
        return group(Arrays.asList(peopleIds));
    }

    public Map<Integer, PeopleIds> group() {
        return group(this);
    }

    public String[] getIds(int type) {
        ArrayList<String> l = new ArrayList<String>();
        for (PeopleId peopleId : this) {
            if (peopleId != null && peopleId.type == type)
                l.add(peopleId.id);
        }
        return l.toArray(new String[l.size()]);
    }

    public long[] getUserIds() {
        return getIdsAsLongArray(PeopleId.USER);
    }

    public String[] getContactIds() {
        return getIds(PeopleId.CONTACT);
    }

    public long[] getIdsAsLongArray(int type) {
        String[] ids = getIds(type);
        long[] a = new long[ids.length];
        for (int i = 0; i < a.length; i++)
            a[i] = Long.parseLong(ids[i]);
        return a;
    }

    public boolean hasUserId(long userId) {
        for (PeopleId peopleId : this) {
            if (peopleId != null && peopleId.isUser() && peopleId.getIdAsLong() == userId)
                return true;
        }
        return false;
    }

    public PeopleId[] toIdArray() {
        return toArray(new PeopleId[size()]);
    }

    public static boolean isEquals(Collection<PeopleId> peopleIds1, Collection<PeopleId> peopleIds2) {
        if (peopleIds1 == peopleIds2)
            return true;

        if (peopleIds1.size() != peopleIds2.size())
            return false;

        for (PeopleId fid1 : peopleIds1) {
            if (!peopleIds2.contains(fid1))
                return false;
        }

        for (PeopleId fid2 : peopleIds2) {
            if (!peopleIds1.contains(fid2))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0)
                buff.append(",");
            buff.append(get(i).toStringId());
        }
        return buff.toString();
    }

    public static PeopleIds parse(PeopleIds reuse, String s) {
        if (reuse == null)
            reuse = new PeopleIds();
        if (StringUtils.isNotEmpty(s)) {
            for (String ss : StringHelper.splitList(s, ",", true)) {
                try {
                    PeopleId pid = PeopleId.parseStringId(ss);
                    reuse.add(pid);
                } catch (Exception ignored){
                }
            }
        }
        return reuse;
    }

    public PeopleIds unique() {
        if (isEmpty())
            return this;

        LinkedList<PeopleId> set = new LinkedList<PeopleId>();
        for (PeopleId peopleId : this)
            set.add(peopleId);
        clear();
        addAll(set);
        return this;
    }

    public Set<PeopleId> toSet(Set<PeopleId> reuse) {
        if (reuse == null)
            reuse = new LinkedHashSet<PeopleId>();

        reuse.addAll(this);
        return reuse;
    }


    private static Pattern ADD_TO_IN_MESSAGE_PATTERN = Pattern.compile("(<A [^>]+>(.+?)<\\/A>)", Pattern.CASE_INSENSITIVE);
    public static String[] parseAddToIds(String message) {
        ArrayList<String> l = new ArrayList<String>();
        if (message.trim().length() > 0) {

            Matcher matcher = ADD_TO_IN_MESSAGE_PATTERN.matcher(message);
            while (matcher.find()) {
                String a = matcher.group();
                String[] s = StringUtils.split(a, "uid=");
                if (s.length > 0) {
                    String uu = s[1];
                    String uu2 = "";
                    char[] aa = uu.toCharArray();
                    for (int i = 0; i < aa.length - 1; i++) {
                        if (StringUtils.isNumeric(String.valueOf(aa[i]))) {
                            uu2 += String.valueOf(aa[i]);
                        } else {
                            break;
                        }
                    }
                    if(StringUtils.isNotBlank(uu2))
                        l.add(uu2);
                }
            }
            matcher.reset();
        }
        return l.toArray(new String[l.size()]);
    }

    public static PeopleIds parseAddTo(String message) {
        return forStringIds(PeopleId.USER, parseAddToIds(message));
    }
}
