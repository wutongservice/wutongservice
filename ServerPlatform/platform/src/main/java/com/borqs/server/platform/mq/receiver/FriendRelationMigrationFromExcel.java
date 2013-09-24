package com.borqs.server.platform.mq.receiver;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.platform.excel.JExcelUtils;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FriendRelationMigrationFromExcel {

    private static Platform p;
    public static Configuration conf;
    private ConnectionFactory connectionFactory;
    //private String db = "mysql/localhost/test_account2/root/1234";
    private String db = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
    private SQLExecutor sqlExecutor;
    private BidiMap map0 = new DualHashBidiMap();
    private Connection connection;

    public FriendRelationMigrationFromExcel() throws SQLException {

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("friendship.simple.connectionFactory", "dbcp"));
        connection = connectionFactory.getConnection(db);
        this.sqlExecutor = new SQLExecutor(connectionFactory, db);
    }

    private boolean migrationFromExcel(Map<String, String> map) throws Exception {


        //---------test map---------
        //Map<String, String> map = new HashMap<String, String>();
        //map0.put("42", "5");
        map0 = orderMapByStreams(map);

        boolean b = migrateFriendRelation(map0);
        return b;
    }

    private boolean migrateFriendRelation(Map<String, String> map) throws AvroRemoteException {
        boolean b = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String sqlFrom = "select * from friend where user=" + entry.getValue();
            String sqlTo = "select * from friend where user=" + entry.getKey();
            RecordSet rsFrom = sqlExecutor.executeRecordSet(sqlFrom, null);
            RecordSet rsTo = sqlExecutor.executeRecordSet(sqlTo, null);

            //RecordSet rsFrom = p.getFriends0(entry.getValue(), Integer.toString(Constants.FRIENDS_CIRCLE), 0, 100000);
            //RecordSet rsTo = p.getFriends0(entry.getKey(), Integer.toString(Constants.FRIENDS_CIRCLE), 0, 100000);
            RecordSet rs = mergeFriend(rsFrom, rsTo);
            b = saveFriend(entry.getKey(), rs);
            if (!b) {
                System.out.println("error data!=----------------------------");
                System.out.println(JsonHelper.toJson(rs, false));
                System.out.println("error data!=----------------------------");
                break;
            }
        }
        return b;

    }

    // compare and merge the recordSet
    private RecordSet mergeFriend(RecordSet from, RecordSet to) {

        Map<List<String>, Record> mapFrom = record2Hash(from);
        Map<List<String>, Record> mapTo = record2Hash(to);
        RecordSet rs = new RecordSet();
        for (Map.Entry<List<String>, Record> entry : mapFrom.entrySet()) {
            if (!mapTo.containsKey(entry.getKey()))
                rs.add(entry.getValue());
        }

        return rs;
    }

    private Map<List<String>, Record> record2Hash(RecordSet rs) {
        if (rs == null || rs.size() < 1)
            return new HashMap<List<String>, Record>();

        Map<List<String>, Record> map = new HashMap<List<String>, Record>();


        for (Record r : rs) {
            List<String> list = new ArrayList<String>();
            list.add(r.getString("friend"));
            list.add(r.getString("circle"));

            Record record = new Record();
            record.put("friend", r.getString("friend"));
            record.put("circle", r.getString("circle"));
            record.put("type", r.getString("type"));
            record.put("created_time", r.getString("created_time"));
            record.put("reason", r.getString("reason"));
            record.put("name", r.getString("name"));
            record.put("content", r.getString("content"));
            map.put(list, record);
        }
        return map;
    }

    private boolean saveFriend(String user_id, RecordSet rs) {
        List<String> listSql = new ArrayList<String>();
        for (Record r : rs) {
            //INSERT INTO friend VALUES(1,100001,6,12222222,23,3,'test','content');
            String sql = "insert into friend values(" + user_id + ","
                    + r.getString("friend") + "," + r.getString("circle") + ","
                    + r.getString("created_time") + "," + r.getString("reason") + ","
                    + r.getString("type") + ",'" + r.getString("name") + "','"
                    + r.getString("content") + "')";
            listSql.add(sql);
        }

        long l = sqlExecutor.executeUpdate(listSql);
        return l == rs.size();
    }

    private BidiMap orderMapByStreams(Map<String, String> map) throws AvroRemoteException {
        BidiMap map0 = new DualHashBidiMap();
        for (Map.Entry<String, String> entry : map.entrySet()) {

            long num0 = sqlExecutor.executeInt(connection, "select count(*) from stream where source=" + entry.getKey(), -1);
            long num1 = sqlExecutor.executeInt(connection, "select count(*) from stream where source=" + entry.getValue(), -1);
            if (num0 >= num1)
                map0.put(entry.getKey(), entry.getValue());
            else
                map0.put(entry.getValue(), entry.getKey());


        }
        return map0;
    }

    private void checkAndMergeFriend() throws SQLException {
        List<String> logList = new ArrayList<String>();
        long num0 = sqlExecutor.executeInt(connection, "select count(*) from friend ", -1);

        String sql = "select * from friend ";
        RecordSet rs = sqlExecutor.executeRecordSet(sql, null);

        for (Record r : rs) {
            String friend = r.getString("friend");
            if (map0.containsValue(friend)) {
                //数据表中存在垃圾账号，首先按照映射执行更新，如果更新不成功，删掉
                String sqlQ = "select count(*) from friend where user ="
                        + r.getString("user") + " and friend = "
                        + map0.getKey(friend) + " and circle="
                        + r.getString("circle");
                long num = sqlExecutor.executeInt(connection, sqlQ, -1);

                if (num > 0) {
                    String sqlD = "delete from friend where user ="
                                                + r.getString("user") + " and friend = "
                                                + map0.getKey(friend) + " and circle="
                                                + r.getString("circle");
                    sqlExecutor.executeUpdate(sqlD);
                    logList.add(sqlD);

                }
            }
        }
        System.out.print("_------------删除的冗余数据------------"+logList+"------------------------");
    }

    public static void main(String[] args) throws Exception {
        FriendRelationMigrationFromExcel f = new FriendRelationMigrationFromExcel();
        File excel = new File("/home/wutong/old_new_ids.xls");
        if (!excel.isFile())
            throw new Exception("excel file load error!");

        JExcelUtils jxl = new JExcelUtils();

        Map<String, String> map = jxl.readMapFromExcel(excel, 0);
        f.migrationFromExcel(map);

        f.checkAndMergeFriend();
        f.destroyed();
    }

    private void destroyed() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

    static {
        GenericTransceiverFactory tf = new GenericTransceiverFactory();
        //String confPath = "/home/wutong/work2/dist/etc/test_web_server.properties";
        //String confPath = "F:\\work\\old2\\account2\\src\\main\\java\\com\\borqs\\server\\com\\borqs\\server\\paltfrom\\test\\PlatformWebServer.properties";
        String confPath = "/home/wutong/work2/dist/etc/test_web_server.properties";

        conf = Configuration.loadFiles(confPath).expandMacros();
        tf.setConfig(conf);
        tf.init();
        p = new Platform(tf);
        p.setConfig(conf);
    }
}
