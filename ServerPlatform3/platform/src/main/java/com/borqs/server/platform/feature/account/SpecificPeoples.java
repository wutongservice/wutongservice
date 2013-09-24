package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.feature.friend.PeopleIds;
import org.apache.commons.collections.MapUtils;

import java.util.HashMap;
import java.util.Map;

public class SpecificPeoples {

    private final Map<String, PeopleIds> peoples = new HashMap<String, PeopleIds>();

    private static final SpecificPeoples INSTANCE = new SpecificPeoples();

    private SpecificPeoples() {
    }

    public static SpecificPeoples getInstance() {
        return INSTANCE;
    }


    public void setPeoplesWithString(Map<String, String> peoples) {
        HashMap<String, PeopleIds> m = new HashMap<String, PeopleIds>();
        for (Map.Entry<String, String> e : peoples.entrySet())
            m.put(e.getKey(), PeopleIds.parse(null, e.getValue()));
        setPeoples(m);
    }

    public Map<String, PeopleIds> getPeoples() {
        return peoples;
    }

    public void setPeoples(Map<String, PeopleIds> peoples) {
        this.peoples.clear();
        if (MapUtils.isNotEmpty(peoples))
            this.peoples.putAll(peoples);
    }

    public PeopleIds getPeopleIds(String category) {
        PeopleIds peopleIds = peoples.get(category);
        return peopleIds != null ? peopleIds : new PeopleIds();
    }

    public long[] getUserIds(String category) {
        return getPeopleIds(category).getUserIds();
    }
}
