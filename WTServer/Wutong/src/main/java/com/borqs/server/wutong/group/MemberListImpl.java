package com.borqs.server.wutong.group;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.PinyinUtils;
import com.borqs.server.base.util.Sort;
import com.borqs.server.wutong.WutongErrors;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

@Deprecated
public class MemberListImpl implements MemberListLogic {
    private static final Logger L = Logger.getLogger(MemberListImpl.class);

    private ConnectionFactory connectionFactory;
    private String db;
    private String memberListTable;

    public MemberListImpl() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("groupMemberList.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("platform.simple.db", null);
        this.memberListTable = conf.getString("groupMemberList.simple.groupMemberListTable", "group_member_list");    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    private String makeSK(Record rec) {
        String fid = rec.getString(COL_FID);
        String f01 = rec.getString(COL_F01);
        String f02 = rec.getString(COL_F02);
        String f03 = rec.getString(COL_F03);
        String f04 = rec.getString(COL_F04);
        String f05 = rec.getString(COL_F05);
        StringBuilder buff = new StringBuilder();
        String s = makeSKItem(fid);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        s = makeSKItem(f01);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        s = makeSKItem(f02);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        s = makeSKItem(f03);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        s = makeSKItem(f04);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        s = makeSKItem(f05);
        if (!s.isEmpty()) {
            buff.append(" ").append(s);
        }
        return buff.toString();
    }

    private String makeSKItem(String s) {
        s = StringUtils.trimToEmpty(s);
        if (s.isEmpty())
            return "";

        StringBuilder buff = new StringBuilder();
        buff.append(s);
        String s1 = PinyinUtils.toFullPinyin(s, " ");
        if (!s1.isEmpty() && !StringUtils.equals(s1, s))
            buff.append(" ").append(s1);

        String s2 = PinyinUtils.toShortPinyin(s);
        if (!s2.isEmpty() && !StringUtils.equals(s2, s) && StringUtils.equals(s2, s1))
            buff.append(" ").append(s2);

        return buff.toString();
    }

    private void updateSK(Context ctx, long groupId, String[] fids, SQLExecutor se) {
        if (ArrayUtils.isNotEmpty(fids)) {

            String selectSQL = new SQLBuilder.Select()
                    .select("fid", "f01", "f02", "f03", "f04", "f05")
                    .from(memberListTable)
                    .where("group_id=${v(gid)} AND fid IN (${vjoin(fids)})", "gid", groupId, "fids", fids)
                    .toString();
            RecordSet recs = se.executeRecordSet(selectSQL, null);

            ArrayList<String> sqls = new ArrayList<String>();
            for (Record rec : recs) {
                String sk = makeSK(rec);
                String sql = new SQLBuilder.Update()
                        .update(memberListTable)
                        .value("sk", sk)
                        .where("group_id=${v(gid)} AND fid=${v(fid)}", "gid", groupId, "fid",rec.getString("fid"))
                        .toString();
                sqls.add(sql);
            }
            se.executeUpdate(sqls);
        }
    }

    @Override
    public Record addMember(Context ctx, long groupId, Record rec) {
        String fid = rec.checkGetString(COL_FID);
        SQLBuilder.Insert sql = new SQLBuilder.Insert()
                .insertIgnoreInto(memberListTable)
                .value("group_id", groupId)
                .value(COL_FID, fid);
        for (String col : COLS) {
            sql.value(col, rec.getString(col, ""));
        }
        sql.value("sk", makeSK(rec));
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql.toString());
        return getMember(ctx, groupId, fid);
    }

    @Override
    public void deleteMembers(Context ctx, long groupId, String[] ids) {
        if (ArrayUtils.isEmpty(ids))
            return;

        String sql = new SQLBuilder.Delete()
                .deleteFrom(memberListTable)
                .where("group_id=${v(gid)} AND fid IN (${vjoin(fids)})", "fids", ids, "gid", groupId)
                .toString();
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql);
    }

    @Override
    public Record updateMember(Context ctx, long groupId, Record rec) {
        String fid = rec.checkGetString(COL_FID);
        SQLBuilder.Update sql = new SQLBuilder.Update()
                .update(memberListTable);
        for (String col : COLS) {
            sql.valueIf(rec.has(col), col, rec.getString(col, ""));
        }
        sql.where("group_id=${v(gid)} AND fid=${v(fid)}", "gid", groupId, "fid", fid);
        SQLExecutor se = getSqlExecutor();
        se.executeUpdate(sql.toString());
        updateSK(ctx, groupId, new String[]{fid}, se);
        return getMember(ctx, groupId, fid);
    }

    @Override
    public int putMembers(Context ctx, long groupId, RecordSet recs, boolean merge) {
        SQLExecutor se = getSqlExecutor();

        ArrayList<String> sqls = new ArrayList<String>();
        for (Record rec : recs) {
            String fid = rec.checkGetString(COL_FID);
            SQLBuilder.Replace sql = new SQLBuilder.Replace()
                    .replaceInto(memberListTable)
                    .value("group_id", groupId)
                    .value(COL_FID, fid);
            for (String col : COLS) {
                sql.value(col, rec.getString(col, ""));
            }
            sql.value("sk", makeSK(rec));
            sqls.add(sql.toString());
        }

        if (!merge) {
            String sql = new SQLBuilder.Delete()
                    .deleteFrom(memberListTable)
                    .where("group_id=${v(gid)}", "gid", groupId)
                    .toString();
            se.executeUpdate(sql);
        }
        se.executeUpdate(sqls);
        String countSql = new SQLBuilder.Select()
                .select("COUNT(*)")
                .from(memberListTable)
                .where("group_id=${v(gid)}", "gid", groupId)
                .toString();
        return (int)se.executeIntScalar(countSql, 0L);
    }

    @Override
    public RecordSet getMembers(Context ctx, long groupId, String sort, int page, int count) {
        Sort sort1 = Sort.parse(sort);
        String sql = new SQLBuilder.Select()
                .select("*")
                .from(memberListTable)
                .where("group_id=${v(gid)}", "gid", groupId)
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(page, count)
                .toString();
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    @Override
    public RecordSet searchMember(Context ctx, long groupId, String kw, String sort, int count) {
        Sort sort1 = Sort.parse(sort);
        String sql = new SQLBuilder.Select()
                .select("*")
                .from(memberListTable)
                .where("group_id=${v(gid)} AND sk LIKE ${v(kw)}", "gid", groupId, "kw", "%" + StringUtils.trimToEmpty(kw) + "%")
                .orderBy(sort1.orderBy, sort1.ascOrDesc)
                .page(0, count)
                .toString();
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql, null);
    }

    @Override
    public Record getMember(Context ctx, long groupId, String fid) {
        Record rec = getMemberNoThrow(ctx, groupId, fid);
        if (rec == null) {
            throw new ServerException(WutongErrors.GROUP_NOT_FOUND_MEMBER_IN_MEMBER_LIST);
        }
        return rec;
    }

    @Override
    public Record getMemberNoThrow(Context ctx, long groupId, String fid) {
        String sql = new SQLBuilder.Select()
                .select("*")
                .from(memberListTable)
                .where("group_id=${v(gid)} AND fid=${v(fid)}", "gid", groupId, "fid", fid)
                .toString();

        SQLExecutor se = getSqlExecutor();
        Record rec = se.executeRecord(sql, null);
        return MapUtils.isEmpty(rec) ? null : rec;
    }
}
