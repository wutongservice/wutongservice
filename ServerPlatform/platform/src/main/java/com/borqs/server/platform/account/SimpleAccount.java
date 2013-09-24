package com.borqs.server.platform.account;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.platform.ErrorCode;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SimpleAccount extends AccountBase {

    private ConnectionFactory connectionFactory;
    private String db;
    private String userTable;
    private String ticketTable;
    private String privacyTable;
    private String globalCounterTable;

    public SimpleAccount() {
    }

    @Override
    public void init() {
        super.init();

        Configuration conf = getConfig();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.userTable = conf.getString("account.simple.userTable", "user");
        this.privacyTable = conf.getString("account.simple.privacyTable", "privacy");
        this.ticketTable = conf.getString("account.simple.ticketTable", "ticket");
        this.globalCounterTable = conf.getString("account.simple.globalCounterTable", "user_id_counter");
    }

    @Override
    public void destroy() {
        this.userTable = this.ticketTable = this.globalCounterTable = null;
        this.privacyTable = null;
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;

        super.destroy();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    @Override
    protected Record findUserByLoginName(String name, String... cols) {
        Schemas.checkSchemaIncludeColumns(userSchema, cols);

        final String SQL = "SELECT ${as_join(alias, cols)}"
                + " FROM ${table}"
                + " WHERE (CAST(${alias.user_id} AS CHAR)=${v(name)}) OR (${alias.login_email1}=${v(name)} OR ${alias.login_email2}=${v(name)} OR ${alias.login_email3}=${v(name)}"
                + " OR ${alias.login_phone1}=${v(name)} OR ${alias.login_phone2}=${v(name)} OR ${alias.login_phone3}=${v(name)})"
                + " AND destroyed_time=0";

        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "cols", cols,
                "name", name);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(userSchema, rec);
        return rec;
    }

    @Override
    protected Record findUserByLoginNameNotInID(String name, String... cols) {
        Schemas.checkSchemaIncludeColumns(userSchema, cols);

        final String SQL = "SELECT ${as_join(alias, cols)}"
                + " FROM ${table}"
                + " WHERE (${alias.login_email1}=${v(name)} OR ${alias.login_email2}=${v(name)} OR ${alias.login_email3}=${v(name)}"
                + " OR ${alias.login_phone1}=${v(name)} OR ${alias.login_phone2}=${v(name)} OR ${alias.login_phone3}=${v(name)})"
                + " AND destroyed_time=0";

        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "cols", cols,
                "name", name);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(userSchema, rec);
        return rec;
    }

    @Override
    protected boolean saveTicket(String ticket, String userId, String appId) {
        final String SQL = "INSERT INTO ${table}"
                + " (${alias.ticket}, ${alias.user}, ${alias.app}, ${alias.created_time})"
                + " VALUES"
                + " (${v(ticket)}, ${v(user_id)}, ${v(app_id)}, ${v(created_time)})";


        String sql = SQLTemplate.merge(SQL, new Object[][]{
                {"table", ticketTable},
                {"alias", ticketSchema.getAllAliases()},
                {"ticket", ticket},
                {"user_id", Long.parseLong(userId)},
                {"app_id", Integer.parseInt(appId)},
                {"created_time", DateUtils.nowMillis()},
        });

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean deleteTicket(String ticket) {
        final String SQL = "DELETE FROM ${table} WHERE ${alias.ticket}=${v(ticket)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "ticket", ticket);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected Record findUserByTicket(String ticket, String... cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE ${alias.ticket}=${v(ticket)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "ticket", ticket,
                "cols", cols);

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        Schemas.standardize(ticketSchema, rec);
        return rec;
    }

    @Override
    protected RecordSet findTicketsByUserId(String userId, String appId, String... cols) {
        final String SQL = "SELECT ${as_join(alias)} FROM ${table} WHERE ${alias.user}=${v(user_id)}";
        String sql = SQLTemplate.merge(SQL,
                "alias", ticketSchema.getAllAliases(),
                "table", ticketTable,
                "user_id", userId);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(ticketSchema, recs);
        return recs;
    }
    
    @Override
    protected RecordSet findUidByMiscellaneous0(String miscellaneous) {
        final String SQL = "SELECT ${alias.user_id} FROM ${table} WHERE destroyed_time = 0 AND ${alias.miscellaneous} like '%"+miscellaneous+"%'";

        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "table", userTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;
    } 
    
    @Override
    protected String findUserIdByUserName0(String username) {
        final String SQL = "SELECT ${alias.user_id} FROM ${table} WHERE ${alias.login_email1} like '%"+ username +"%' or ${alias.login_email2} like '%"+ username +"%' "
                + " or ${alias.login_email3} like '%"+ username +"%' or ${alias.login_phone1} like '%"+ username +"%' or ${alias.login_phone2} like '%"+ username +"%' "
                + " or ${alias.login_phone3} like '%"+ username +"%'";
        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "table", userTable);

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs.size()<=0?"0":recs.getFirstRecord().getString("user_id");
    }
    
    @Override
    protected RecordSet searchUserByUserName0(String username,int page,int count) {
        final String SQL = "SELECT ${alias.user_id} FROM ${table} WHERE ${alias.login_email1} like '%"+ username +"%' or ${alias.login_email2} like '%"+ username +"%'"
                + " or ${alias.login_email3} like '%"+ username +"%' or ${alias.login_phone1} like '%"+ username +"%' or ${alias.login_phone2} like '%"+ username +"%' "
                + " or ${alias.login_phone3} like '%"+ username +"%' or ${alias.display_name} like '%"+ username +"%' ${limit}";
        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "table", userTable,
                "limit", SQLUtils.pageToLimit(page, count));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }
    
    @Override
    protected String generateUserId() {
        final String SQL1 = "INSERT INTO ${table} (key_, count_) VALUES ('user', 10000)"
                + " ON DUPLICATE KEY UPDATE count_ = count_ + 1";

        final String SQL2 = "SELECT count_ FROM ${table} WHERE key_ = 'user'";

        String sql1 = SQLTemplate.merge(SQL1, "table", globalCounterTable);
        String sql2 = SQLTemplate.merge(SQL2, "table", globalCounterTable);

        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql1);
        Record rec = se.executeRecord(sql2, null);
        long count = rec.getInt("count_", 0L);
        if (count == 0L)
            throw new AccountException(ErrorCode.GENERATE_USER_ID_ERROR, "Generate user Id error");

        return Long.toString(count);
    }

    @Override
    protected boolean saveUser(Record info) {
        Schemas.standardize(userSchema, info);

        final String SQL = "INSERT INTO ${table} ${values_join(alias, info)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "alias", userSchema.getAllAliases(),
                "info", info);

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected boolean deleteUser(String userId, long deleted_time) {
        final String SQL = "UPDATE ${table} SET destroyed_time=${v(deleted_time)} WHERE ${alias.user_id}=${v(user_id)}";
        String sql = SQLTemplate.merge(SQL,
                "table", userTable,
                "deleted_time", deleted_time,
                "alias", userSchema.getAllAliases(),
                "user_id", Long.parseLong(userId));

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);

        sql = "update suggested_user set refuse_time = "+DateUtils.nowMillis()+" where suggested in ("+userId+")";
        se.executeUpdate(sql);

        sql = "update suggested_user set refuse_time = "+DateUtils.nowMillis()+" where user in ("+userId+")";
        se.executeUpdate(sql);

        sql = "delete from suggested_user where type=10 and reason like '%"+userId+"%'";
        se.executeUpdate(sql);

        sql = "delete from request where source in ("+userId+")";
        se.executeUpdate(sql);

        sql = "update comment set destroyed_time = "+DateUtils.nowMillis()+" where commenter  in ("+userId+")";
        se.executeUpdate(sql);

        sql = "delete from like_ where liker in ("+userId+")";
        se.executeUpdate(sql);

        sql = "update social_contacts set uid=0 where uid in ("+userId+")";
        se.executeUpdate(sql);

        return n > 0;
    }

    @Override
    protected boolean updateUser(Record user) {
        Schemas.standardize(userSchema, user);

        String[] groups = userSchema.getColumnsGroups(user.getColumns());
        long now = DateUtils.nowMillis();
        long userId = user.getInt("user_id", 0);
        if (userId == 0)
            return false;

        String sql = new SQLBuilder.Update(userSchema)
                .update(userTable)
                .values(user)
                .valueIf(ArrayUtils.contains(groups, "basic"), "basic_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "status_"), "status_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "profile"), "profile_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "business"), "business_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "contact"), "contact_info_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "address_"), "address_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "work"), "work_history_updated_time", now)
                .valueIf(ArrayUtils.contains(groups, "education"), "education_history_updated_time", now)
                .where("${alias.user_id}=${v(user_id)}", "user_id", userId)
                .and("destroyed_time = 0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }
    
    protected boolean bindUser0(Record user) {
        Schemas.standardize(userSchema, user);

        long userId = user.getInt("user_id", 0);
        if (userId == 0)
            return false;

        String sql = new SQLBuilder.Update(userSchema)
                .update(userTable)
                .values(user)
                .where("${alias.user_id}=${v(user_id)}", "user_id", userId)
                .and("destroyed_time = 0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        long n = se.executeUpdate(sql);
        return n > 0;
    }

    @Override
    protected RecordSet findUsersByUserIds(List<String> userIds, String... cols) {
        final String SQL = "SELECT ${as_join(alias, cols)} FROM ${table} WHERE destroyed_time = 0 AND ${alias.user_id} IN (${user_ids})";

        String sql = SQLTemplate.merge(SQL,
                "alias", userSchema.getAllAliases(),
                "cols", cols,
                "table", userTable,
                "user_ids", StringUtils.join(userIds, ","));

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;
    }

    @Override
    protected RecordSet findUsersPasswordByUserIds(String userIds) {
        final String sql = "SELECT user_id,password FROM user WHERE destroyed_time = 0 and user_id in ("+ userIds +")";

        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        Schemas.standardize(userSchema, recs);
        return recs;
    }

    @Override
    protected boolean updatePasswordByUserId(String userId, String password) {
        final String sql = "update user set password='" + password + "' where user_id=" + userId;
        SQLExecutor se = getSqlExecutor();
        return se.executeUpdate(sql) > 0;
    }

	@Override
	protected boolean setPrivacy0(String userId, RecordSet privacyItemList) {
		String sql = "INSERT INTO " + privacyTable + " (user, resource, auths) VALUES ";
		
		for(Record privacyItem : privacyItemList)
		{
			String resource = privacyItem.getString("resource");
			String auths = privacyItem.getString("auths");
			if(StringUtils.isBlank(auths))
			{
				auths = "";
			}
			sql += "(" + userId + ", '" + resource + "', '" + auths + "'), ";
		}

		sql = StringUtils.substringBeforeLast(sql, ",");
		sql += " ON DUPLICATE KEY UPDATE auths=VALUES(auths)";
		
	    String sql2 = "DELETE FROM " + privacyTable + " WHERE auths=''"; 
		
		SQLExecutor se = getSqlExecutor();
	    long n = se.executeUpdate(sql);
	    se.executeUpdate(sql2);
	    
	    return n > 0;				
	}

	private List<String> getResourceNodesList(String resource)
	{
		List<String> l = new ArrayList<String>();
		String[] p = StringUtils2.splitArray(resource, ".", true);
		for(int i = p.length - 1; i >= 0; i--)
		{
			String temp = "";
			for(int j = 0; j <= i; j++)
			{
				temp += (p[j] + ".");
			}
			l.add(StringUtils.substringBeforeLast(temp, "."));
		}
		
		return l;
	}
	
	private String[] getResourceNodesArray(String resource)
	{				
		String[] p = StringUtils2.splitArray(resource, ".", true);
		String[] arr = new String[p.length];
		for(int i = p.length - 1; i >= 0; i--)
		{
			String temp = "";
			for(int j = 0; j <= i; j++)
			{
				temp += (p[j] + ".");
			}
			arr[i] = StringUtils.substringBeforeLast(temp, ".");
		}
		
		return arr;
	}
	
	private Record findResourceAuths(String resource, RecordSet recs, String[] nodes)
	{
		Record privacyItem = new Record();
		privacyItem.put("resource", resource);
		privacyItem.put("auths", "");
		
		for(int i = nodes.length - 1; i >= 0; i--)
		{
			for(Record rec : recs)
			{
				if(rec.getString("resource").contains(nodes[i]))
				{
					privacyItem.put("auths", rec.getString("auths"));
					return privacyItem;
				}
			}
		}
		
		return privacyItem;
	}
	
	@Override
	protected RecordSet getAuths0(String userId, List<String> resources) {
		ArrayList<String> l = new ArrayList<String>();
		for(String resource : resources)
		{
			l.addAll(getResourceNodesList(resource));
		}
		
		final String SQL = "SELECT ${alias.resource},${alias.auths} FROM ${table} WHERE ${alias.user}=${v(user)} AND ${alias.resource} IN (${vjoin(l)})";
		
		String sql = SQLTemplate.merge(SQL, new Object[][]{
				 {"table", privacyTable},
				 {"alias", privacySchema.getAllAliases()},
				 {"user", userId},
				 {"l", l}				 
		 });

	    SQLExecutor se = getSqlExecutor();
	    RecordSet recs = se.executeRecordSet(sql, null);
	    Schemas.standardize(privacySchema, recs);
	    		
		RecordSet privacyItemList = new RecordSet();
		
		for(String resource : resources)
		{
			String[] nodes = getResourceNodesArray(resource);
			Record privacyItem = findResourceAuths(resource, recs, nodes);
			privacyItemList.add(privacyItem);
		}
		
	    return privacyItemList;
	}

    @Override
    protected RecordSet getUsersAuths0(String userIds) {
        String sql = "";
        if (userIds.length() > 0)
            return new RecordSet();

        sql = "select * from privacy where user in (" + userIds + ")";
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected RecordSet findAllUserIds0(boolean all) {
        String sql = "";
        if (all) {
            sql = "select user_id,display_name from user order by user_id";
        } else {
            sql = "select user_id,display_name from user where destroyed_time=0 order by user_id";
        }
        SQLExecutor se = getSqlExecutor();
        RecordSet recs = se.executeRecordSet(sql, null);
        return recs;
    }

    @Override
    protected boolean saveShortUrl0(String long_url, String short_url) {
        if (findLongUrl0(short_url).equals("")) {
            final String sql = "INSERT INTO short_url"
                    + " (long_url,short_url)"
                    + " VALUES"
                    + " ('" + long_url + "','" + short_url + "')";
            SQLExecutor se = getSqlExecutor();
            se.executeUpdate(sql);
        }
        return true;
    }

    @Override
    protected String findLongUrl0(String short_url) {
        String sql = "select long_url from short_url where short_url='"+short_url+"'";
        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return !rec.isEmpty()?rec.getString("long_url"):"";
    }

    @Override
    public ByteBuffer getUserIdsByNames(CharSequence loginNames){
        return null;
    }

    @Override
    public CharSequence getBorqsUserIds() throws AvroRemoteException, ResponseError {
        return null;
    }
}
