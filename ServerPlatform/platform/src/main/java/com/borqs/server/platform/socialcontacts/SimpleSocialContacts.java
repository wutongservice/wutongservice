package com.borqs.server.platform.socialcontacts;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SQLUtils;
import com.borqs.server.base.util.Encoders;

public class SimpleSocialContacts extends SocialContactsBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String SocialContactsTable;

    public SimpleSocialContacts() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("SocialContacts.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("socialcontacts.simple.db", null);
        this.SocialContactsTable = conf.getString("SocialContacts.simple.SocialContactsTable", "social_contacts");
    }

    @Override
    public void destroy() {
        this.SocialContactsTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean createSocialContacts0(String owner, String username, int type, String content, String uid) {
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
                    se.executeUpdate(sql);
                }
            }
        }
        return true;
    }

    protected boolean createSocialContacts1(String owner, String username, int type, String content, String uid) {
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

    protected boolean hasSocialContacts0(String owner, String username, String content, String uid) {
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

    protected boolean hasSocialContacts1(String owner, String username, String content) {
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
    protected RecordSet getSocialContacts0(String owner, int type, int page, int count) {
        String sql0 = "select * from social_contacts where owner='" + owner + "' AND LENGTH(content)<>32";
        if (type != 0) {
            sql0 += " and type='" + type + "' ";
        }
        sql0 += " order by username " + SQLUtils.pageToLimit(page, count);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql0.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getSocialContacts1(String owner) {
        StringBuilder sql = new StringBuilder();
        sql.append(SQLTemplate.merge("SELECT distinct(uid) FROM ${table} WHERE ${alias.owner}=${v(owner)} AND uid>0",
                "owner", owner, "table", SocialContactsTable, "alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getSocialContacts2(String duserId, String uids) {
//        StringBuilder sql = new StringBuilder();
//        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
//                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where uid>0 and owner<>" + duserId + " and uid in (" + uids + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getCommSocialContactsM2(String userId) {
//        StringBuilder sql = new StringBuilder();
//        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
//                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where owner<>" + userId + " and " +
                " uid>0 and uid in (select uid from social_contacts where uid>0 and owner=" + userId + ") and owner not in " +
                "(select friend from friend where type=0 and circle<>4 and reason<>5 and reason<>6 and user=" + userId + ") and owner not in " +
                "(select suggested from suggested_user where user=" + userId + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getCommSocialContactsU3(String userId, String friendId) {
//        StringBuilder sql = new StringBuilder();
//        sql.append(SQLTemplate.merge("SELECT distinct(owner) FROM ${table} WHERE ${alias.uid}=${v(uid)}",
//                "uid", uid, "table", SocialContactsTable,"alias", socialContactsSchema.getAllAliases()));
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(uid) from social_contacts where owner=" + userId + "" +
                " and uid>0 and uid in " +
                "(select uid from social_contacts where owner=" + friendId + " and uid>0) and uid<>" + userId + " and uid<>" + friendId + "";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getWhohasMyContacts0(String userId, String email, String phone) {
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

        return recs;
    }

    @Override
    protected RecordSet getDistinctUsername0(String uid) {
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(username) from social_contacts where uid='" + uid + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getDistinctOwner0(String uid, String username) {
        SQLExecutor se = getSqlExecutor();
        String sql = "select distinct(owner) from social_contacts where uid='" + uid + "' and username='" + username + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getUserName0(String owner, String uid) {
        SQLExecutor se = getSqlExecutor();
        String sql = "select username from social_contacts where uid='" + uid + "' and uid<>'0' and owner='" + owner + "'";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }

    @Override
    protected RecordSet getCommSocialContactsByUid0(String owner, String uid) {
        SQLExecutor se = getSqlExecutor();
        String sql = "select owner,uid from social_contacts where  OWNER <> uid AND owner<> " + owner + " and uid in(" + uid + ")";
        RecordSet recs = se.executeRecordSet(sql.toString(), null);
        return recs;
    }
}
