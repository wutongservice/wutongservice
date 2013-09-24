package com.borqs.server.migrate.platform;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.io.VfsUtils;
import com.borqs.server.base.migrate.Migrate;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.SQLException2;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLStatementHandler;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.platform.Constants;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

public class UserMigration extends Migration {
    // old: users
    /*
        +--------------------+---------------+------+-----+---------+----------------+
        | Field              | Type          | Null | Key | Default | Extra          |
        +--------------------+---------------+------+-----+---------+----------------+
        | id                 | bigint(20)    | NO   | PRI | NULL    | auto_increment |
        | name               | varchar(255)  | NO   | MUL | NULL    |                |
        | password           | varchar(255)  | NO   |     | NULL    |                |
        | nick_name          | varchar(255)  | NO   |     | NULL    |                |
        | date_of_birth      | bigint(20)    | YES  |     | NULL    |                |
        | company            | varchar(255)  | YES  |     | NULL    |                |
        | province           | int(11)       | YES  |     | NULL    |                |
        | city               | int(11)       | YES  |     | NULL    |                |
        | created_at         | bigint(20)    | NO   |     | NULL    |                |
        | last_visit_time    | bigint(20)    | YES  |     | NULL    |                |
        | verify_code        | int(11)       | YES  |     | NULL    |                |
        | verified           | int(11)       | NO   |     | 0       |                |
        | domain             | varchar(255)  | NO   | UNI | NULL    |                |
        | profile_image_url  | varchar(255)  | YES  |     | NULL    |                |
        | profile_simage_url | varchar(255)  | YES  |     | NULL    |                |
        | profile_limage_url | varchar(255)  | YES  |     | NULL    |                |
        | location           | varchar(255)  | YES  |     | NULL    |                |
        | url                | varchar(255)  | YES  |     | NULL    |                |
        | status             | varchar(2048) | YES  |     | NULL    |                |
        | longitude          | varchar(20)   | YES  |     | NULL    |                |
        | latitude           | varchar(20)   | YES  |     | NULL    |                |
        | phone_number       | varchar(20)   | YES  |     | NULL    |                |
        | status_time        | bigint(20)    | YES  |     | NULL    |                |
        | email              | varchar(255)  | YES  |     | NULL    |                |
        +--------------------+---------------+------+-----+---------+----------------+
     */

    // old: basic_info
    /*
        +--------------------+---------------+------+-----+---------+-------+
        | Field              | Type          | Null | Key | Default | Extra |
        +--------------------+---------------+------+-----+---------+-------+
        | uid                | bigint(20)    | NO   | PRI | NULL    |       |
        | gender             | tinyint(1)    | YES  |     | 1       |       |
        | marriage_status    | int(11)       | YES  |     | NULL    |       |
        | interest           | varchar(255)  | YES  |     | NULL    |       |
        | activities         | varchar(255)  | YES  |     | NULL    |       |
        | religious          | varchar(255)  | YES  |     | NULL    |       |
        | favorite_quotation | varchar(2048) | YES  |     | NULL    |       |
        | description        | varchar(2048) | YES  |     | NULL    |       |
        +--------------------+---------------+------+-----+---------+-------+

     */


    // new: user
    /*
        +--------------------------------+--------------+------+-----+---------+-------+
        | Field                          | Type         | Null | Key | Default | Extra |
        +--------------------------------+--------------+------+-----+---------+-------+
        | user_id                        | bigint(20)   | NO   | PRI | NULL    |       |
        | password                       | varchar(32)  | NO   |     | NULL    |       |
        | login_email1                   | varchar(64)  | YES  | MUL |         |       |
        | login_email2                   | varchar(64)  | YES  | MUL |         |       |
        | login_email3                   | varchar(64)  | YES  | MUL |         |       |
        | login_phone1                   | varchar(32)  | YES  | MUL |         |       |
        | login_phone2                   | varchar(32)  | YES  | MUL |         |       |
        | login_phone3                   | varchar(32)  | YES  | MUL |         |       |
        | domain_name                    | varchar(32)  | YES  |     |         |       |
        | display_name                   | varchar(32)  | YES  | MUL |         |       |
        | created_time                   | bigint(20)   | NO   |     | NULL    |       |
        | last_visited_time              | bigint(20)   | YES  |     | 0       |       |
        | image_url                      | varchar(512) | YES  |     |         |       |
        | small_image_url                | varchar(512) | YES  |     |         |       |
        | large_image_url                | varchar(512) | YES  |     |         |       |
        | basic_updated_time             | bigint(20)   | YES  |     | 0       |       |
        | status                         | varchar(256) | YES  |     |         |       |
        | status_updated_time            | bigint(20)   | YES  |     | 0       |       |
        | first_name                     | varchar(32)  | YES  |     |         |       |
        | middle_name                    | varchar(32)  | YES  |     |         |       |
        | last_name                      | varchar(32)  | YES  |     |         |       |
        | gender                         | char(1)      | YES  |     | m       |       |
        | birthday                       | varchar(32)  | YES  |     |         |       |
        | timezone                       | varchar(32)  | YES  |     |         |       |
        | interests                      | varchar(256) | YES  |     |         |       |
        | languages                      | varchar(256) | YES  |     | []      |       |
        | marriage                       | char(1)      | YES  |     | n       |       |
        | religion                       | varchar(32)  | YES  |     |         |       |
        | about_me                       | varchar(512) | YES  |     |         |       |
        | profile_updated_time           | bigint(20)   | YES  |     | 0       |       |
        | company                        | varchar(64)  | YES  |     |         |       |
        | department                     | varchar(64)  | YES  |     |         |       |
        | job_title                      | varchar(32)  | YES  |     |         |       |
        | office_address                 | varchar(64)  | YES  |     |         |       |
        | profession                     | varchar(32)  | YES  |     |         |       |
        | job_description                | varchar(256) | YES  |     |         |       |
        | business_updated_time          | bigint(20)   | YES  |     | 0       |       |
        | contact_info                   | text         | YES  |     | NULL    |       |
        | contact_info_updated_time      | bigint(20)   | YES  |     | 0       |       |
        | family                         | text         | YES  |     | NULL    |       |
        | coworker                       | text         | YES  |     | NULL    |       |
        | address                        | text         | YES  |     | NULL    |       |
        | address_updated_time           | bigint(20)   | YES  |     | 0       |       |
        | work_history                   | text         | YES  |     | NULL    |       |
        | work_history_updated_time      | bigint(20)   | YES  |     | 0       |       |
        | education_history              | text         | YES  |     | NULL    |       |
        | education_history_updated_time | bigint(20)   | YES  |     | 0       |       |
        | miscellaneous                  | text         | YES  |     | NULL    |       |
        | destroyed_time                 | bigint(20)   | YES  |     | 0       |       |
        +--------------------------------+--------------+------+-----+---------+-------+

     */


    @Override
    public void migrate() {
        user(getOldPlatformDb(), getNewPlatformDb());
        userImages(getNewPlatformDb(), getOldQiupuDataDir(), getNewPlatformImageDir());
    }

    public static void user(final String inPlatformDb, final String outPlatformDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM user");
        SQLExecutor.executeStatement(getConnectionFactory(), inPlatformDb, new SQLStatementHandler() {
            @Override
            public void handle(Statement stmt) throws SQLException {
                final Statement stmt0 = stmt;
                final HashSet<String> emails = new HashSet<String>();
                final HashSet<String> phones = new HashSet<String>();
                Migrate.migrate(
                        new SQLInput(getConnectionFactory(), inPlatformDb, "SELECT * FROM users"),
                        new SQLInsertOutput(getConnectionFactory(), outPlatformDb, "user"),
                        new CounterMigrateHandler() {
                            @Override
                            protected void handle0(Record in, Record[] out) throws MigrateStopException {
                                Record out1 = out[0];

                                long oldUserId = in.checkGetInt("id");
                                Record basicInfo;
                                try {
                                    basicInfo = SQLExecutor.executeRecord(stmt0, String.format("SELECT * FROM basic_info WHERE uid=%s", oldUserId), null);
                                } catch (SQLException e) {
                                    throw new SQLException2(e);
                                }


                                String email = in.getString("email", "");
                                String phone = in.getString("phone_number", "");
                                if (!email.isEmpty() && !emails.contains(email)) {
                                    emails.add(email);
                                } else {
                                    email = "";
                                }

                                if (!phone.isEmpty() && !phones.contains(phone)) {
                                    phones.add(phone);
                                } else {
                                    phone = "";
                                }

                                long now = DateUtils.nowMillis();
                                out1.put("user_id", oldUserId);
                                out1.put("password", in.checkGetString("password"));
                                out1.put("login_email1", email);
                                out1.put("login_email2", "");
                                out1.put("login_email3", "");
                                out1.put("login_phone1", phone);
                                out1.put("login_phone2", "");
                                out1.put("login_phone3", "");
                                out1.put("domain_name", Long.toString(in.checkGetInt("id")));
                                out1.put("display_name", in.checkGetString("nick_name"));
                                out1.put("created_time", in.checkGetInt("created_at"));
                                out1.put("last_visited_time", 0);
                                out1.put("image_url", in.checkGetString("profile_image_url"));
                                out1.put("small_image_url", in.checkGetString("profile_simage_url"));
                                out1.put("large_image_url", in.checkGetString("profile_limage_url"));
                                out1.put("status", in.checkGetString("status"));
                                out1.put("status_updated_time", in.checkGetInt("status_time"));
                                out1.put("first_name", "");
                                out1.put("middle_name", "");
                                out1.put("last_name", "");
                                out1.put("gender", basicInfo.getInt("gender", 1) == 1 ? "m" : "f");
                                out1.put("birthday", convertBirthday(in.checkGetInt("date_of_birth")));
                                out1.put("timezone", "");
                                out1.put("interests", basicInfo.getString("interest", ""));
                                out1.put("languages", "[]");
                                out1.put("marriage", Constants.MARRIAGE_UNKNOWN);
                                out1.put("religion", "");
                                out1.put("about_me", "");
                                out1.put("profile_updated_time", 0);
                                out1.put("company", in.checkGetString("company"));
                                out1.put("department", "");
                                out1.put("job_title", "");
                                out1.put("office_address", "");
                                out1.put("profession", "");
                                out1.put("job_description", "");
                                out1.put("business_updated_time", 0);
                                out1.put("contact_info", makeContactInfo(email, phone));
                                out1.put("contact_info_updated_time", now);
                                out1.put("family", "[]");
                                out1.put("coworker", "[]");
                                out1.put("address", makeAddress(in.checkGetString("location")));
                                out1.put("address_updated_time", now);
                                out1.put("work_history", "[]");
                                out1.put("work_history_updated_time", 0);
                                out1.put("education_history", "[]");
                                out1.put("miscellaneous", "[]");
                                out1.put("destroyed_time", 0);
                            }
                        });
            }
        });

    }

    public static void userImages(String outPlatformDb, String inDataDir, String outImageDir) {
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), outPlatformDb, "SELECT user_id, image_url, small_image_url, large_image_url FROM user", null);
        for (Record rec : recs) {
            long userId = rec.checkGetInt("user_id");
            String inImageUrl = trimPath(rec.checkGetString("image_url"));
            String inSmallImageUrl = trimPath(rec.checkGetString("small_image_url"));
            String inLargeImageUrl = trimPath(rec.checkGetString("large_image_url"));

            if (StringUtils.isNotBlank(inImageUrl) && StringUtils.isNotBlank(inSmallImageUrl) && StringUtils.isNotBlank(inLargeImageUrl)) {
                long now = DateUtils.nowMillis();
                String outImageUrl = "profile_" + userId + "_" + now + "_M.jpg";
                String outSmallImageUrl = "profile_" + userId + "_" + now + "_S.jpg";
                String outLargeImageUrl = "profile_" + userId + "_" + now + "_L.jpg";

                System.out.println(inDataDir + inImageUrl + " => " + outImageDir + "/" + outImageUrl);
                System.out.println(inDataDir + inSmallImageUrl + " => " + outImageDir + "/" + outSmallImageUrl);
                System.out.println(inDataDir + inLargeImageUrl + " => " + outImageDir + "/" + outLargeImageUrl);

                String sql = makeUpdateImageUrl(userId, outImageUrl, outSmallImageUrl, outLargeImageUrl);
                System.out.println(sql);
                VfsUtils.copyFile(inDataDir + inImageUrl, outImageDir + "/" + outImageUrl, false);
                VfsUtils.copyFile(inDataDir + inSmallImageUrl, outImageDir + "/" + outSmallImageUrl, false);
                VfsUtils.copyFile(inDataDir + inLargeImageUrl, outImageDir + "/" + outLargeImageUrl, false);
                SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, sql);
            }
        }

    }

    private static String makeUpdateImageUrl(long userId, String imageUrl, String smallImageUrl, String largeImageUrl) {
        return String.format("UPDATE user SET image_url='%s', small_image_url='%s', large_image_url='%s' WHERE user_id=%s", imageUrl, smallImageUrl, largeImageUrl, userId);
    }

    private static String convertBirthday(long ts) {
        return ts != 0 ? DateUtils.formatDate(ts) : "";
    }

    private static String makeContactInfo(String email, String phone) {
        Record rec = new Record();
        rec.putIf("mobile_telephone_number", phone, StringUtils.isNotBlank(phone));
        rec.putIf("email_address", email, StringUtils.isNotBlank(email));
        return rec.toString(true, false);
    }

    private static String makeAddress(String location) {
        if (StringUtils.isBlank(location)) {
            return "[]";
        } else {
            Record rec = Record.of(new Object[][]{
                    {"type", "home"},
                    {"country", ""},
                    {"state", ""},
                    {"city", ""},
                    {"street", location},
                    {"postal_code", ""},
                    {"po_box", ""},
                    {"extended_address", ""},
            });
            RecordSet recs = new RecordSet();
            recs.add(rec);
            return recs.toString(false, false);
        }
    }
}
