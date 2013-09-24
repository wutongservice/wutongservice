package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.RandomUtils;
import org.codehaus.plexus.util.StringUtils;

public class HolidayRecordImpl implements HolidayRecordLogic, Initializable {
    private static final Logger L = Logger.getLogger(HolidayRecordImpl.class);
    public final Schema holidayRecordSchema = Schema.loadClassPath(HolidayRecordImpl.class, "holidayRecord.schema");

    private ConnectionFactory connectionFactory;
    private String db;
    private String holidayRecord = "holiday_record";


    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);

        this.holidayRecord = conf.getString("tag.simple.holiday_record", "holiday_record");
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }


    public RecordSet getActionConfig(Context ctx, String scope, String name) {
        String sql0 = new SQLBuilder.Select()
                .select("name , content")
                .from(this.holidayRecord)
                .where("scope=" + scope)
                .andIf(StringUtils.isNotBlank(name), "name =${v(name)}", "name", name)
                .and("destroyed_time=0")
                .toString();

        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }


    @Override
    public Record saveHolidayRecord(Context ctx, Record holiday) {
        final String METHOD = "saveHolidayRecord";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, holiday);

        SQLExecutor se = getSqlExecutor();

        holiday.set("id", Long.toString(RandomUtils.generateId()));
        final String SQL = "INSERT INTO ${table} ${values_join(alias, holiday)}";
        String sql0 = SQLTemplate.merge(SQL,
                "table", this.holidayRecord, "alias", holidayRecordSchema.getAllAliases(),
                "holiday", holiday);

        long result = se.executeUpdate(sql0);
        L.op(ctx, "saveHolidayRecord");
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return holiday;
    }

    @Override
    public Record updateHolidayRecord(Context ctx, Record holiday) {
        final String METHOD = "updateHolidayRecord";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, holiday);


        SQLExecutor se = getSqlExecutor();

        final String SQL = new SQLBuilder.Update().update(holidayRecord)
                .valueIf(StringUtils.isNotBlank(holiday.getString("holi_status")), "holi_status", holiday.getString("holi_status"))
                .valueIf(StringUtils.isNotBlank(holiday.getString("finish_time")), "finish_time", holiday.getString("finish_time"))
                .where("relation_id=" + holiday.getString("relation_id")).toString();

        String sql0 = SQLTemplate.merge(SQL,
                "table", this.holidayRecord, "alias", holidayRecordSchema.getAllAliases(),
                "holiday", holiday);

        long result = se.executeUpdate(sql0);

        L.op(ctx, "updateHolidayRecord");
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return holiday;
    }

    @Override
    public RecordSet getTotalRecords(Context ctx, Record holiday) {
        String sql0 = new SQLBuilder.Select()
                .select(" id ,owner_id,owner,sum(days) as days")
                .from(this.holidayRecord)
                .where("1=1")
                .andIf(StringUtils.isNotBlank(holiday.getString("owner")), "owner_id in(${v(owner_id)})", "owner_id", holiday.getString("owner"))
                .andIf(StringUtils.isNotBlank(holiday.getString("approver")), "approver_id=${v(approver_id)}", "approver_id", holiday.getString("approver"))
                .andIf(StringUtils.isNotBlank(holiday.getString("holi_type")), "holi_type=${v(holi_type)}", "holi_type", holiday.getString("holi_type"))
                .andIf(StringUtils.isNotBlank(holiday.getString("dep")), "dep=${v(dep)}", "dep", holiday.getString("dep"))
                .andIf(StringUtils.isNotBlank(holiday.getString("holi_status")), "holi_status=${v(holi_status)}", "holi_status", holiday.getString("holi_status"))
                .andIf(StringUtils.isNotBlank(holiday.getString("apply_time_start")), "apply_time>=${v(apply_time_start)}", "apply_time_start", holiday.getString("apply_time_start"))
                .andIf(StringUtils.isNotBlank(holiday.getString("apply_time_end")), "apply_time<=${v(apply_time_end)}", "apply_time_end", holiday.getString("apply_time_end"))
                .and("holi_status=1")
                .toString();

        sql0 += " GROUP BY owner order by finish_time desc";
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);

    }

    @Override
    public RecordSet getDetailRecords(Context ctx, Record holiday) {
        String sql0 = new SQLBuilder.Select()
                .select(" id ,owner,owner_id,days, approver,approver_id,mate,mate_id,holi_type,holi_start_time,days,dep,apply_time,holi_status,finish_time")
                .from(this.holidayRecord)
                .where("1=1")
                .andIf(StringUtils.isNotBlank(holiday.getString("owner")), "owner_id=${v(owner_id)}", "owner_id", holiday.getString("owner"))
                .andIf(StringUtils.isNotBlank(holiday.getString("approver")), "approver_id=${v(approver_id)}", "approver_id", holiday.getString("approver"))
                .andIf(StringUtils.isNotBlank(holiday.getString("holi_type")), "holi_type=${v(holi_type)}", "holi_type", holiday.getString("holi_type"))
                .andIf(StringUtils.isNotBlank(holiday.getString("dep")), "dep=${v(dep)}", "dep", holiday.getString("dep"))
                .andIf(StringUtils.isNotBlank(holiday.getString("holi_status")), "holi_status=${v(holi_status)}", "holi_status", holiday.getString("holi_status"))
                .andIf(StringUtils.isNotBlank(holiday.getString("apply_time_start")), "apply_time>=${v(apply_time_start)}", "apply_time_start", holiday.getString("apply_time_start"))
                .andIf(StringUtils.isNotBlank(holiday.getString("apply_time_end")), "apply_time<=${v(apply_time_end)}", "apply_time_end", holiday.getString("apply_time_end"))
                .and("holi_status=1")
                .toString();

        sql0 += " order by owner, finish_time desc";
        SQLExecutor se = getSqlExecutor();
        return se.executeRecordSet(sql0, null);
    }
}
