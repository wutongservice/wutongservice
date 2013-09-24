package com.borqs.server.wutong.usersugg;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.log.TraceCall;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ErrorUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.qiupu.QiupuLogic;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.contacts.SocialContactsLogic;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;

import java.util.*;

import static com.borqs.server.wutong.Constants.*;

public class SuggestedUserImpl implements SuggestedUserLogic, Initializable {
    private static final Logger L = Logger.getLogger(SuggestedUserImpl.class);


    protected final Schema suggestedUserSchema = Schema.loadClassPath(SuggestedUserImpl.class, "suggestedUser.schema");
    private ConnectionFactory connectionFactory;
    private String db;
    private String suggestedUserTable;
    private String qiupuUid;

    public static final long DEFAULT_IGNORE_BACK_DATE = 24L * 60 * 60 * 30 * 1000;

    public SuggestedUserImpl() {
    }

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
        suggestedUserSchema.loadAliases(conf.getString("schema.suggestedUser.alias", null));
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("suggesteduser.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("suggesteduser.simple.db", null);
        this.suggestedUserTable = conf.getString("suggesteduser.simple.suggestedUserTable", "suggested_user");
        this.qiupuUid = conf.getString("qiupu.uid", "102");
    }

    @Override
    public void destroy() {
        this.suggestedUserTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    private static String toStr(Object o) {
        return ObjectUtils.toString(o);
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    protected boolean refuseSuggestUser0(String userId, String suggested) {
        final String SQL = "UPDATE ${table} SET ${alias.refuse_time}=${v(refuse_time)} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", suggestedUserSchema.getAllAliases()},
                {"suggested", suggested},
                {"table", suggestedUserTable},
                {"refuse_time", DateUtils.nowMillis()},
                {"user", userId},
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    @TraceCall
    @Override
    public boolean refuseSuggestUser(Context ctx, String userId, String suggested) {
        try {
            return refuseSuggestUser0(toStr(userId), toStr(suggested));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean deleteSuggestUser0(String userId, String suggested) {
        final String SQL = "delete from ${table} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL,
                "table", suggestedUserTable, "alias", suggestedUserSchema.getAllAliases(), "user", userId, "suggested", suggested);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean deleteSuggestUser(Context ctx, String userId, String suggested) {
        try {
            return deleteSuggestUser0(toStr(userId), toStr(suggested));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getSuggestUser0(String userId, int limit) {
        final String sql = "select * from suggested_user where user=" + userId + " and refuse_time=0 and " +
                "suggested not in (select friend from friend where user=" + userId + " and type=0 and circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " order by type,create_time limit " + limit + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getSuggestUser(Context ctx, String userId, int limit) {
        try {
            return getSuggestUser0(toStr(userId), limit);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getIfExistSuggestUser0(String meId, String suggestUserId) {
        final String SQL = "SELECT * FROM ${table}"
                + " WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggestUserId)} ORDER BY ${alias.type}";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", suggestedUserSchema.getAllAliases()},
                {"table", suggestedUserTable},
                {"user", meId},
                {"suggestUserId", suggestUserId},
        });
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getIfExistSuggestUser(Context ctx, String userId, String suggestUserId) {
        try {
            return getIfExistSuggestUser0(toStr(userId), toStr(suggestUserId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean create0(String toUserId, String beSuggested, int type, String reson) {
        if (toUserId.equals(beSuggested)) {
            return false;
        }
        Record r = getIfExistSuggestUser0(toUserId, beSuggested).getFirstRecord();
        if (!r.isEmpty()) {//exist want update
            int oldType = (int) r.getInt("type");
            //update old
            if (type < oldType) {
                updateSuggestUser0(toUserId, beSuggested, type, reson);
            } else if (type == oldType && type == 10) {
                String udReason = r.getString("reason") + "," + reson;
                updateSuggestUser0(toUserId, beSuggested, type, udReason);
            }
            return true;
        } else {
            if (!beSuggested.equals(qiupuUid)) {
                final String SQL = "INSERT INTO ${table} VALUES(${v(user)},${v(suggested)},${v(create_time)},0,${v(type)},${v(reason)})";
                String sql = SQLTemplate.merge(SQL, new Object[][]{
                        {"alias", suggestedUserSchema.getAllAliases()},
                        {"suggested", beSuggested},
                        {"table", suggestedUserTable},
                        {"create_time", DateUtils.nowMillis()},
                        {"user", toUserId},
                        {"type", type},
                        {"reason", reson},});
                SQLExecutor se = getSqlExecutor();
                long n = se.executeUpdate(sql);
                return n > 0;
            } else {
                return true;
            }
        }
    }

    @TraceCall
    @Override
    public boolean createSuggestUser(Context ctx, String userId, String suggestedUsers, int type, String reason) {
        try {
            String[] suggestedUserIds = StringUtils2.splitArray(toStr(suggestedUsers), ",", true);
            for (String suggestedUserId : suggestedUserIds) {
                if (!toStr(userId).equals(toStr(suggestedUserId)))
                    create0(toStr(userId), toStr(suggestedUserId), type, toStr(reason));
            }
            return 1 > 0;
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean updateSuggestUser0(String userId, String suggested, int type, String reason) {
        final String SQL = "UPDATE ${table} SET ${alias.type}=${v(type)},${alias.reason}=${v(reason)} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", suggestedUserSchema.getAllAliases()},
                {"suggested", suggested},
                {"type", type},
                {"reason", reason},
                {"table", suggestedUserTable},
                {"user", userId},
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean updateSuggestUser(Context ctx, String userId, String suggestUserId, int type, String reason) {
        try {
            return updateSuggestUser0(toStr(userId), toStr(suggestUserId), type, toStr(reason));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected String getWhoSuggest0(String to, String beSuggested) {
        final String SQL = "SELECT ${alias.reason} FROM ${table}"
                + " WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)} AND ${alias.type}=${v(type)} AND ${alias.refuse_time}=0";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", suggestedUserSchema.getAllAliases()},
                {"table", suggestedUserTable},
                {"user", to},
                {"suggested", beSuggested},
                {"type", Integer.valueOf(RECOMMENDER_USER)}
        });
        SQLExecutor se = getSqlExecutor();
        Object o = se.executeScalar(sql);
        if (o == null)
            return "";
        else
            return String.valueOf(o);
    }

    @TraceCall
    @Override
    public String getWhoSuggest(Context ctx, String to, String beSuggested) {
        try {
            return getWhoSuggest0(toStr(to), toStr(beSuggested));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected boolean backSuggestUser0(String userId, long dateDiff) {
        long flagDate = DateUtils.nowMillis() - dateDiff;
        final String sql = "update suggested_user set refuse_time=0 where refuse_time<>0 and user=" + userId + " and refuse_time<=" + flagDate + "";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @TraceCall
    @Override
    public boolean backSuggestUser(Context ctx, String userId, long dateDiff) {
        try {
            return backSuggestUser0(toStr(userId), dateDiff);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getSuggestFromBothFriend0(String userId) {
        final String sql = "select distinct(user) from friend where type=0 and " +
                "friend<>" + qiupuUid +
                " and user not in (select friend from friend where type=0 and friend<>" + qiupuUid + " and user=" + userId + " AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " and user not in (select suggested from suggested_user where user=" + userId + ")"
                +" and friend in (select friend from friend where type=0 and friend<>" + qiupuUid + " and user=" + userId + " AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getSuggestFromBothFriend(Context ctx, String userId) {
        try {
            return getSuggestFromBothFriend0(toStr(userId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getSuggestFromHasMyContactInfo0(String userId) {
        final String sql = "select distinct(owner) from social_contacts where uid=" + userId + "" +
                " and owner not in (select friend from friend where user=" + userId + " and type=0 AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " and owner not in (select suggested from suggested_user where user=" + userId + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getSuggestFromHasMyContactInfo(Context ctx, String userId) {
        try {
            return getSuggestFromHasMyContactInfo0(toStr(userId));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getSuggestUserHistory0(String userId, int limit) {
        final String sql = "select * from suggested_user where user=" + userId + " and refuse_time=0 order by type,create_time limit " + limit + "";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getSuggestUserHistory(Context ctx, String userId, int limit) {
        try {
            return getSuggestUserHistory0(toStr(userId), limit);
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    protected RecordSet getWhoSuggestedHim0(String userId, String beSuggested) {
        final String sql = "select * from suggested_user where user='" + userId + "' and suggested='" + beSuggested + "' and type=10 and reason<>'' and refuse_time=0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @TraceCall
    @Override
    public RecordSet getWhoSuggestedHim(Context ctx, String userId, String beSuggested) {
        try {
            return getWhoSuggestedHim0(toStr(userId), toStr(beSuggested));
        } catch (Throwable t) {
            throw ErrorUtils.wrapResponseError(t);
        }
    }

    // Platform

    @TraceCall
    @Override
    public boolean refuseSuggestUserP(Context ctx, String userId, String suggested) {
        Validate.notNull(userId);
        Validate.notNull(suggested);
        GlobalLogics.getAccount().checkUserIds(ctx, userId, suggested);

        return refuseSuggestUser(ctx, userId, suggested);
    }

    @TraceCall
    @Override
    public boolean createSuggestUserP(Context ctx, String userId, String suggestedUsers, int type, String reason) {
        Validate.notNull(userId);
        Validate.notNull(suggestedUsers);
        List<String> suggestedUsers0 = StringUtils2.splitList(toStr(suggestedUsers), ",", true);
        if (suggestedUsers0.size() > 0) {
            for (int i = suggestedUsers0.size() - 1; i >= 0; i--) {
                if (suggestedUsers0.get(i).length() > Constants.USER_ID_MAX_LEN)
                    suggestedUsers0.remove(i);
            }
        }
        suggestedUsers = StringUtils.join(suggestedUsers0, ",");
        suggestedUsers = Commons.parseUserIds(ctx, userId, suggestedUsers);
        GlobalLogics.getAccount().checkUserIds(ctx, userId, suggestedUsers);

        return createSuggestUser(ctx, userId, suggestedUsers, type, reason);
    }

    @TraceCall
    @Override
    public boolean deleteSuggestUserP(Context ctx, String userId, String suggested) {
        Validate.notNull(userId);
        Validate.notNull(suggested);
        GlobalLogics.getAccount().checkUserIds(ctx, userId, suggested);

        return deleteSuggestUser(ctx, userId, suggested);
    }

    @TraceCall
    @Override
    public boolean backSuggestUserP(Context ctx, String userId) {
        Validate.notNull(userId);

        return backSuggestUser(ctx, userId, DEFAULT_IGNORE_BACK_DATE);
    }

    @TraceCall
    @Override
    public boolean createSuggestUserFromHaveBorqsIdP(Context ctx, String userId) {
        SocialContactsLogic s = GlobalLogics.getSocialContacts();
        boolean b = false;
        //get info from socialContact，insert into suggested_user
        RecordSet recs = s.getSocialContactsUid(ctx, userId);
        String uids = recs.joinColumnValues("uid", ",");
        if (uids.length() > 0) {
            createSuggestUserP(ctx, userId, String.valueOf(uids), Integer.valueOf(FROM_ADDRESS_HAVEBORQSID), "");
        }
        return b;
    }

    @TraceCall
    @Override
    public boolean createSuggestUserFromHaveCommLXRP(Context ctx, String userId) {
        SocialContactsLogic s = GlobalLogics.getSocialContacts();
        boolean b = false;
        //get info from socialContact，insert into suggested_user
        RecordSet recs = s.getCommSocialContactsM(ctx, userId);
        for (Record r : recs) {
            String uid = r.getString("owner");
            RecordSet bo = s.getCommSocialContactsU(ctx, userId, uid);
            String bothF = bo.joinColumnValues("uid", ",");
            if (bothF.length() > 0) {
                createSuggestUserP(ctx, userId, uid, Integer.valueOf(FROM_ADDRESS_HAVECOMMONBORQSID), bothF);
            }
        }
        return b;
    }

    @TraceCall
    @Override
    public boolean createSuggestUserByHasMyContactP(Context ctx, String userId) {
        boolean b = false;
        //get from friend，insert into suggested_user
        RecordSet recs = getSuggestFromHasMyContactInfo(ctx, userId);
        String uids = recs.joinColumnValues("user", ",");
        if (uids.length() > 0) {
            createSuggestUserP(ctx, userId, String.valueOf(uids), Integer.valueOf(FROM_ADDRESS_HASMYCONTACTINFO), "");
        }
        return b;
    }

    @TraceCall
    @Override
    public boolean createSuggestUserFromCommonFriendsP(Context ctx, String userId) {

        FriendshipLogic f = GlobalLogics.getFriendship();
        boolean b = false;
        //get from friend，insert into suggested_user
        RecordSet recs = getSuggestFromBothFriend(ctx, userId);

        for (Record r : recs) {
            String uid = r.getString("user");
            RecordSet bo = f.getBothFriendsIds(ctx, userId, uid, 0, 200);
            String bothF = bo.joinColumnValues("friend", ",");
            if (bothF.length() > 0) {
                createSuggestUserP(ctx, userId, uid, Integer.valueOf(IN_COMMON_FRIENDS), bothF);
            }
        }
        return b;

    }

    @TraceCall
    @Override
    public boolean autoCreateSuggestUsersP(Context ctx, String userId) {
        //2,send stream many 10
        StreamLogic stream = GlobalLogics.getStream();
        RecordSet stream_user = stream.topSendStreamUser(ctx, 10);
        String sUserids = "";
        if (stream_user.size() > 0)
            sUserids = stream_user.joinColumnValues("source", ",");

        String qUserids = "";
//        //3,shared apps many 10
        QiupuLogic qp = QiupuLogics.getQiubpu();
        RecordSet qiupu_user = qp.getStrongMan(ctx,"", 0, 10);
        if (qiupu_user.size() > 0)
            qUserids = stream_user.joinColumnValues("user", ",");

        //4,many followers 10

        FriendshipLogic fs = GlobalLogics.getFriendship();
        RecordSet fs_user = fs.topUserFollowers(ctx, Long.parseLong(userId), 10);
        String fUserids = "";
        if (fs_user.size() > 0)
            fUserids = stream_user.joinColumnValues("friend", ",");


        //5,same company  5


        //6,same school  5


        //7,same sex  5


        //merge userid
        List<String> out_list = new ArrayList<String>();
        String userIds0 = "";
        if (sUserids.length() > 0)
            userIds0 = sUserids + ",";
        if (qUserids.length() > 0)
            userIds0 += qUserids + ",";
        if (fUserids.length() > 0)
            userIds0 += fUserids + ",";

        if (userIds0.length() >= 2) {
            List<String> l0 = StringUtils2.splitList(toStr(userIds0), ",", true);
            for (String u : l0) {
                if (!out_list.contains(u) && !u.equals("")) {
                    out_list.add(u);
                }
            }

            String userIds = "";
            for (String u : out_list) {
                if (!u.equals(userId))
                    userIds += u + ",";
            }
            createSuggestUserP(ctx, userId, StringUtils.substringBeforeLast(userIds, ","), Integer.valueOf(FROM_SYSTEM), "");
        }
        return true;
    }

    @TraceCall
    @Override
    public RecordSet getSuggestUserP(Context ctx, String userId, int limit, boolean getBack) {
        Validate.notNull(userId);
//        checkUserIds(userId);
        if (getBack) {
            backSuggestUserP(ctx, userId);
        }
        //if


        AccountLogic account = GlobalLogics.getAccount();

        RecordSet rs = new RecordSet();
        RecordSet rs0 = getSuggestUser(ctx, userId, limit);
        if (rs0.size() <= 10 && rs0.size() < limit) {
            //study from address ,contact have borqsid,
            createSuggestUserFromHaveBorqsIdP(ctx, userId);

            //study from address ,have common  lxr,
            createSuggestUserFromHaveCommLXRP(ctx, userId);

             //study from address,for has my contactinfo
            createSuggestUserByHasMyContactP(ctx, userId);

            //study from friend ,for common friend
            createSuggestUserFromCommonFriendsP(ctx, userId);

            // the same school

            // the same company

            //get delete from suggest
            backSuggestUserP(ctx, userId);

            rs = getSuggestUser(ctx, userId, limit);
            if (rs.size() == 0) {
                autoCreateSuggestUsersP(ctx, userId);
                rs.addAll(getSuggestUser(ctx, userId, limit));
            }
        } else {
            rs = rs0;
        }

//            RecordSet rs=  RecordSet.fromByteBuffer(su.getSuggestUser(userId, limit));
        RecordSet outrs0 = new RecordSet();
        if (rs.size() > 0) {
            for (int i = 0; i < rs.size(); i++) {
                String suggestedId = rs.get(i).getString("suggested");
                Record u = account.getUserByIdBaseColumns(ctx, suggestedId).getFirstRecord();
                if (!u.isEmpty()) {
                    u.put("suggest_type", rs.get(i).getString("type"));
                    u.put("suggest_reason", "");
                    if (rs.get(i).getInt("type") == Long.parseLong(RECOMMENDER_USER)
                            || rs.get(i).getInt("type") == Long.parseLong(FROM_ADDRESS_HAVECOMMONBORQSID)
                            || rs.get(i).getInt("type") == Long.parseLong(IN_COMMON_FRIENDS)) {
                        String reasonStr = rs.get(i).getString("reason");
                        if (reasonStr.length() > 0) {
                            RecordSet reasonUser = account.getUserByIdBaseColumns(ctx, reasonStr);
                            u.put("suggest_reason", reasonUser);
                        }
                    }
                    outrs0.add(u);
                }
            }
        }
        return outrs0;
    }

    @TraceCall
    @Override
    public boolean updateSuggestUserReasonP(Context ctx) {
        AccountLogic a = GlobalLogics.getAccount();
        SocialContactsLogic so = GlobalLogics.getSocialContacts();
        FriendshipLogic f = GlobalLogics.getFriendship();

        //==================================================

        RecordSet recs = a.findAllUserIds(ctx, true);
        for (Record r : recs) {
            RecordSet rs0 = getSuggestUserHistory(ctx, r.getString("user_id"), 1000);
            for (Record rs : rs0) {
                if (rs.getInt("type") == Integer.valueOf(FROM_ADDRESS_HAVECOMMONBORQSID)) {
                    //  共同联系人
                    RecordSet bo = so.getCommSocialContactsU(ctx, rs.getString("user"), rs.getString("suggested"));
                    String bothF = bo.joinColumnValues("uid", ",");

                    if (bothF.length() > 0) {
                        //update
                        updateSuggestUser(ctx, rs.getString("user"), rs.getString("suggested"), Integer.valueOf(FROM_ADDRESS_HAVECOMMONBORQSID), bothF);
                    }

                }

                if (rs.getInt("type") == Integer.valueOf(IN_COMMON_FRIENDS)) {
                    //  共同好友
                    RecordSet bo = f.getBothFriendsIds(ctx, rs.getString("user"), rs.getString("suggested"), 0, 1000);
                    String bothF = bo.joinColumnValues("friend", ",");
                    if (bothF.length() > 0) {
                        updateSuggestUser(ctx, rs.getString("user"), rs.getString("suggested"), Integer.valueOf(IN_COMMON_FRIENDS), bothF);
                    }
                }
            }
        }

        //==================================================

        return true;
    }

    @TraceCall
    @Override
    public boolean recommendUserP(Context ctx, String whoSuggest, String toUserId, String beSuggestedUserIds) {
        String srcName = GlobalLogics.getAccount().getUser(ctx, whoSuggest, whoSuggest, "display_name")
                .getString("display_name", "您的朋友");

        boolean b = false;
        List<String> uIds = StringUtils2.splitList(toStr(beSuggestedUserIds), ",", true);
        for (String uid : uIds) {
            b = createSuggestUserP(ctx, toUserId, uid, Integer.valueOf(RECOMMENDER_USER), whoSuggest);
        }

        String beSuggested = uIds.get(0);
        String beSuggestedName = GlobalLogics.getAccount().getUser(ctx, beSuggested, beSuggested, "display_name")
                .getString("display_name", "");

        Commons.sendNotification(ctx, NTF_SUGGEST_USER,
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(whoSuggest),
                Commons.createArrayNodeFromStrings(srcName, beSuggestedName, String.valueOf(uIds.size())),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(whoSuggest, srcName, beSuggested, beSuggestedName, String.valueOf(uIds.size())),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(),
                Commons.createArrayNodeFromStrings(toUserId)
        );

        return b;
    }

    //the same school
    @TraceCall
    @Override
    public boolean createSuggestUserFromSameSchoolP(Context ctx, String userId, Map<String, List<String>> map) {
        if (map == null)
            return true;

        RecordSet users = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), userId, "education_history");
        if (users.size() == 0) {
            return true;
        }

        Record r = users.getFirstRecord();
        JsonNode jn = r.toJsonNode();
        JsonNode schools = jn.get("education_history");
        List<String> schoolNames = schools.findValuesAsText("school");

        Set<String> set = new TreeSet<String>();
        set.addAll(schoolNames);

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey().equals(r.getString("user_id")))
                continue;

            List listEntry = entry.getValue();
            for (String s : set) {
                if (StringUtils.isNotEmpty(s)) {
                    if (listEntry.contains(s)) {
                        createSuggestUser(ctx, userId, entry.getKey(), Integer.valueOf(FROM_USERPROFILE_EDUINFO), s);
                    }
                }
            }

        }

        return true;
    }

    //the same company
    @TraceCall
    @Override
    public boolean createSuggestUserFromSameCompanyP(Context ctx, String userId, Map<String, List<String>> map) {
        if (map == null)
            return true;
        RecordSet users = GlobalLogics.getAccount().getUsers(ctx, ctx.getViewerIdString(), userId, "work_history");
        if (users.size() == 0) {
            return true;
        }

        Record r = users.getFirstRecord();
        JsonNode jn = r.toJsonNode();


        JsonNode companies = jn.get("work_history");
        List<String> companyNames = companies.findValuesAsText("company");

        Set<String> set = new TreeSet<String>();
        set.addAll(companyNames);

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey().equals(r.getString("user_id")))
                continue;

            List listEntry = entry.getValue();
            for (String s : set) {
                if (StringUtils.isNotEmpty(s)) {
                    if (listEntry.contains(s)) {
                        createSuggestUser(ctx, userId, entry.getKey(), Integer.valueOf(FROM_USERPROFILE_WORKINFO), s);
                    }
                }
            }

        }

        return true;
    }
}
