package com.borqs.server.compatible;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.Circles;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.LinkedHashSet;

public class CompatibleCircle {
    public static final String V1COL_CIRCLE_ID = "circle_id";
    public static final String V1COL_CIRCLE_NAME = "circle_name";
    public static final String V1COL_MEMBER_COUNT = "member_count";
    public static final String V1COL_UPDATED_TIME = "updated_time";
    public static final String V1COL_MEMBERS = "members";

    public static final String[] CIRCLE_COLUMNS = {
            V1COL_CIRCLE_ID,
            V1COL_CIRCLE_NAME,
            V1COL_MEMBER_COUNT,
            V1COL_UPDATED_TIME,
    };

    public static final String[] CIRCLE_COLUMNS_WITH_MEMBERS = {
            V1COL_CIRCLE_ID,
            V1COL_CIRCLE_NAME,
            V1COL_MEMBER_COUNT,
            V1COL_UPDATED_TIME,
            V1COL_MEMBERS,
    };


    public static String[] v1ToV2Columns(String[] v1Cols) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();
        if (ArrayUtils.contains(v1Cols, V1COL_CIRCLE_ID))
            l.add(Circle.COL_CIRCLE_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_CIRCLE_NAME))
            l.add(Circle.COL_CIRCLE_NAME);
        if (ArrayUtils.contains(v1Cols, V1COL_MEMBER_COUNT))
            l.add(Circle.COL_MEMBER_COUNT);
        if (ArrayUtils.contains(v1Cols, V1COL_UPDATED_TIME))
            l.add(Circle.COL_UPDATED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_MEMBERS))
            l.add(Circle.COL_MEMBERS);
        return l.toArray(new String[l.size()]);
    }

    public static String circleToJson(final Circle circle, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeCircle(jg, circle, v1Cols);
            }
        }, human);
    }

    public static String circlesToJson(final Circles circles, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeCircles(jg, circles, v1Cols);
            }
        }, human);
    }

    public static void serializeCircle(JsonGenerator jg, Circle circle, String[] v1Cols) throws IOException {
        jg.writeStartObject();

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CIRCLE_ID))
            jg.writeNumberField(V1COL_CIRCLE_ID, circle.getCircleId());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CIRCLE_NAME))
            jg.writeStringField(V1COL_CIRCLE_NAME, ObjectUtils.toString(circle.getCircleName()));

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_UPDATED_TIME))
            jg.writeNumberField(V1COL_UPDATED_TIME, circle.getUpdatedTime());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MEMBER_COUNT))
            jg.writeNumberField(V1COL_MEMBER_COUNT, circle.getMemberCount());

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MEMBERS))
            circle.writeAddonJsonAs(jg, Circle.COL_MEMBERS, V1COL_MEMBERS, USER_TRANSFORMER);

        jg.writeEndObject();
    }

    public static void serializeCircles(JsonGenerator jg, Circles circles, String[] v1Cols) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(circles)) {
            for (Circle circle : circles) {
                if (circle != null)
                    serializeCircle(jg, circle, v1Cols);
            }
        }
        jg.writeEndArray();
    }

    public static String v2ToV1Json(String json, String[] v1Cols, boolean human) {
        JsonNode jn = JsonHelper.parse(json);
        if (jn.isArray()) {
            final Circles circles = Circles.fromJsonNode(null, jn);
            return circlesToJson(circles, v1Cols, human);
        } else if (jn.isObject()) {
            final Circle circle  = Circle.fromJsonNode(jn);
            return circleToJson(circle, v1Cols, human);
        } else {
            throw new IllegalArgumentException();
        }
    }


    public static class CircleJsonTransformer implements Addons.AddonValueTransformer {

        public final String[] circleV1Columns;

        public CircleJsonTransformer(String[] circleV1Columns) {
            this.circleV1Columns = circleV1Columns;
        }

        @Override
        public Object transform(Object old) {
            String json = ObjectUtils.toString(old);
            return Addons.jsonAddonValue(v2ToV1Json(json, circleV1Columns, true));
        }
    }

    private static final String[] USER_V1_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    private static final CompatibleUser.UserJsonTransformer USER_TRANSFORMER = new CompatibleUser.UserJsonTransformer(USER_V1_COLUMNS);
}
