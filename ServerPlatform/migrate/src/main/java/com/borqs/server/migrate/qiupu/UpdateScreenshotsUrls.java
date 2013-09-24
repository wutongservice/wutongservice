package com.borqs.server.migrate.qiupu;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.sql.SimpleConnectionFactory;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import java.util.ArrayList;

public class UpdateScreenshotsUrls {
    private static final ConnectionFactory CONNECTION_FACTORY = new SimpleConnectionFactory();
    private static final String ALIYUN_QIUPU_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/qiupuservice/qiupuservice/qiupuservice";
    private static final String ALIYUN_ACCOUNT_DB = "mysql/borqsservice.mysql.rds.aliyuncs.com/accounts/accounts/accounts";

    public static ConnectionFactory getConnectionFactory() {
        return CONNECTION_FACTORY;
    }

    public static void updateScreenshotsUrls() throws Exception {
        final String UPDATE_SCREENSHOTS_URLS_SQL
                = "UPDATE qapk SET screenshots_urls=${v(screenshots_urls)} WHERE screenshots_urls=${v(old_urls)}";
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_QIUPU_DB,
                "SELECT screenshots_urls FROM qapk", null);

        for (Record rec : recs) {
            String inScreenshotPaths = rec.getString("screenshots_urls");
            try {
                ArrayNode arrayNode = (ArrayNode) JsonUtils.parse(inScreenshotPaths);
                ArrayList<String> outScreenshotPaths = new ArrayList<String>();
                for (JsonNode jsonNode : arrayNode) {
                    String inScreenshotPath = jsonNode.getTextValue();
                    String outScreenshotPath = inScreenshotPath;
                    if (StringUtils.countMatches(StringUtils.substringAfterLast(inScreenshotPath, "screenshot"), ".") == 2) {
                        String suffix = StringUtils.substringAfterLast(inScreenshotPath, ".");
                        outScreenshotPath = StringUtils.substringBeforeLast(StringUtils.substringBeforeLast(inScreenshotPath, "."), ".")
                                + "." + suffix;
                    }
                    outScreenshotPaths.add(outScreenshotPath);
                }
                String outJson = JsonUtils.toJson(outScreenshotPaths, false);
                String sql = SQLTemplate.merge(UPDATE_SCREENSHOTS_URLS_SQL, "screenshots_urls",
                        outJson, "old_urls", inScreenshotPaths);
                SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_QIUPU_DB, sql);
                System.out.println("Success: " + inScreenshotPaths + " -> " + outJson);
            } catch (Exception e) {
                System.out.println("Failed: " + inScreenshotPaths);
                continue;
            }
        }
    }

    //update stream attachments urls
    public static void updateStreamAttachments() throws Exception {
        final String UPDATE_ATTACHMENTS_URLS_SQL
                = "UPDATE stream SET attachments=${v(attachments)} WHERE attachments=${v(old_attachments)}";
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB,
                "SELECT attachments FROM stream WHERE type=2", null);

        for (Record rec : recs) {
            String oldAttach = rec.getString("attachments");
            try {
//                String newAttach = StringUtils.replace(oldAttach, "http://api.borqs.com/sys/icon", "http://storage.aliyun.com/wutong-system");
//                newAttach = StringUtils.replace(newAttach, "http://api.borqs.com/scheme_image", "http://storage.aliyun.com/wutong-system");
//                newAttach = StringUtils.replace(newAttach, "http://api.borqs.com/profile_image", "http://storage.aliyun.com/wutong-photo");
//                newAttach = StringUtils.replace(newAttach, "http://api.borqs.com/apk", "http://storage.aliyun.com/wutong-app");
//                newAttach = StringUtils.replace(newAttach, "http://static-apk.borqs.com/apk", "http://storage.aliyun.com/wutong-app");
//                newAttach = StringUtils.replace(newAttach, "http://api.borqs.com/links", "http://storage.aliyun.com/wutong-link");
//                newAttach = newAttach.replaceAll("http://api.borqs.com/photo/(\\w+)/(\\w+)/(\\w+)", "http://storage.aliyun.com/wutong-photo/$3");
//                newAttach = StringUtils.replace(newAttach, "http://api.borqs.com/photo", "http://storage.aliyun.com/wutong-photo");

//                String newAttach = StringUtils.replace(oldAttach, "http://storage.aliyun.com/wutong-system", "http://storage.aliyun.com/wutong-data/system");
//                newAttach = StringUtils.replace(newAttach, "http://storage.aliyun.com/wutong-photo", "http://storage.aliyun.com/wutong-data/media/photo");
//                newAttach = StringUtils.replace(newAttach, "http://storage.aliyun.com/wutong-link", "http://storage.aliyun.com/wutong-data/media/link");

                String newAttach = oldAttach.replaceAll("http://storage.aliyun.com/wutong-data/media/photo/(\\w+)/(\\w+)/(\\w+)", "http://storage.aliyun.com/wutong-data/media/photo/$3");
                
                String sql = SQLTemplate.merge(UPDATE_ATTACHMENTS_URLS_SQL, "attachments",
                        newAttach, "old_attachments", oldAttach);
                SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql);
                System.out.println("Success: " + oldAttach);
                System.out.println(" -> " + newAttach);
            } catch (Exception e) {
                System.out.println("Failed: " + oldAttach);
                continue;
            }
        }
    }

    public static void updatePhotoIdAndStreamId() {
        RecordSet recs = SQLExecutor.executeRecordSet(getConnectionFactory(), ALIYUN_ACCOUNT_DB,
                "SELECT photo_id, img_middle, stream_id FROM photo WHERE stream_id<>0", null);
        
        for (Record rec : recs) {
            String photoId = rec.getString("photo_id");
            String imgMiddle = rec.getString("img_middle");
            String streamId = rec.getString("stream_id");
            try {
                String sql = "UPDATE photo SET photo_id=" + photoId + ", stream_id=" + streamId
                        + " WHERE img_middle='" + imgMiddle + "'";
                SQLExecutor.executeUpdate(getConnectionFactory(), ALIYUN_ACCOUNT_DB, sql);
                System.out.println("Success: " + photoId);
            } catch (Exception e) {
                System.out.println("Failed: " + photoId);
                continue;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (ArrayUtils.isEmpty(args)) {
            System.out.println("Usage: UpdateScreenshotsUrls [function]");
            System.out.println("      function=1: update screenshots urls");
            System.out.println("      function=2: update stream attachments urls");
            System.out.println("      function=3: update photoId and streamId");
        }

        String function = args[0];
        if (StringUtils.equals(function, "1")) {
            updateScreenshotsUrls();
        }
        else if (StringUtils.equals(function, "2")) {
            updateStreamAttachments();
        }
        else if (StringUtils.equals(function, "3")) {
            updatePhotoIdAndStreamId();
        }
    }
}
