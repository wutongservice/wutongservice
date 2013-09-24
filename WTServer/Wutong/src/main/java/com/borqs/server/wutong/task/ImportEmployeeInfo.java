package com.borqs.server.wutong.task;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.*;
import com.borqs.server.base.util.StringUtils2;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;

public class ImportEmployeeInfo {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static void main(String[] args) throws Exception {
        String sql = "select * from employee_list";
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);

        String emails = recs.joinColumnValues("email", ",");
        String[] emailArr = StringUtils2.splitArray(emails, ",", true);
        String con1 = "login_email1 in (" + SQLUtils.valueJoin(",", emailArr) + ")";
        String con2 = "login_email2 in (" + SQLUtils.valueJoin(",", emailArr) + ")";
        String con3 = "login_email3 in (" + SQLUtils.valueJoin(",", emailArr) + ")";
        String con = con1 + " or " + con2 + " or " + con3;
        sql = "select user_id, login_email1, login_email2, login_email3 from user2 where (" + con + ") and destroyed_time=0";
        RecordSet users = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
        Record map = new Record();
        for (Record user : users) {
            long userId = user.getInt("user_id");
            String loginEmail1 = user.getString("login_email1");
            String loginEmail2 = user.getString("login_email2");
            String loginEmail3 = user.getString("login_email3");
            if (ArrayUtils.contains(emailArr, loginEmail1))
                map.put(loginEmail1, userId);
            if (ArrayUtils.contains(emailArr, loginEmail2))
                map.put(loginEmail2, userId);
            if (ArrayUtils.contains(emailArr, loginEmail3))
                map.put(loginEmail3, userId);
        }

        ArrayList<String> sqls = new ArrayList<String>();
        for (Record rec : recs) {
            String email = rec.getString("email", "");
            sql = new SQLBuilder.Replace()
                    .replaceInto("employee_info")
                    .values(new Record()
                            .set("circle_id", 10000000072L)
                            .set("user_id", map.getInt(email, 0L))
                            .set("employee_id", rec.getString("employee_id", ""))
                            .set("email", email)
                            .set("name", rec.getString("name", ""))
                            .set("name_en", rec.getString("name_en", ""))
                            .set("tel", rec.getString("tel", ""))
                            .set("mobile_tel", rec.getString("mobile_tel", ""))
                            .set("department", rec.getString("department", ""))
                            .set("job_title", rec.getString("job_title", ""))
                            .set("job_title_en", rec.getString("job_title_en", ""))
                            .set("comment", rec.getString("comment", ""))
                            .set("sk", rec.getString("sk", ""))
                    )
                    .toString();
            sqls.add(sql);
        }

        long n = SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sqls);

        System.out.println("Import " + n + " rows");
    }
}
