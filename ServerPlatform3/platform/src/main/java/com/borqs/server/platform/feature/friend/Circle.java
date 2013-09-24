package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.PlatformResource;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.json.JsonBean;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;

public class Circle extends Addons implements JsonBean, Copyable<Circle> {

    public static final String COL_CIRCLE_ID = "circle_id";
    public static final String COL_CIRCLE_NAME = "circle_name";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_FRIEND_IDS = "friends";
    public static final String COL_MEMBER_COUNT = "member_count";
    public static final String COL_MEMBERS = "members";

    public static final String[] CIRCLE_COLUMNS = {
            Circle.COL_CIRCLE_ID,
            Circle.COL_CIRCLE_NAME,
            Circle.COL_UPDATED_TIME,
            Circle.COL_MEMBER_COUNT,
    };

    public static final String[] CIRCLE_COLUMNS_WITH_MEMBERS = {
            Circle.COL_CIRCLE_ID,
            Circle.COL_CIRCLE_NAME,
            Circle.COL_UPDATED_TIME,
            Circle.COL_MEMBER_COUNT,
            Circle.COL_MEMBERS,
    };

    public static final int MAX_CUSTOM_CIRCLE_COUNT = 20;
    public static final int MIN_CUSTOM_CIRCLE_ID = 101;
    public static final int MAX_CUSTOM_CIRCLE_ID = 250;

    public static final int CIRCLE_PUBLIC = 0;
    public static final int CIRCLE_ALL_FRIEND = 1;
    public static final int CIRCLE_STRANGER = 2;
    public static final int CIRCLE_FOLLOWER = 3;

    public static final int CIRCLE_BLOCKED = 4;
    public static final int CIRCLE_ADDRESS_BOOK = 5;
    public static final int CIRCLE_DEFAULT = 6;
    public static final int CIRCLE_ME = 7;
    public static final int CIRCLE_FAMILY = 9;
    public static final int CIRCLE_CLOSED_FRIENDS = 10;
    public static final int CIRCLE_ACQUAINTANCE = 11;



    public static final int[] BUILTIN_VIRTUAL_CIRCLES = {
            CIRCLE_PUBLIC, CIRCLE_ALL_FRIEND, CIRCLE_STRANGER, CIRCLE_FOLLOWER, CIRCLE_ME, CIRCLE_CLOSED_FRIENDS
    };

    public static final int[] BUILTIN_ACTUAL_CIRCLES = {
            CIRCLE_BLOCKED, CIRCLE_ADDRESS_BOOK, CIRCLE_DEFAULT, CIRCLE_FAMILY, CIRCLE_ACQUAINTANCE, CIRCLE_CLOSED_FRIENDS
    };

    public static final int[] BUILTIN_FINITE_CIRCLES = {
            CIRCLE_ALL_FRIEND, CIRCLE_BLOCKED, CIRCLE_ADDRESS_BOOK, CIRCLE_DEFAULT,
            CIRCLE_ME, CIRCLE_FAMILY, CIRCLE_ACQUAINTANCE, CIRCLE_CLOSED_FRIENDS
    };

    public static final int[] BUILTIN_INFINITE_CIRCLES = {
            CIRCLE_PUBLIC, CIRCLE_STRANGER, CIRCLE_FOLLOWER
    };


    private int circleId;
    private String circleName;
    private long updatedTime;
    private PeopleIds friendIds;
    private int memberCount = 0;

    public Circle() {
    }

    public Circle(int circleId) {
        this(circleId, "", 0, null);
    }

    public Circle(int circleId, String circleName, long updatedTime) {
        this(circleId, circleName, updatedTime, null);
    }

    public Circle(int circleId, String circleName, long updatedTime, PeopleIds friendIds) {
        this.circleId = circleId;
        this.circleName = circleName;
        this.updatedTime = updatedTime;
        this.friendIds = friendIds;
    }

    public int getCircleId() {
        return circleId;
    }

    public void setCircleId(int circleId) {
        this.circleId = circleId;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public PeopleIds getFriendIds() {
        return friendIds;
    }

    public void setFriendIds(PeopleIds friendIds) {
        this.friendIds = friendIds;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public static int[] getCircleIds(Collection<Circle> circles) {
        LinkedHashSet<Integer> circleIds = new LinkedHashSet<Integer>();
        if (CollectionUtils.isNotEmpty(circles)) {
            for (Circle circle : circles)
                circleIds.add(circle.getCircleId());
        }
        return CollectionsHelper.toIntArray(circleIds);
    }


    public static String getBuiltinCircleName(int circleId, String loc) {
        ResourceBundle res = PlatformResource.getResource(loc);
        switch (circleId) {
            case CIRCLE_BLOCKED:
                return res.getString(PlatformResource.CIRCLE_NAME_BLOCKED);
            case CIRCLE_ADDRESS_BOOK:
                return res.getString(PlatformResource.CIRCLE_NAME_ADDRESS_BOOK);
            case CIRCLE_DEFAULT:
                return res.getString(PlatformResource.CIRCLE_NAME_DEFAULT);
            case CIRCLE_FAMILY:
                return res.getString(PlatformResource.CIRCLE_NAME_FAMILY);
            case CIRCLE_ACQUAINTANCE:
                return res.getString(PlatformResource.CIRCLE_NAME_ACQUAINTANCE);
            case CIRCLE_CLOSED_FRIENDS:
                return res.getString(PlatformResource.CIRCLE_NAME_CLOSED_FRIENDS);
        }
        return "Circle " + circleId;
    }


    public static boolean isCustomCircleId(int circleId) {
        return circleId >= MIN_CUSTOM_CIRCLE_ID && circleId <= MAX_CUSTOM_CIRCLE_ID;
    }


    public static boolean isActualCircle(int circleId) {
        return ArrayUtils.contains(BUILTIN_ACTUAL_CIRCLES, circleId) || isCustomCircleId(circleId);
    }

    public static boolean isVirtualCircle(int circleId) {
        return ArrayUtils.contains(BUILTIN_VIRTUAL_CIRCLES, circleId);
    }

    public static boolean isFiniteCircle(int circleId) {
        return ArrayUtils.contains(BUILTIN_FINITE_CIRCLES, circleId) || isCustomCircleId(circleId);
    }

    public static boolean isInfiniteCircle(int circleId) {
        return ArrayUtils.contains(BUILTIN_INFINITE_CIRCLES, circleId);
    }

    public static boolean isBuiltinActualCircle(int circleId) {
        return ArrayUtils.contains(BUILTIN_ACTUAL_CIRCLES, circleId);
    }

    @Override
    public void deserialize(JsonNode jn) {
        setCircleId(jn.path(COL_CIRCLE_ID).getValueAsInt());
        setCircleName(ObjectUtils.toString(jn.path(COL_CIRCLE_NAME).getTextValue()));
        setUpdatedTime(jn.path(COL_UPDATED_TIME).getValueAsLong());
        JsonNode friendIdsNode = jn.path(COL_FRIEND_IDS);
        if (friendIdsNode.isArray()) {
            PeopleIds friendIds = new PeopleIds();
            for (int i = 0; i < friendIds.size(); i++) {
                JsonNode friendIdNode = friendIdsNode.get(i);
                if (friendIdNode.isIntegralNumber()) {
                    friendIds.add(PeopleId.fromId(friendIdNode.getValueAsLong()));
                } else {
                    friendIds.add(PeopleId.fromId(friendIdNode.getValueAsText()));
                }
            }
            setFriendIds(friendIds);
        } else {
            setFriendIds(new PeopleIds());
        }
        setMemberCount(jn.path(COL_MEMBER_COUNT).getValueAsInt(0));
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
        jg.writeStartObject();

        if (outputColumn(cols, COL_CIRCLE_ID))
            jg.writeNumberField(COL_CIRCLE_ID, getCircleId());

        if (outputColumn(cols, COL_CIRCLE_NAME))
            jg.writeStringField(COL_CIRCLE_NAME, ObjectUtils.toString(getCircleName()));

        if (outputColumn(cols, COL_UPDATED_TIME))
            jg.writeNumberField(COL_UPDATED_TIME, getUpdatedTime());

        if (outputColumn(cols, COL_FRIEND_IDS)) {
            jg.writeFieldName(COL_FRIEND_IDS);
            jg.writeStartArray();
            if (CollectionUtils.isNotEmpty(friendIds)) {
                for (PeopleId friendId : friendIds)
                    jg.writeObject(friendId.toId());
            }
            jg.writeEndArray();
        }

        if (outputColumn(cols, COL_MEMBER_COUNT))
            jg.writeNumberField(COL_MEMBER_COUNT, getMemberCount());

        writeAddonsJson(jg, cols);

        jg.writeEndObject();
    }

    @Override
    public String toString() {
        return JsonHelper.toJson(this, true);
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }

    public static Circle fromJsonNode(JsonNode jn) {
        Circle circle = new Circle();
        circle.deserialize(jn);
        return circle;
    }

    public static Circle fromJson(String json) {
        return fromJsonNode(JsonHelper.parse(json));
    }

    @Override
    public Circle copy() {
        Circle c = new Circle(circleId, circleName, updatedTime, friendIds != null ? new PeopleIds(friendIds) : null);
        c.setMemberCount(memberCount);
        return c;
    }
}
