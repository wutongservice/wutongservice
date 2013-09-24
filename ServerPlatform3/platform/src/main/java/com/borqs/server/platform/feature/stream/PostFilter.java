package com.borqs.server.platform.feature.stream;


import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class PostFilter implements JsonBean {

    public int types;
    public int app;
    public long min;
    public long max;

    public Set<PeopleId> friendIds;
    public Set<PeopleId> excludeIds;

    public PostFilter() {
    }

    public PostFilter(int types, int app, long min, long max, Set<PeopleId> friendIds) {
        this(types, app, min, max, friendIds, null);
    }

    public PostFilter(int types, int app, long min, long max, Set<PeopleId> friendIds, Set<PeopleId> excludeIds) {
        this.types = types;
        this.app = app;
        this.min = min;
        this.max = max;
        this.friendIds = friendIds;
        this.excludeIds = excludeIds;
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(this, true);
    }


    @Override
    public void deserialize(JsonNode jn) {
        JsonNode sub = jn.path("types");
        types = sub.getValueAsInt(0);

        sub = jn.path("app");
        app = sub.getValueAsInt(App.APP_NONE);

        min = jn.path("min").getLongValue();
        max = jn.path("max").getLongValue();

        sub = jn.get("friends");
        if (sub == null) {
            friendIds = null;
        } else {
            HashSet<PeopleId> friends = new HashSet<PeopleId>();
            for (String s :StringHelper.splitList(sub.getTextValue(), ",", true)) {
                friends.add(PeopleId.parseStringId(s));
            }
            this.friendIds = friends;
        }

        sub = jn.get("exclude");
        if (sub == null) {
            excludeIds = null;
        } else {
            HashSet<PeopleId> excludes = new HashSet<PeopleId>();
            for (String s :StringHelper.splitList(sub.getTextValue(), ",", true)) {
                excludes.add(PeopleId.parseStringId(s));
            }
            this.excludeIds = excludes;
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        jg.writeStartObject();
        jg.writeNumberField("types", types);
        jg.writeNumberField("app", app);
        jg.writeNumberField("min", min);
        jg.writeNumberField("max", max);

        if (friendIds == null) {
            jg.writeNullField("friends");
        } else {
            jg.writeStringField("friends", new PeopleIds(friendIds).toString());
        }
        if (excludeIds == null) {
            jg.writeNullField("excludes");
        } else {
            jg.writeStringField("excludes", new PeopleIds(excludeIds).toString());
        }
        jg.writeEndObject();
    }

    public byte[] toBytes() {
        String json = JsonHelper.toJson(this, false);
        return Charsets.toBytes(json);
    }

    public static PostFilter fromBytes(byte[] bytes) {
        String json = Charsets.fromBytes(bytes);
        PostFilter filter = new PostFilter();
        filter.deserialize(JsonHelper.parse(json));
        return filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PostFilter other = (PostFilter) o;

        return types == other.types
                && app == other.app
                && min == other.min
                && max == other.max
                && SetUtils.isEqualSet(friendIds, other.friendIds);
    }

    public boolean equalsWithoutTime(PostFilter other) {
        if (this == other)
            return true;
        if (other == null)
            return false;

        return types == other.types
                && app == other.app
                && SetUtils.isEqualSet(friendIds, other.friendIds);
    }

    @Override
    public int hashCode() {
        int result = types;
        result = 31 * result + app;
        result = 31 * result + (int) (min ^ (min >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + (friendIds != null ? friendIds.hashCode() : 0);
        return result;
    }

    public PostFilter copyWithMax(long max) {
        PostFilter filter = new PostFilter();
        filter.types = types;
        filter.app = app;
        filter.max = max;
        filter.min = min;
        filter.friendIds = friendIds;
        return filter;
    }

    public static PostFilter newEmpty() {
        return new PostFilter(Post.ALL_POST_TYPES, App.APP_NONE, 0, Long.MAX_VALUE, null);
    }

    public static PostFilter newEmptyForFriends(PeopleId... friendIds) {
        HashSet<PeopleId> friends = new HashSet<PeopleId>();
        Collections.addAll(friends, friendIds);
        PostFilter filter = new PostFilter();
        filter.friendIds = friends;
        return filter;
    }

}
