package com.borqs.server.wutong.task;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.wutong.group.GroupImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GenerateGroupSortKey {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
    private static final String TEST_ACCOUNT_DB = "mysql/192.168.5.22/accounts/root/111111";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static void main(String[] args) throws Exception {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        String sql = "SELECT id, name FROM group_";

        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql, null);
        for (Record rec : recs) {
            map.put(rec.getString("id"), rec.getString("name"));
        }

        ArrayList<String> sqls = new ArrayList<String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String groupId = entry.getKey();
            String name = entry.getValue();
            sql = "UPDATE group_ set sort_key='" + GroupImpl.makeGroupSortKey(name) + "' WHERE id=" + groupId;
            sqls.add(sql);
        }
        long n = SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sqls);

        System.out.println("Update " + n + " rows");
    }
}
