package com.borqs.server.platform.feature.contact;

import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class Contacts extends ArrayList<Contact> {

    public Contacts() {
    }

    public Contacts(int initialCapacity) {
        super(initialCapacity);
    }

    public Contacts(Collection<? extends Contact> c) {
        super(c);
    }

    public Contacts(Contact... contacts) {
        Collections.addAll(this, contacts);
    }

    public String[] getContents() {
        Set<String> set = new HashSet<String>();
        for (Contact contact : this) {
            String content = contact.getContent();
            if (!set.contains(content))
                set.add(content);
        }
        return set.toArray(new String[set.size()]);
    }

    public Map<Long, String[]> getGroupedContents() {
        LinkedHashMap<Long, String[]> m = new LinkedHashMap<Long, String[]>();
        for (Contact contact : this) {
            Long owner = contact.getOwner();
            String content = contact.getContent();
            String[] contents = new String[]{};
            if (m.containsKey(owner))
                contents = m.get(owner);
            m.put(owner, (String[]) ArrayUtils.add(contents, content));
        }
        return m;
    }

    public long[] getOwners() {
        ArrayList<Long> l = new ArrayList<Long>();
        for (Contact contact : this) {
            Long owner = contact.getOwner();
            if (!l.contains(owner))
                l.add(owner);
        }
        return CollectionsHelper.toLongArray(l);
    }

    public Map<String, long[]> getGroupedOwners() {
        LinkedHashMap<String, long[]> m = new LinkedHashMap<String, long[]>();
        for (Contact contact : this) {
            long owner = contact.getOwner();
            String content = contact.getContent();
            long[] owners = new long[]{};
            if (m.containsKey(content))
                owners = m.get(content);
            m.put(content, (long[]) ArrayUtils.add(owners, owner));
        }
        return m;
    }

    public Contact[] toContactArray() {
        return toArray(new Contact[size()]);
    }

    public String[] getContactIds() {
        ArrayList<String> l = new ArrayList<String>(size() + 1);
        for (Contact contact : this) {
            if (contact != null)
                l.add(contact.getId());
        }
        return l.toArray(new String[l.size()]);
    }
}
