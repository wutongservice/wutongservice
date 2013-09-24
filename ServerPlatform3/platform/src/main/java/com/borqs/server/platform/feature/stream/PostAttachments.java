package com.borqs.server.platform.feature.stream;


import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class PostAttachments {
    private final Map<PeopleId, Set<Target>> attachments = new HashMap<PeopleId, Set<Target>>();

    public PostAttachments() {
    }

    public void clearAttachments() {
        attachments.clear();
    }

    public PostAttachments addAttachments(Post post) {
        if (post != null) {
            PeopleId pid = post.getSourcePeople();
            Set<Target> targets = attachments.get(pid);
            if (targets == null) {
                targets = new LinkedHashSet<Target>();
                attachments.put(pid, targets);
            }
            Target[] tt = post.getAttachmentTargetIds();
            if (ArrayUtils.isNotEmpty(tt))
                Collections.addAll(targets, tt);
        }
        return this;
    }

    public PeopleIds getPeopleIds() {
        return new PeopleIds(attachments.keySet());
    }

    public Target[] getTarget(PeopleId peopleId) {
        Set<Target> targets = attachments.get(peopleId);
        return CollectionUtils.isNotEmpty(targets) ? targets.toArray(new Target[targets.size()]) : new Target[0];
    }

    public Target[] getAllTargets() {
        LinkedHashSet<Target> l = new LinkedHashSet<Target>();
        for (Set<Target> tt : attachments.values()) {
            if (tt != null)
                l.addAll(tt);
        }
        return l.toArray(new Target[l.size()]);
    }

    public int getAllTargetsCount() {
        HashSet<Target> l = new HashSet<Target>();
        for (Set<Target> tt : attachments.values()) {
            if (tt != null)
                l.addAll(tt);
        }
        return l.size();
    }
}
