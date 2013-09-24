package com.borqs.server.wutong.contacts;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.Encoders;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.friendship.FriendshipLogic;
import com.borqs.server.wutong.setting.SettingLogic;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SocialContactsImpl implements SocialContactsLogic, Initializable {
    public final Schema socialContactsSchema = Schema.loadClassPath(SocialContactsImpl.class, "socialcontacts.schema");
    Logger L = Logger.getLogger(SocialContactsImpl.class);
    private ConnectionFactory connectionFactory;
    private String db;
    private String SocialContactsTable;

    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("SocialContacts.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("socialcontacts.simple.db", null);
        this.SocialContactsTable = conf.getString("SocialContacts.simple.SocialContactsTable", "social_contacts");

    }

    @Override
    public void destroy() {

    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    public boolean createSocialContacts(Context ctx, String owner, String username, int type, String content, String uid) {
        final String METHOD = "createSocialContacts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, owner, username, type, content, uid);
        if (!hasSocialContacts0(owner, username, content, uid)) {
            if (!hasSocialContacts1(owner, username, content)) {
                createSocialContacts1(owner, username, type, content, uid);
            } else {
                if (Integer.valueOf(uid) > 0) {
                    final String SQL = "update ${table} set uid=" + uid + " WHERE ${alias.owner}=${v(owner)} AND ${alias.username}=${v(username)} AND ${alias.content}=${v(content)}";
                    String sql = SQLTemplate.merge(SQL, new Object[][]{
                            {"alias", socialContactsSchema.getAllAliases()},
                            {"table", SocialContactsTable},
                            {"owner", owner},
                            {"username", username},
                            {"content", content},});
                    SQLExecutor se = getSqlExecutor();
                    L.op(ctx, "createSocialContacts");
                    se.executeUpdate(sql);
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;
    }

    public boolean createSocialContacts1(String owner, String username, int type, String content, String uid) {
        final String SQL = "INSERT INTO ${table}(owner,username,type,content,uid) VALUES(${v(owner)},${v(username)},${v(type)},${v(content)},${v(uid)})";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", socialContactsSchema.getAllAliases()},
                {"owner", owner},
                {"table", SocialContactsTable},
                {"username", username},
                {"type", type},
                {"content", content},
                {"uid", uid},});
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    public boolean hasSocialContacts0(String owner, String username, String content, String uid) {
        final String SQL = "SELECT count(*) FROM ${table} WHERE ${alias.owner}=${v(owner)} AND ${alias.username}=${v(username)} AND ${alias.content}=${v(content)} AND ${alias.uid}=${v(uid)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", socialContactsSchema.getAllAliases()},
                {"table", SocialContactsTable},
                {"owner", owner},
                {"username", username},
                {"uid", uid},
                {"content", content},});
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }

    public boolean hasSocialContacts1(String owner, String username, String content) {
        final String SQL = "SELECT count(*) FROM ${table} WHERE ${alias.owner}=${v(owner)} AND ${alias.username}=${v(username)} AND ${alias.content}=${v(content)}";
        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"alias", socialContactsSchema.getAllAliases()},
                {"table", SocialContactsTable},
                {"owner", owner},
                {"username", username},
                {"content", content},});
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        return count.intValue() > 0;
    }

    @Override
    public RecordSet getSocialContacts(Context ctx, String owner, int type, int page, int count) {
        final String METHOD = "getSocialContacts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, owner, type, page, count);

        String sql0 = "select * from social_contacts where owner='" + owner + "' AND LENGTH(content)<>32";
        if (type != 0) {
            sql0 += " and type='" + type + "' ";
        }
        sql0 += " order by username " + SQLUtils.pageToLimit(page, count);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql0.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }


    @Override
    public RecordSet getSocialContactsUid(Context ctx, String owner) {
        final String METHOD = "getSocialContacts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, owner);
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT distinct(uid) FROM ${table} WHERE ${alias.owner}=${v(owner)} AND uid>0",
                "owner", owner, "table", SocialContactsTable, "alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }


    public RecordSet getSocialContactsOwner(Context ctx, String duserId, String uids) {
        //        StringBuilder sql = new StringBuilder();
        //        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
        //                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where uid>0 and owner<>" + duserId + " and uid in (" + uids + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    public RecordSet getCommSocialContactsM(Context ctx, String userId) {
        final String METHOD = "getCommSocialContactsM";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        //        StringBuilder sql = new StringBuilder();
        //        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
        //                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where owner<>" + userId + " and  " +
                " uid>0  and owner not in " +
                "(select friend from friend where type=0 and circle<>4 and reason<>5 and reason<>6 and user=" + userId + ") and owner not in " +
                "(select suggested from suggested_user where user=" + userId + ") and uid in (select distinct(uid) from social_contacts where uid>0 and owner=" + userId + " AND OWNER <> uid)";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getCommSocialContactsU(Context ctx, String userId, String friendId) {
        final String METHOD = "getCommSocialContactsU";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        //        StringBuilder sql = new StringBuilder();
        //        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
        //                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(uid) from social_contacts where owner=" + userId + "" +
                " and uid>0 and uid in " +
                "(select uid from social_contacts where owner in(" + friendId + ") and uid>0) and uid<>" + userId + " and uid not in(" + friendId + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getWhohasMyContacts(Context ctx, String userId, String email, String phone) {
        final String METHOD = "getWhohasMyContacts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId);
        String c = "";
        String c1 = "";
        if (!email.equals(""))
            c1 = Encoders.md5Hex(email);
        if (!phone.equals(""))
            c1 = Encoders.md5Hex(phone);

        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where (content='" + c + "' or content='" + c1 + "') and owner<>" + userId + "";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);

        String sql1 = "update social_contacts set uid=" + userId + " where (content='" + c + "' or content='" + c1 + "') and uid=0";
        long n = se.executeUpdate(sql1);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getDistinctUsername(Context ctx, String uid) {
        final String METHOD = "getDistinctUsername";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, uid);
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(username) from social_contacts where uid='" + uid + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getDistinctOwner(Context ctx, String uid, String username) {
        final String METHOD = "getDistinctOwner";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, uid, username);
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where uid='" + uid + "' and username='" + username + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getUserName(Context ctx, String owner, String uid) {
        final String METHOD = "getUserName";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, uid);
        SQLExecutor se = getSqlExecutor();
        String sql = "select username from social_contacts where uid='" + uid + "' and uid<>'0' and owner='" + owner + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet getCommSocialContactsByUid(Context ctx, String owner, String uid) {
        final String METHOD = "getCommSocialContactsByUid";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, uid);
        SQLExecutor se = getSqlExecutor();
        String sql = "select owner,uid from social_contacts where  OWNER <> uid AND owner<> " + owner + " and uid in(" + uid + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return recs;
    }

    @Override
    public RecordSet createSocialContacts(Context ctx, String userId, String updateInfo, String ua, String loc) {
        final String METHOD = "createSocialContacts";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, ua, loc);
        SocialContactsLogic s = GlobalLogics.getSocialContacts();
        SettingLogic settingLogic = GlobalLogics.getSetting();
        AccountLogic accountLogic = GlobalLogics.getAccount();
        FriendshipLogic friendshipLogic = GlobalLogics.getFriendship();

        //L.debug("===0 upload socialcontact,updateInfo=:" + updateInfo);
        updateInfo = new String(Encoders.fromBase64(updateInfo));
        //L.debug("===0 upload socialcontact,updateInfo from base64=:" + updateInfo);
        RecordSet recs = RecordSet.fromJson(updateInfo);
        boolean wantAdd = true;
        Record setting = settingLogic.getByUsers(ctx, "socialcontact.autoaddfriend", userId);
        //Record setting = Record.of(c1, v1, c2, v2, c3, v3, c4, v4, c5, v5)
        RecordSet return_rs = new RecordSet();
        //L.debug("===0 upload socialcontact:" + recs.toString(false, false));
        boolean b = true;
        List<String> ul = new ArrayList<String>();
        for (Record rec : recs) {
            //parse user info from upload
            String username = rec.getString("username");
            int type = (int) rec.getInt("type");
            String content = rec.getString("content");
            //if in user info
            String user_id = accountLogic.findUserIdByUserName(ctx, content);
            //if exist ,add friend,insert into db
            if (!user_id.equals("0") && !user_id.equals(userId)) {
                Record return_r = new Record();
                return_r.put("contact_id", rec.getString("contact_id"));
                return_r.put("user_id", user_id);
                return_r.put("username", username);
                return_r.put("type", type);
                return_r.put("content", content);

                boolean isFriend = false;
                try {
                    isFriend = friendshipLogic.isFriendP(ctx, userId, String.valueOf(user_id));
                } catch (Exception e) {
                    //L.debug("isFriend error,userId=:" + userId + ",user_id=" + user_id);
                }
                if (!isFriend && (!user_id.equals(userId))) {
                    ul.add(String.valueOf(user_id));
                }

                return_r.put("isfriend", isFriend);

                Record u = accountLogic.getUser(ctx, userId, user_id, AccountLogic.USER_COLUMNS_SHAK);
                return_r.put("display_name", u.getString("display_name"));
                return_r.put("image_url", u.getString("image_url"));
                return_r.put("remark", u.getString("remark"));
                return_r.put("perhaps_name", u.getString("perhaps_name"));
                if (StringUtils.isNotEmpty(u.getString("in_circles")))
                    return_r.put("in_circles", RecordSet.fromJson(u.getString("in_circles")));
                return_r.put("his_friend", u.getBoolean("his_friend", false));
                return_r.put("bidi", u.getBoolean("bidi", false));
                return_rs.add(return_r);
            } //if not exist ,insert into db
            try {
                b = s.createSocialContacts(ctx, userId, username, type, content, String.valueOf(user_id));
            } catch (Exception e) {
                //L.debug("createSocialContacts error,userId=:" + userId + ",username=" + username + ",type=" + type + ",content=" + content + ",user_id=" + user_id);
            }
        }

        //数据库里的标志，0为   允许加好友，1为不许自动加好友，
        int flag = 0;    //现在定的标志，0允许加好友，不发notification   1为不加好友，只发notification，加入people you may know

        //第一次自动加，不给自己发notification
        //以后,加入people you may know，给自己发notification
        if (!setting.isEmpty()) {
            String f = setting.getString(userId);
            if (f.equals("100")) {
                flag = 0;
                Record values = Record.of("socialcontact.autoaddfriend", "1");
                settingLogic.setPreferences(ctx, userId, values);
            } else if (f.equals("1")) {
                flag = 1;
            } else if (f.equals("0")) {
                flag = 0;
            }
        }

        String ulc = ul.size() > 0 ? StringUtils.join(ul, ",") : "";

        if (flag == 0) {
            if (ul.size() > 0) {
                friendshipLogic.setFriends(ctx, String.valueOf(userId), ulc, String.valueOf(Constants.ADDRESS_BOOK_CIRCLE), Constants.FRIEND_REASON_SOCIALCONTACT, true);
                friendshipLogic.setFriends(ctx, String.valueOf(userId), ulc, String.valueOf(Constants.ACQUAINTANCE_CIRCLE), Constants.FRIEND_REASON_SOCIALCONTACT, true);
            }
        } else if (flag == 1) {
            if (ul.size() > 0) {
                //L.debug("===0 upload socialcontact,ul=:" + StringUtils.join(ul, ","));
                //add in people you may know
                //createSuggestUserFromHaveBorqsId(userId);

                GlobalLogics.getSuggest().createSuggestUser(ctx, userId, StringUtils.join(ul, ","), Integer.valueOf(Constants.FROM_ADDRESS_HAVEBORQSID), "");
                //send  notification    to myself
                Commons.sendNotification(ctx, Constants.NTF_PEOPLE_YOU_MAY_KNOW,
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(userId),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId, ulc),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(),
                        Commons.createArrayNodeFromStrings(userId)
                );
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        //L.debug("===0 upload socialcontact,return_rs=:" + return_rs.toString(false, false));
        return return_rs;

    }

    @Override
    public RecordSet findBorqsIdFromContactInfo(Context ctx, RecordSet in_contact) {
        final String METHOD = "findBorqsIdFromContactInfo";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, in_contact);
        if (in_contact.size() > 0) {
            for (Record rec : in_contact) {
                String user_id_by_email = "";
                String user_id_by_phone = "";
                String user_id_by_name = "";
                String real_user_id = "";
                if (rec.has("email")) {

                    user_id_by_email = GlobalLogics.getAccount().getUserIdsByNames(ctx, rec.getString("email")).getFirstRecord().getString("user_id");

                }
                if (StringUtils.isBlank(user_id_by_email) && rec.has("phone")) {

                    user_id_by_phone = GlobalLogics.getAccount().getUserIdsByNames(ctx, rec.getString("phone")).getFirstRecord().getString("user_id");

                }
                /*if (StringUtils.isBlank(user_id_by_email) && StringUtils.isBlank(user_id_by_phone) && rec.has("name")) {

                    RecordSet tmp = GlobalLogics.getAccount().getUserIdsByNames(ctx, rec.getString("name"));
                    if (tmp.size() == 1) {
                        user_id_by_name = tmp.getFirstRecord().getString("user_id");
                    }

                }*/
                // -------------------I watched for 10 minutes and not understand it ,so I rewrite it ,forgive me-------------

                //以上已经确定是不是返回了，如果返回，只能返回唯一的一个ID
                //1,
                /*if (!user_id_by_email.equals("") && !user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_phone) && user_id_by_email.equals(user_id_by_name))
                        real_user_id = user_id_by_email;
                }
                //2,
                if (!user_id_by_email.equals("") && !user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_phone))
                        real_user_id = user_id_by_email;
                }
                //3,
                if (!user_id_by_email.equals("") && user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_email.equals(user_id_by_name))
                        real_user_id = user_id_by_email;
                }
                //4,
                if (user_id_by_email.equals("") && !user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    if (user_id_by_phone.equals(user_id_by_name))
                        real_user_id = user_id_by_phone;
                }
                //5,
                if (!user_id_by_email.equals("") && user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    real_user_id = user_id_by_email;
                }
                //6,
                if (user_id_by_email.equals("") && !user_id_by_phone.equals("") && user_id_by_name.equals("")) {
                    real_user_id = user_id_by_phone;
                }
                //7,
                if (user_id_by_email.equals("") && user_id_by_phone.equals("") && !user_id_by_name.equals("")) {
                    real_user_id = user_id_by_name;
                }*/

                // This is the new one , smart and clean
                real_user_id = user_id_by_email.equals("")?(user_id_by_phone.equals("")?user_id_by_name:user_id_by_phone):user_id_by_email;

                if (!real_user_id.equals("")) {
                    Record u = GlobalLogics.getAccount().getUsersBaseColumns(ctx, real_user_id).getFirstRecord();
                    rec.put("user_id", real_user_id);
                    rec.put("display_name", u.getString("display_name"));
                    rec.put("image_url", u.getString("image_url"));
                    rec.put("perhaps_name", u.getString("perhaps_name"));
//                    rec.put("address", u.getString("address"));
                }
            }
        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return in_contact;
    }

    public Record findMyAllPhoneBookP(Context ctx, String userId, String updateInfo) {
        final String METHOD = "findMyAllPhoneBookP";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, userId, updateInfo);
        try {
            if (updateInfo.length() > 0) {
                updateInfo = new String(Encoders.fromBase64(updateInfo));
                RecordSet recs = RecordSet.fromJson(updateInfo);

                for (Record rec : recs) {
                    //parse user info from upload
                    String username = rec.getString("username");
                    int type = (int) rec.getInt("type");
                    String content = rec.getString("content");
                    //if in user info
                    String user_id = GlobalLogics.getAccount().findUserIdByUserName(ctx, content);
                    //if exist ,add friend,insert into db

                    try {
                        createSocialContacts(ctx, userId, username, type, content, String.valueOf(user_id));
                    } catch (Exception e) {
                        L.debug(ctx, "createSocialContacts error,userId=:" + userId + ",username=" + username + ",type=" + type + ",content=" + content + ",user_id=" + user_id);
                    }
                }
            }
            RecordSet recs_all = getSocialContacts(ctx, userId, 0, 0, 1000);
            RecordSet out1 = new RecordSet();
            RecordSet out2 = new RecordSet();
            for (Record rec : recs_all) {
                if (!rec.getString("uid").equals("") && !rec.getString("uid").equals("0")) {
                    out1.add(rec);
                } else {
                    out2.add(rec);
                }
            }
            String uids = out1.joinColumnValues("uid", "");
            RecordSet users = GlobalLogics.getAccount().getUsers(ctx, userId, uids, Constants.USER_COLUMNS_SHAK);

            Record out_all = new Record();
            out_all.put("in_borqs", users);
            out_all.put("social_contacts", out2);
            return out_all;
        } catch (Exception e) {
            L.debug(ctx, "==upload error==" + e);
            return null;
        } finally {
            if (L.isTraceEnabled())
                L.traceEndCall(ctx, METHOD);
        }
    }
}
