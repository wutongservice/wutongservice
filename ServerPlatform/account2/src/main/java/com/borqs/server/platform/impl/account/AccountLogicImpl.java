package com.borqs.server.platform.impl.account;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.platform.account2.*;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.TextEnum;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class AccountLogicImpl extends WebMethodServlet {
    private static final Logger L = LoggerFactory.getLogger(AccountLogicImpl.class);
    AccountImpl account = new AccountImpl();
    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private static String prefix  = "http://storage.aliyun.com/wutong-data/media/photo/";
    private static String sysPrefix  = "http://storage.aliyun.com/wutong-data/system/";
    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        prefix = conf.getString("platform.profileImagePattern",prefix);
        sysPrefix = conf.getString("platform.sysIconUrlPattern",sysPrefix);
        account.setConfig(this.getConfiguration());
        transceiverFactory.setConfig(conf);
        transceiverFactory.init();
    }


    @WebMethod("v2/internal/updateAccount")
    public boolean update(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Long user_id = qp.checkGetInt("user");
        String ua = getDecodeHeader(req, "User-Agent", "");
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";

        Record record = new Record();
        for (String str : qp.keySet()) {
            record.put(str, qp.getString(str, ""));
        }

        User userOrg = account.getUser(user_id);
        User user0 = readUser(record, userOrg);
        boolean b = account.update(user0);


        if (b) {
            Platform p = platform();
            Record record0 = AccountConverter.converUser2Record(user0, null);
            p.sendNodificationInternal(String.valueOf(user_id), record0, lang, user0.getDisplayName());
        }
        return b;
    }

    @WebMethod("v2/internal/getUsers")
    public JsonNode getUsers(QueryParams qp) throws AvroRemoteException {
        String users = qp.checkGetString("userIds");
        long[] longs = StringUtils2.splitIntArray(users, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("cols", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});

        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(qp, longs, users), cols, true));
    }

    @WebMethod("v2/internal/user/show")
    public JsonNode showUsers(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        if (ticket != null) {
            viewerId = p.checkSignAndTicket(qp);
        }
        String userIds = qp.checkGetString("users");
        long[] longs = StringUtils2.splitIntArray(userIds, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("columns", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});
        
        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(qp, longs, userIds), cols, true));
        //return p.getUsers(viewerId, userIds, qp.getString("columns", Platform.USER_LIGHT_COLUMNS_USER_SHOW));
    }

    @WebMethod("v2/internal/getFriends")
    public JsonNode getFriends(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        String friendsId = p.getFriendsId(
                qp.checkGetString("viewerId"),
                qp.checkGetString("userId"),
                qp.checkGetString("circleIds"),
                qp.checkGetString("cols"),
                (int) qp.checkGetInt("page"),
                (int) qp.checkGetInt("count"));

        if (StringUtils.isEmpty(friendsId))
            return null;

        long[] longs = StringUtils2.splitIntArray(friendsId, ",");

        String[] s = (String[]) ArrayUtils.addAll(StringUtils2.splitArray(qp.getString("cols", ""), ",", true), User.FULL_COLUMNS);
        String[] cols = CollectionsHelper.removeElements(s, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});

        return JsonHelper.parse(UserHelper.usersToJson(getUsers0(qp, longs, friendsId), cols, true));

    }

    private List<User> getUsers0(QueryParams qp, long[] userIds, String userId) throws AvroRemoteException {
        List<User> users = account.getUsers(userIds);
        if (users.isEmpty())
            return null;

        Map<Long, User> map = new HashMap<Long, User>();
        map.clear();
        for (User u : users)
            map.put(u.getUserId(), u);

        Platform p = platform();
        RecordSet rs = p.getUsers(qp.checkGetString("viewerId"), userId, qp.checkGetString("cols"), true);

        if (rs.isEmpty())
            return null;

        List<User> userList = new ArrayList<User>();
        for (Record r : rs) {
            long user_id = r.getInt("user_id");
            User user = map.get(user_id);
            users.get(0).toString();
            if (user == null)
                continue;

            user.setAddon("miscellaneous", r.getString("miscellaneous", ""));
            user.setAddon("bidi", r.getBoolean("bidi", false));
            user.setAddon("in_circles", r.getString("in_circles", "[]"));
            user.setAddon("his_friend", r.getBoolean("his_friend", false));
            user.setAddon("favorites_count", r.getInt("favorites_count", 1));
            user.setAddon("friends_count", r.getInt("friends_count", 0));
            user.setAddon("followers_count", r.getInt("followers_count", 0));
            user.setAddon("shared_count", r.getString("shared_count", ""));
            user.setAddon("profile_privacy", r.getBoolean("profile_privacy", false));
            user.setAddon("pedding_requests", r.getString("pedding_requests", "[]"));
            user.setAddon("profile_friends", r.getString("profile_friends", "[]"));
            user.setAddon("profile_followers", r.getString("profile_followers", "[]"));
            user.setAddon("profile_shared_photos", r.getString("profile_shared_photos", "[]"));
            userList.add(user);
        }
        addPrefix(userList);
        return userList;
    }

    private void addPrefix(List<User> data) {
        for (User user : data) {
            if (user != null) {
                PhotoInfo pi = user.getPhoto();
                if (pi != null)
                    pi.addUrlPrefix(prefix);
                else {
                    pi = new PhotoInfo();
                    pi.addDefualtUrlPrefix(sysPrefix);
                    user.setPhoto(pi);
                }
            }
        }
    }

    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }

    private static User readUser(Record req, User org) {
        User user = new User();
        user.setUserId(org.getUserId());
        if (req.has(User.COL_DISPLAY_NAME))
            user.setName(NameInfo.split(req.getString(User.COL_DISPLAY_NAME)));

        for (Schema.Column c : Schema.columns()) {
            String col = c.column;
            if (c.type == Schema.Column.Type.SIMPLE) {
                if (!req.has(col))
                    continue;

                Object value = Schema.parseSimpleValue(c.simpleType, req.getString(col));
                user.setProperty(col, value);
            } else if (c.type == Schema.Column.Type.OBJECT) {
                Map<String, String> strMap = getMap(col + ".", true, null, req);

                if (MapUtils.isEmpty(strMap))
                    continue;

                Object oldVal = org.getProperty(col, null);
                StringablePropertyBundle newVal = (StringablePropertyBundle) (oldVal != null ? ((Copyable) oldVal).copy() : c.newDefaultValue());
                Map<Integer, Object> props = toProperties(strMap, newVal.subMap());
                newVal.readProperties(props, true);
                user.setProperty(col, newVal);
            } else if (c.type == Schema.Column.Type.OBJECT_ARRAY || c.type == Schema.Column.Type.SIMPLE_ARRAY) {
                if (!req.has(col))
                    continue;

                JsonNode jn = JsonHelper.parse(req.getString(col));
                Object v = User.propertyFromJsonNode(c, jn);
                user.setProperty(col, v);
            }
        }
        return user;
    }

    public static Map<String, String> getMap(String keyPrefix, boolean removePrefix, Map<String, String> reuse, Record record) {
        if (reuse == null)
            reuse = new LinkedHashMap<String, String>();
        for (String key : record.keySet()) {
            if (key.startsWith(keyPrefix)) {
                String v = record.getString(key);
                if (removePrefix)
                    reuse.put(StringUtils.removeStart(key, keyPrefix), v);
                else
                    reuse.put(key, v);
            }
        }
        return reuse;
    }

    private static Map<Integer, Object> toProperties(Map<String, String> strMap, TextEnum te) {
        HashMap<Integer, Object> props = new HashMap<Integer, Object>();
        for (Map.Entry<String, String> e : strMap.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            Integer nk = te.getValue(k);
            if (nk != null)
                props.put(nk, v);
        }
        return props;
    }

    protected static String getDecodeHeader(HttpServletRequest req, String name, String def) throws UnsupportedEncodingException {
        String v = req.getHeader(name);
        return StringUtils.isNotEmpty(v) ? java.net.URLDecoder.decode(v, "UTF-8") : def;
    }

}
