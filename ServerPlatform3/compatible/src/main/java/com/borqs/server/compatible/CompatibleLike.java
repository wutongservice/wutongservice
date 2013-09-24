package com.borqs.server.compatible;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.like.LikeExpansionSupport;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;

public class CompatibleLike {

    public static final String V1SUBCOL_COUNT = "count";
    public static final String V1SUBCOL_USERS = "users";

    public static final String[] LIKES_USERS_V1_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    public static String v2ToV1LikesAddonJson(String json) {
        final JsonNode jn = JsonHelper.parse(json);
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jg.writeStartObject();
                jg.writeFieldName(V1SUBCOL_COUNT);
                jg.writeTree(jn.path(LikeExpansionSupport.SUBCOL_COUNT));
                jg.writeFieldName(V1SUBCOL_USERS);
                jg.writeRawValue(CompatibleUser.v2JsonNodeToV1Json(jn.path(LikeExpansionSupport.SUBCOL_USERS), LIKES_USERS_V1_COLUMNS, true));
                jg.writeEndObject();
            }
        }, true);
    }

    public static Addons.AddonValueTransformer LIKES_ADDON_TRANSFORMER = new LikesAddonTransformer();

    private static class LikesAddonTransformer implements Addons.AddonValueTransformer {
        @Override
        public Object transform(Object old) {
            String json = ObjectUtils.toString(old);
            return Addons.jsonAddonValue(v2ToV1LikesAddonJson(json));
        }
    }
}
