package com.borqs.server.platform.mq.receiver;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.platform.excel.JExcelUtils;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;


public class UpdateUserNameFromExcel {

    private static Platform p;
    public static Configuration conf;
    private ConnectionFactory connectionFactory;
    //private String db = "mysql/localhost/test_account2/root/1234";
    private String db = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
    private SQLExecutor sqlExecutor;
    private BidiMap map0 = new DualHashBidiMap();
    private Connection connection;

    public UpdateUserNameFromExcel() throws SQLException {

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("friendship.simple.connectionFactory", "dbcp"));
        connection = connectionFactory.getConnection(db);
        this.sqlExecutor = new SQLExecutor(connectionFactory, db);
    }








    private void reName(Map<String, String> map,Map<String,String>mapName) throws AvroRemoteException {

        for (Map.Entry<String, String> entry : map.entrySet()) {

            Record r = sqlExecutor.executeRecord(connection, "SELECT user_id, login_email1,login_email2,login_email3,display_name FROM user2 WHERE destroyed_time=0 and  user_id =" + entry.getKey(),null);
            if(r.size()==0){
                r = sqlExecutor.executeRecord(connection, "SELECT user_id, login_email1,login_email2,login_email3,display_name FROM user2 WHERE destroyed_time=0 and  user_id =" + entry.getValue(), null);
            }
            chargeAndUpdate(r,mapName);

        }
    }

    private void chargeAndUpdate(Record r,Map<String,String> mapName) throws AvroRemoteException {

        if(r.size()==0)
            return;


        for(String s:r.getColumns()){
            String login_email = r.getString(s);
            if(mapName.containsKey(login_email)){
                if(!r.getString("display_name").equals(mapName.get(login_email))){
                    Record userName = new Record();
                    userName.set("display_name",mapName.get(login_email));
                    p.updateAccount(r.getString("user_id"),userName);
                    System.out.println("-------------------更新名字"+r.getString("user_id")+"---------------");
                    System.out.println(userName.toString());

                    break;
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        UpdateUserNameFromExcel f = new UpdateUserNameFromExcel();
        File excel = new File("/home/wutong/old_new_ids.xls");
        //File excel = new File("D:/old_new_ids.xls");
        if (!excel.isFile())
            throw new Exception("excel file load error!");

        //File excelName = new File("F:/Directory_Phone1.xls");
        File excelName = new File("/home/wutong/Directory_Phone1.xls");
        if (!excelName.isFile())
            throw new Exception("excelName file load error!");

        JExcelUtils jxl = new JExcelUtils();

        Map<String, String> map = jxl.readMapFromExcel(excel, 0);

        Map<String,String> mapName = jxl.readMapFromExcel(excelName,0);
        System.out.println("----------------map from excel start------------------");
        System.out.println("----------------"+map.toString()+"------------------");
        System.out.println("----------------map from excel end------------------");

        f.reName(map,mapName);

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
