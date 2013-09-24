package com.borqs.server.platform.feature.conversation;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class Conversations extends ArrayList<Conversation> {

    public long[] getUsers() {
        Set<Long> set = new HashSet<Long>();
        for (Conversation conversation : this) {
            long user = conversation.getUser();
            if (!set.contains(user))
                set.add(user);
        }
        return CollectionsHelper.toLongArray(set);
    }

    public Map<Target, long[]> getGroupedUsers() {
        LinkedHashMap<Target, long[]> m = new LinkedHashMap<Target, long[]>();
        for (Conversation conversation : this) {
            Target target = conversation.getTarget();
            long user = conversation.getUser();
            long[] users = new long[]{};
            if (m.containsKey(target))
                users = m.get(target);
            m.put(target, ArrayUtils.add(users, user));
        }
        return m;
    }

    public Target[] getTargets() {
        ArrayList<Target> l = new ArrayList<Target>();
        for (Conversation conversation : this) {
            Target target = conversation.getTarget();
            if (!l.contains(target))
                l.add(target);
        }
        return l.toArray(new Target[l.size()]);
    }

    public Map<Long, Target[]> getGroupedTargets() {
        LinkedHashMap<Long, Target[]> m = new LinkedHashMap<Long, Target[]>();
        for (Conversation conversation : this) {
            Target target = conversation.getTarget();
            long user = conversation.getUser();
            Target[] targets = new Target[]{};
            if (m.containsKey(user))
                targets = m.get(user);
            m.put(user, (Target[]) ArrayUtils.add(targets, target));
        }
        return m;
    }

    public String[] getTargetIds() {
        ArrayList<String> l = new ArrayList<String>(size());
        for (Conversation conv : this) {
            Target t = conv.getTarget();
            if (t != null)
                l.add(t.id);
        }
        return l.toArray(new String[l.size()]);
    }

    public long[] getTargetIdsAsLong() {
        ArrayList<Long> l = new ArrayList<Long>(size());
        for (Conversation conv : this) {
            Target t = conv.getTarget();
            if (t != null)
                l.add(t.getIdAsLong());
        }
        return CollectionsHelper.toLongArray(l);
    }

    public String[] getTargetIds(int targetType) {
        ArrayList<String> l = new ArrayList<String>(size());
        for (Conversation conv : this) {
            Target t = conv.getTarget();
            if (t != null && t.type == targetType)
                l.add(t.id);
        }
        return l.toArray(new String[l.size()]);
    }

    public long[] getTargetIdsAsLong(int targetType) {
        ArrayList<Long> l = new ArrayList<Long>(size());
        for (Conversation conv : this) {
            Target t = conv.getTarget();
            if (t != null && t.type == targetType)
                l.add(t.getIdAsLong());
        }
        return CollectionsHelper.toLongArray(l);
    }
}
