package com.borqs.server.platform.feature.comment;


import com.borqs.server.platform.feature.Target;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class Comments extends ArrayList<Comment> implements JsonBean {
    public Comments(int initialCapacity) {
        super(initialCapacity);
    }

    public Comments() {
    }

    public Comments(Collection<? extends Comment> c) {
        super(c);
    }

    public Comments(Comment... comments) {
        Collections.addAll(this, comments);
    }

    @Override
    public void deserialize(JsonNode jn) {
        for (int i = 0; i < jn.size(); i++) {
            Comment comment = Comment.fromJsonNode(jn.get(i));
            add(comment);
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jg, provider);
    }

    @Override
    public void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException, JsonProcessingException {
        serialize(jg, (String[]) null);
    }

    public void serialize(JsonGenerator jg, String[] cols) throws IOException {
        jg.writeStartArray();
        for (Comment comment : this) {
            if (comment != null)
                comment.serialize(jg, cols);
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

    public static Comments fromJsonNode(Comments reuse, JsonNode jn) {
        Comments comments = reuse != null ? reuse : new Comments();
        comments.deserialize(jn);
        return comments;
    }

    public static Comments fromJson(Comments reuse, String json) {
        return fromJsonNode(reuse, JsonHelper.parse(json));
    }

    @Override
    public String toString() {
        return toJson(null, true);
    }

    public Target[] getCommentTargets() {
        ArrayList<Target> targets = new ArrayList<Target>();
        for (Comment comment : this) {
            if (comment != null)
                targets.add(comment.getCommentTarget());
        }
        return targets.toArray(new Target[targets.size()]);
    }

    public Comment getComment(long commentId) {
        for (Comment comment : this) {
            if (comment != null && comment.getCommentId() == commentId)
                return comment;
        }
        return null;
    }

    public Comments getComments(Comments reuse, long... commentIds) {
        if (reuse == null)
            reuse = new Comments();

        for (Comment comment : this) {
            if (comment != null && ArrayUtils.contains(commentIds, comment.getCommentId()))
                reuse.add(comment);
        }

        return reuse;
    }

    public Comment[] getCommentArray() {
        return toArray(new Comment[size()]);
    }

    public PeopleIds getCommenter() {
        PeopleIds source = new PeopleIds();
        for (Comment comment : this) {
            if (comment != null)
                source.add(comment.getCommenterPeople());
        }
        return source.unique();
    }

    public long[] getSourceIds() {
        LinkedHashSet<Long> l = new LinkedHashSet<Long>();
        for (Comment comment : this) {
            if (comment != null)
                l.add(comment.getCommenterId());
        }
        return CollectionsHelper.toLongArray(l);
    }

    public PeopleIds getAddTo() {
        PeopleIds addTo = new PeopleIds();
        for (Comment comment : this) {
            if (comment != null && comment.getAddTo() != null)
                addTo.addAll(comment.getAddTo());
        }
        return addTo.unique();
    }
}
