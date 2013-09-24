package com.borqs.server.wutong.task;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.usersugg.SuggestedUserLogic;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.codehaus.jackson.JsonNode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateSuggestUser {
    public static Configuration conf;
    private ConnectionFactory connectionFactory;
    //private String db = "mysql/localhost/accounts/root/1234";
    private static String db = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";
    //private String confPath = "F:\\\\work\\\\refactProduct\\\\Dist\\\\src\\\\main\\\\etc\\\\test.config.properties";
    private static String confPath = "/home/wutong/work2/dist/etc/test_web_server.properties";
    private SQLExecutor sqlExecutor;
    private BidiMap map0 = new DualHashBidiMap();
    private Connection connection;

    public CreateSuggestUser() throws SQLException {

        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("friendship.simple.connectionFactory", "dbcp"));
        connection = connectionFactory.getConnection(db);
        this.sqlExecutor = new SQLExecutor(connectionFactory, db);
    }


    private void createSuugest() {
        Context ctx = new Context();
        Map<String, List<String>> mapUsersSchool = new HashMap<String, List<String>>();
        Map<String, List<String>> mapUsersCompany = new HashMap<String, List<String>>();
        long num = sqlExecutor.executeIntScalar("SELECT count(user_id) FROM user2 WHERE destroyed_time = 0", 0);
        int page = (int) num / 100;
        String user_id = "10000";
        for (int i = 0; i <= page; i++) {
            RecordSet rs = sqlExecutor.executeRecordSet("SELECT user_id FROM user2 WHERE destroyed_time = 0 and user_id >=" + user_id + " order by user_id limit 100", null);

            if (rs.size() == 0)
                break;

            List userIds = rs.getIntColumnValues("user_id");
            RecordSet rsUsers = GlobalLogics.getAccount().getUsers(ctx, StringUtils2.joinIgnoreBlank(",", userIds), "education_history,work_history");

            for (Record r : rsUsers) {
                List<String> listSchool = new ArrayList<String>();
                List<String> listCompany = new ArrayList<String>();
                JsonNode jn = r.toJsonNode();
                JsonNode schools = jn.get("education_history");
                List<String> schoolNames = schools.findValuesAsText("school");

                JsonNode companies = jn.get("work_history");
                List<String> companyNames = companies.findValuesAsText("company");
                if (schoolNames != null && schoolNames.size() > 0)
                    listSchool.addAll(schoolNames);
                if (companyNames != null && companyNames.size() > 0)
                    listCompany.addAll(companyNames);
                if (listSchool.size() > 0)
                    mapUsersSchool.put(r.getString("user_id"), listSchool);
                if (listCompany.size() > 0)
                    mapUsersCompany.put(r.getString("user_id"), listCompany);


                //the same school

                //the same company
                user_id = r.getString("user_id");

            }
        }

        user_id = "10000";
        // company school
        for (int i = 0; i <= page; i++) {
            RecordSet rs = sqlExecutor.executeRecordSet("SELECT user_id FROM user2 WHERE destroyed_time = 0 and user_id >=" + user_id + " limit 100", null);


            if (rs.size() == 0)
                break;

            for (Record r : rs) {
                String id = r.getString("user_id");
                RecordSet rsIgnore = sqlExecutor.executeRecordSet("SELECT distinct(friend) FROM friend WHERE user =" + id, null);
                List<String> listIgnore = rsIgnore.getStringColumnValues("friend");
                SuggestedUserLogic suggestedUserLogic = GlobalLogics.getSuggest();
                //study from address ,contact have borqsid,
                suggestedUserLogic.createSuggestUserFromHaveBorqsIdP(ctx, id);

                //study from address ,have common  lxr,
                suggestedUserLogic.createSuggestUserFromHaveCommLXRP(ctx, id);

                //study from address,for has my contactinfo
                suggestedUserLogic.createSuggestUserByHasMyContactP(ctx, id);

                //study from friend ,for common friend
                suggestedUserLogic.createSuggestUserFromCommonFriendsP(ctx, id);

                //the same school
                ctx.setViewerId(Long.parseLong(id));
                GlobalLogics.getSuggest().createSuggestUserFromSameSchoolP(ctx, id, ignoreId(listIgnore, mapUsersSchool));

                //the same company
                GlobalLogics.getSuggest().createSuggestUserFromSameCompanyP(ctx, id, ignoreId(listIgnore, mapUsersCompany));

                user_id = r.getString("user_id");

            }
        }

    }

    private Map<String, List<String>> ignoreId(List<String> ignoreIds, Map<String, List<String>> map) {
        if (map.size() < 1 || ignoreIds.size() < 1)
            return null;

        Map<String, List<String>> maps = new HashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!ignoreIds.contains(entry.getKey())) {
                maps.put(entry.getKey(), entry.getValue());
            }
        }
        return maps;
    }

    public static void main(String[] args) throws Exception {
        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }
        GlobalConfig.loadFiles(confPath);


        GlobalConfig.loadFiles(confPath);
        conf = GlobalConfig.get();
        GlobalLogics.init();
        CreateSuggestUser f = new CreateSuggestUser();


        f.createSuugest();

        f.destroyed();
    }

    private void destroyed() {
        this.connectionFactory = ConnectionFactory.close(connectionFactory);
        db = null;
    }

}
