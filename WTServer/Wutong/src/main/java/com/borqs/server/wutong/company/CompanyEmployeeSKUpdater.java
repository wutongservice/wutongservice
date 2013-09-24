package com.borqs.server.wutong.company;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.DBCPConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;

import java.util.ArrayList;

public class CompanyEmployeeSKUpdater {
    public static void main(String[] args) throws Exception {
        String db = args[0];

        ConnectionFactory cf = new DBCPConnectionFactory();
        SQLExecutor se = new SQLExecutor(cf, db);
        String sql = new SQLBuilder.Select()
                .select("*")
                .from("employee_list")
                .toString();

        final ArrayList<String> updateSqls = new ArrayList<String>();
        se.executeRecordHandler(sql, new RecordHandler() {
            @Override
            public void handle(Record rec) {
                String sk = CompanyImpl.makeEmployeeSK(
                        rec.getString("name"),
                        rec.getString("email"),
                        rec.getString("tel"),
                        rec.getString("mobile_tel"),
                        rec.getString("department")
                );
                //System.out.println(sk);
                String updateSql = new SQLBuilder.Update()
                        .update("employee_list")
                        .value("sk", sk)
                        .where("company_id=${v(company_id)} AND email=${v(email)}",
                                "company_id", rec.getInt("company_id"),
                                "email", rec.getString("email"))
                        .toString();
                updateSqls.add(updateSql);
            }
        });

        for (String updateSql : updateSqls) {
            System.out.println(updateSql);
        }
        se.executeUpdate(updateSqls);
    }
}
