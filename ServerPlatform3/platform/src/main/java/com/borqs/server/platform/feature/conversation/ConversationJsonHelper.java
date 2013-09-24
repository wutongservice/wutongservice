package com.borqs.server.platform.feature.conversation;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

import java.io.IOException;

public class ConversationJsonHelper implements JsonSerializableWithType {
    private static final String[] USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    private static final String COL_COUNT = "count";
    private static final String COL_USERS = "users";

    private long[] userIds;
    private long count;
    private AccountLogic account;

    public ConversationJsonHelper(AccountLogic account, long[] userIds, long count) {
        this.account = account;
        this.userIds = userIds;
        this.count = count;
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
        Users users = account.getUsers(Context.create(), USER_COLUMNS, userIds);

        //generate json
        jg.writeStartObject();
        jg.writeNumberField(COL_COUNT, count);
        jg.writeFieldName(COL_USERS);
        jg.writeRawValue(users.toJson(USER_COLUMNS, true));
        jg.writeEndObject();
    }

    public String toJson(final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serialize(jg, cols);
            }
        }, human);
    }
}
