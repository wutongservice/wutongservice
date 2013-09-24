package com.borqs.server.platform.suggesteduser;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.service.platform.Constants;

import static com.borqs.server.service.platform.Constants.BLOCKED_CIRCLE;
import static com.borqs.server.service.platform.Constants.FRIEND_REASON_DEFAULT_DELETE;
import static com.borqs.server.service.platform.Constants.FRIEND_REASON_SOCIALCONTACT_DELETE;

public class SimpleSuggestedUser extends SuggestedUserBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String suggestedUserTable;
    private String qiupuUid;

    public SimpleSuggestedUser() {

    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
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

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    @Override
    protected boolean create0(String toUserId, String beSuggested, int type, String reson) {
        if (toUserId.equals(beSuggested)){
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
    
    @Override
    protected boolean refuseSuggestUser0(String userId, String suggested) {
        final String SQL = "UPDATE ${table} SET ${alias.refuse_time}=${v(refuse_time)} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL, new Object[][] {
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
    
    @Override
    protected boolean updateSuggestUser0(String userId, String suggested,int type,String reason) {
        final String SQL = "UPDATE ${table} SET ${alias.type}=${v(type)},${alias.reason}=${v(reason)} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL, new Object[][] {
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
    
    @Override
    protected boolean deleteSuggestUser0(String userId, String suggested) {
        final String SQL = "delete from ${table} WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)}";
        String sql = SQLTemplate.merge(SQL,
            "table", suggestedUserTable, "alias", suggestedUserSchema.getAllAliases(), "user", userId,"suggested",suggested);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean backSuggestUser0(String userId ,long dateDiff) {
        long flagDate =  DateUtils.nowMillis()-dateDiff;
        final String sql = "update suggested_user set refuse_time=0 where refuse_time<>0 and user="+userId +" and refuse_time<="+ flagDate +"";
        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    @Override
    protected RecordSet getSuggestUser0(String userId, int limit) {
        final String sql = "select * from suggested_user where user="+userId+" and refuse_time=0 and " +
                "suggested not in (select friend from friend where user="+userId+" and type=0 and circle<>"+Constants.BLOCKED_CIRCLE+" and reason<>"+Constants.FRIEND_REASON_SOCIALCONTACT_DELETE+" and reason<>"+Constants.FRIEND_REASON_DEFAULT_DELETE+")" +
                " order by type,create_time limit "+limit+"";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

     
    @Override
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

	@Override
	protected String getWhoSuggest0(String to, String beSuggested) {
		final String SQL = "SELECT ${alias.reason} FROM ${table}"
                + " WHERE ${alias.user}=${v(user)} AND ${alias.suggested}=${v(suggested)} AND ${alias.type}=${v(type)} AND ${alias.refuse_time}=0";

        String sql = SQLTemplate.merge(SQL, new Object[][]{
                    {"alias", suggestedUserSchema.getAllAliases()},
                    {"table", suggestedUserTable},
                    {"user", to},
                    {"suggested", beSuggested},
                    {"type", Integer.valueOf(Constants.RECOMMENDER_USER)}
                    });
        SQLExecutor se = getSqlExecutor();
        Object o = se.executeScalar(sql);
        if(o == null)
        	return "";
        else
        	return String.valueOf(o);        
	}

    @Override
    protected RecordSet getSuggestFromBothFriend0(String userId) {
        final String sql = "select distinct(user) from friend where type=0 and " +
                "friend<>"+qiupuUid+" and friend in (select friend from friend where type=0 and friend<>"+qiupuUid+" and user="+userId+" AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " and user not in (select friend from friend where type=0 and friend<>"+qiupuUid+" and user="+userId+" AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " and user not in (select suggested from suggested_user where user="+userId+")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet getSuggestFromHasMyContactinfo0(String userId) {
         final String sql="select distinct(owner) from social_contacts where uid="+userId+"" +
                " and owner not in (select friend from friend where user="+userId+" and type=0 AND circle<>" + BLOCKED_CIRCLE + " and reason<>" + FRIEND_REASON_SOCIALCONTACT_DELETE + " and reason<>" + FRIEND_REASON_DEFAULT_DELETE + ")" +
                " and owner not in (select suggested from suggested_user where user="+userId+")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet getSuggestUserHistory0(String userId, int limit) {
        final String sql = "select * from suggested_user where user="+userId+" and refuse_time=0 order by type,create_time limit "+limit+"";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet getWhoSuggestedHim0(String userId, String beSuggested) {
        final String sql = "select * from suggested_user where user='"+userId+"' and suggested='"+beSuggested+"' and type=10 and reason<>'' and refuse_time=0";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(suggestedUserSchema, recs);
        return recs;
    }
}