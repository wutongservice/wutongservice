package com.borqs.server.platform.feature.account;


import com.borqs.server.platform.feature.TargetInfo;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.*;

public class Users extends ArrayList<User> implements JsonBean {
    public Users() {
    }

    public Users(int initialCapacity) {
        super(initialCapacity);
    }

    public Users(Collection<? extends User> c) {
        super(c);
    }

    public Users(User... users) {
        Collections.addAll(this, users);
    }


    @Override
    public void deserialize(JsonNode jn) {
        int len = jn.size();
        for (int i = 0; i < len; i++) {
            JsonNode sub = jn.get(i);
            add(User.fromJsonNode(sub));
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[])null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartArray();
        for (User user : this) {
            if (user != null)
                user.serialize(jg, cols);
        }
        jg.writeEndArray();
    }

    public static Users fromJson(Users reuse, String json) {
        return fromJsonNode(reuse, JsonHelper.parse(json));
    }

    public static Users fromJsonNode(Users reuse, JsonNode jn) {
        if (reuse == null)
            reuse = new Users();

        reuse.deserialize(jn);
        return reuse;
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }


    public long[] getUserIds() {
        ArrayList<Long> l = new ArrayList<Long>(size());
        for (User user : this) {
            if (user != null)
                l.add(user.getUserId());
        }
        return CollectionsHelper.toLongArray(l);
    }

    public PeopleIds getPeopleIds(PeopleIds reuse) {
        if (reuse == null)
            reuse = new PeopleIds();

        for (User user : this) {
            if (user != null)
                reuse.add(user.getPeopleId());
        }
        return reuse;
    }

    public String[] getDisplayNames() {
        ArrayList<String> l = new ArrayList<String>();
        for (User user : this) {
            l.add(user != null ? user.getDisplayName() : "");
        }
        return l.toArray(new String[l.size()]);
    }

    public String[] getNicknames() {
        ArrayList<String> l = new ArrayList<String>();
        for (User user : this) {
            l.add(user != null ? user.getNickname() : "");
        }
        return l.toArray(new String[l.size()]);
    }

    public User[] toUserArray() {
        return toArray(new User[size()]);
    }

    public User getUser(long userId) {
        for (User user : this) {
            if (user != null && user.getUserId() == userId)
                return user;
        }
        return null;
    }

    public Users getUsers(Users reuse, long... userIds) {
        if (reuse == null)
            reuse = new Users();

        for (long userId : userIds) {
            User user = getUser(userId);
            if (user != null)
                reuse.add(user);
        }
        return reuse;
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    public void sortByUserIds(final long... userIds) {
        if (isEmpty() || ArrayUtils.isEmpty(userIds))
            return;

        LinkedHashMap<Long, User> m = new LinkedHashMap<Long, User>();
        for (User user : this)
            m.put(user.getUserId(), user);

        ArrayList<User> users = new ArrayList<User>();
        for (long userId : userIds) {
            if (!m.containsKey(userId))
                continue;

            User user = m.get(userId);
            users.add(user);
            m.remove(userId);
        }

        users.addAll(m.values());
        clear();
        addAll(users);
    }

    public TargetInfo[] getTargetInfo() {
        ArrayList<TargetInfo> l = new ArrayList<TargetInfo>(size());
        for (User user : this) {
            if (user != null)
                l.add(user.getTargetInfo());
        }
        return l.toArray(new TargetInfo[l.size()]);
    }
}
