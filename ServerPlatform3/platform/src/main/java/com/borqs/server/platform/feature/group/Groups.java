package com.borqs.server.platform.feature.group;


import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.util.CollectionsHelper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Groups extends ArrayList<Group> {

    public Groups() {
    }

    public Groups(Collection<? extends Group> c) {
        super(c);
    }

    public Groups(int initialCapacity) {
        super(initialCapacity);
    }

    public Groups(Group... groups) {
        this(Arrays.asList(groups));
    }

    public long[] getGroupIds() {
        ArrayList<Long> l = new ArrayList<Long>(size());
        for (Group group : this) {
            l.add(group.getGroupId());
        }
        return CollectionsHelper.toLongArray(l);
    }

    public RecordSet toInfo(RecordSet reuse) {
        if (reuse == null)
            reuse = new RecordSet();

        for (Group group : this) {
            reuse.add(group.toInfo());
        }
        return reuse;
    }

    public void addGroupFromInfo(RecordSet info) {
        for (Record rec : info)
            add(Group.infoToGroup(rec));
    }
}
