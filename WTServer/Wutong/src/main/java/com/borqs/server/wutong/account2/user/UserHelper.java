package com.borqs.server.wutong.account2.user;


import com.borqs.server.wutong.account2.util.json.JsonGenerateHandler;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserHelper {

    public static List<User> usersFromJson(List<User> reuse, String json) {
        return usersFromJsonNode(reuse, JsonHelper.parse(json));
    }

    public static List<User> usersFromJsonNode(List<User> reuse, JsonNode jn) {
        List<User> l = reuse != null ? reuse : new ArrayList<User>(jn.size());
        for (int i = 0; i < jn.size(); i++)
            l.add(User.fromJsonNode(jn.get(i)));
        return l;
    }

    public static String usersToJson(final List<User> users, final String[] cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                jg.writeStartArray();
                for (User user : users) {
                    if (user != null)
                        user.serialize(jg, cols);
                    else
                        jg.writeNull();
                }
                jg.writeEndArray();
            }
        }, human);
    }

    public static long[] getUsersIds(List<User> users) {
        long[] a = new long[users.size()];
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            a[i] = user != null ? user.getUserId() : 0;
        }
        return a;
    }

    public static User findUser(List<User> users, long userId) {
        for (User user : users) {
            if (user != null && user.getUserId() == userId)
                return user;
        }
        return null;
    }
}
