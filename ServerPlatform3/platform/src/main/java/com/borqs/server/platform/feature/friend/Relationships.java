package com.borqs.server.platform.feature.friend;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

public class Relationships extends ArrayList<Relationship> {
    public Relationships() {
    }

    public Relationships(int initialCapacity) {
        super(initialCapacity);
    }

    public Relationships(Collection<? extends Relationship> c) {
        super(c);
    }

    public Relationship getRelation(PeopleId viewer, PeopleId target) {
        for (Relationship rel : this) {
            if (rel != null
                    && ObjectUtils.equals(viewer, rel.getViewerId())
                    && ObjectUtils.equals(target, rel.getTargetId()))
                return rel;
        }
        return null;
    }

    @Override
    public String toString() {
        return "[" + StringUtils.join(this, ", ") + "]";
    }
}
