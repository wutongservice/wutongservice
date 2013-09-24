package com.borqs.server.platform.feature.stream;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class Posts extends ArrayList<Post> implements JsonBean {
    public Posts(int initialCapacity) {
        super(initialCapacity);
    }

    public Posts() {
    }

    public Posts(Collection<? extends Post> c) {
        super(c);
    }

    public Posts(Post... posts) {
        Collections.addAll(this, posts);
    }

    public Target[] getPostTargets() {
        ArrayList<Target> targets = new ArrayList<Target>();
        for (Post post : this) {
            if (post != null)
                targets.add(post.getPostTarget());
        }
        return targets.toArray(new Target[targets.size()]);
    }

    @Override
    public void deserialize(JsonNode jn) {
        for (int i = 0; i < jn.size(); i++)
            add(Post.fromJsonNode(jn.get(i)));
    }

    public static Posts fromJsonNode(Posts reuse, JsonNode jn) {
        if (reuse == null)
            reuse = new Posts();
        reuse.deserialize(jn);
        return reuse;
    }

    public static Posts fromJson(Posts reuse, String json) {
        return fromJsonNode(reuse, JsonHelper.parse(json));
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
        for (Post post : this) {
            if (post != null)
                post.serialize(jg, cols);
        }
        jg.writeEndArray();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    public long[] getSourceIds() {
        LinkedHashSet<Long> l = new LinkedHashSet<Long>();
        for (Post post : this) {
            if (post != null)
                l.add(post.getSourceId());
        }
        return CollectionsHelper.toLongArray(l);
    }

    public PeopleIds getSource() {
        PeopleIds source = new PeopleIds();
        for (Post post : this) {
            if (post != null)
                source.add(post.getSourcePeople());
        }
        return source.unique();
    }

    public PeopleIds getTo() {
        PeopleIds to = new PeopleIds();
        for (Post post : this) {
            if (post != null && post.getTo() != null)
                to.addAll(post.getTo());
        }
        return to.unique();
    }

    public PeopleIds getAddTo() {
        PeopleIds addTo = new PeopleIds();
        for (Post post : this) {
            if (post != null && post.getAddTo() != null)
                addTo.addAll(post.getAddTo());
        }
        return addTo.unique();
    }

    public String[] getAttachmentIds(int types) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();
        for (Post post : this) {
            if (post == null)
                continue;

            if ((post.getType() & types) != 0) {
                String[] ss = post.getAttachmentIds();
                if (ArrayUtils.isNotEmpty(ss))
                    Collections.addAll(l, ss);
            }
        }
        return l.toArray(new String[l.size()]);
    }

    public long[] getQuoteIds() {
        LinkedHashSet<Long> l = new LinkedHashSet<Long>();
        for (Post post : this) {
            if (post != null) {
                long quote = post.getQuote();
                if (quote > 0)
                    l.add(quote);
            }
        }
        return CollectionsHelper.toLongArray(l);
    }

    public Post getPost(long postId) {
        for (Post post : this) {
            if (post != null && postId == post.getPostId())
                return post;
        }
        return null;
    }

    public void removeSourceless(Context ctx, AccountLogic account) {
        long[] sourceIds = getSourceIds();
        long[] existsSourceIds = account.getExistsIds(ctx, sourceIds);

        ArrayList<Post> l = new ArrayList<Post>();
        for (Post post : this) {
            if (post != null) {
                if (ArrayUtils.contains(existsSourceIds, post.getSourceId()))
                    l.add(post);
            }
        }
        clear();
        addAll(l);
    }
}
