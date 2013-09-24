package com.borqs.server.migrate.platform;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.migrate.Migrate;
import com.borqs.server.base.migrate.MigrateStopException;
import com.borqs.server.base.migrate.handler.CounterMigrateHandler;
import com.borqs.server.base.migrate.input.SQLInput;
import com.borqs.server.base.migrate.output.PrintOutput;
import com.borqs.server.base.migrate.output.SQLInsertOutput;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.migrate.Migration;
import com.borqs.server.service.platform.Constants;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.schema.JsonSchema;

public class StreamMigration extends Migration {
    // old: streams
    /*
        +-----------------+---------------+------+-----+---------+-------+
        | Field           | Type          | Null | Key | Default | Extra |
        +-----------------+---------------+------+-----+---------+-------+
        | id              | varchar(255)  | NO   | PRI | NULL    |       |
        | from_id         | bigint(20)    | NO   |     | NULL    |       |
        | to_id           | varchar(255)  | NO   |     | NULL    |       |
        | message         | varchar(2048) | YES  |     | NULL    |       |
        | filter_type     | int(11)       | YES  |     | NULL    |       |
        | attachment_type | int(11)       | YES  |     | NULL    |       |
        | attachment      | varchar(4096) | YES  |     | NULL    |       |
        | created_time    | bigint(20)    | NO   |     | NULL    |       |
        | updated_time    | bigint(20)    | NO   |     | NULL    |       |
        | application     | int(11)       | YES  |     | NULL    |       |
        | source          | varchar(255)  | YES  |     | NULL    |       |
        | privacy         | int(11)       | NO   |     | 0       |       |
        | custom_id       | varchar(255)  | YES  |     | NULL    |       |
        | parent_id       | varchar(255)  | YES  |     | NULL    |       |
        | ifdelete        | tinyint(1)    | YES  |     | 0       |       |
        +-----------------+---------------+------+-----+---------+-------+

     */

    // new: stream
    /*
        +----------------+---------------+------+-----+---------+-------+
        | Field          | Type          | Null | Key | Default | Extra |
        +----------------+---------------+------+-----+---------+-------+
        | post_id        | bigint(20)    | NO   | PRI | NULL    |       |
        | source         | bigint(20)    | NO   | MUL | NULL    |       |
        | created_time   | bigint(20)    | NO   |     | NULL    |       |
        | updated_time   | bigint(20)    | YES  |     | 0       |       |
        | destroyed_time | bigint(20)    | YES  |     | 0       |       |
        | quote          | bigint(20)    | YES  | MUL | 0       |       |
        | root           | bigint(20)    | YES  | MUL | 0       |       |
        | mentions       | varchar(5000) | YES  |     |         |       |
        | app            | int(11)       | NO   |     | NULL    |       |
        | type           | int(11)       | NO   |     | NULL    |       |
        | message        | varchar(4096) | NO   |     | NULL    |       |
        | app_data       | varchar(4096) | YES  |     | {}      |       |
        | attachments    | varchar(8192) | YES  |     | []      |       |
        | device         | varchar(64)   | YES  |     |         |       |
        | can_comment    | tinyint(4)    | NO   |     | 1       |       |
        | can_like       | tinyint(4)    | NO   |     | 1       |       |
        | privince       | tinyint(4)    | NO   |     | 0       |       |
        +----------------+---------------+------+-----+---------+-------+

     */


    @Override
    public void migrate() {
        stream(getOldPlatformDb(), getNewPlatformDb());
    }

    public static void stream(String inPlatformDb, String outPlatformDb) {
        SQLExecutor.executeUpdate(getConnectionFactory(), outPlatformDb, "DELETE FROM stream");
        Migrate.migrate(
                new SQLInput(getConnectionFactory(), inPlatformDb, "SELECT * FROM streams"),
                //new PrintOutput(),
                new SQLInsertOutput(getConnectionFactory(), outPlatformDb, "stream"),
                new CounterMigrateHandler() {
                    @Override
                    protected void handle0(Record in, Record[] out) throws MigrateStopException {
                        Record out1 = out[0];
                        long outPostId = RandomUtils.generateId(in.checkGetInt("created_time"));
                        long quote = in.getInt("parent_id", 0L);

                        int attachmentsType = (int) in.checkGetInt("attachment_type");
                        //System.out.println(in.checkGetString("message"));
                        if (attachmentsType == Constants.TEXT_POST || attachmentsType == Constants.LINK_POST || attachmentsType == Constants.APK_POST) {
                            out1.put("post_id", outPostId);
                            out1.put("source", in.checkGetInt("from_id"));
                            out1.put("created_time", in.checkGetInt("created_time"));
                            out1.put("updated_time", 0);
                            out1.put("destroyed_time", 0);
                            out1.put("quote", quote);
                            out1.put("root", quote);
                            out1.put("mentions", in.checkGetString("to_id"));
                            out1.put("app", 1); // QIUPU APP = 1
                            out1.put("type", makeType(attachmentsType));
                            out1.put("message", in.checkGetString("message"));
                            out1.put("app_data", "{}");
                            out1.put("attachments", makeAttachmentsWithApkInfo(attachmentsType, in.checkGetString("attachment")));
                            out1.put("device", "");
                            out1.put("can_comment", 1);
                            out1.put("can_like", 1);
                            out1.put("privince", in.checkGetInt("privacy"));
                        } else {
                            out1.clear();
                        }
                    }
                }
        );
    }

    private static int makeType(int old) {
        switch (old) {
            case 0:
                return Constants.TEXT_POST;
            case 1:
                return Constants.APK_POST;
            case 3:
                return Constants.APK_LINK_POST;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String makeAttachments(int type, String attachments) {
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        switch (type) {
            case 0:
                return JsonUtils.toJson(jnf.arrayNode(), false);
            case 1: {
                JsonNode jn = JsonUtils.parse(attachments);
                String apkId = String.format("%s-%s-arm", jn.get(0).path("apkcomponentname").getTextValue(), jn.get(0).path("apkversioncode").getIntValue());
                return RecordSet.of(Record.of("apk_id", apkId)).toString(false, false);
            }
            case 3: {
                return JsonUtils.toJson(JsonUtils.parse(attachments), false);
            }
            default:
                throw new IllegalArgumentException();

        }
    }

    private static String makeAttachmentsWithApkInfo(int type, String attachments) {
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        switch (type) {
            case 0:
                return JsonUtils.toJson(jnf.arrayNode(), false);
            case 1: {
                JsonNode jn = JsonUtils.parse(attachments);
                String apkId = String.format("%s-%s-arm", jn.get(0).path("apkcomponentname").getTextValue(), jn.get(0).path("apkversioncode").getIntValue());
                return RecordSet.of(Record.of("apk_id", apkId)).toString(false, false);
            }
            case 3: {
                return JsonUtils.toJson(JsonUtils.parse(attachments), false);
            }
            default:
                throw new IllegalArgumentException();

        }
    }
}
