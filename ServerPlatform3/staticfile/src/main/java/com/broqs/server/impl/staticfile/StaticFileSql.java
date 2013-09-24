package com.broqs.server.impl.staticfile;


import com.borqs.server.platform.feature.staticfile.video.StaticFile;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;

import static com.borqs.server.platform.sql.Sql.value;

public class StaticFileSql {
    public static String insertStaticFile(String table, StaticFile staticFile) {
        return new Sql().insertInto(table).values(
                value("file_id", staticFile.getFileId()),
                value("title", staticFile.getTitle()),
                value("summary", staticFile.getSummary()),
                value("description", staticFile.getDescription()),
                value("file_size", staticFile.getFileSize()),
                value("user_id", staticFile.getUserId()),
                value("exp_name", staticFile.getExpName()),
                value("html_url", staticFile.getHtmlUrl()),
                value("content_type", staticFile.getContentType()),
                value("new_file_name", staticFile.getNewFileName()),
                value("created_time", staticFile.getCreatedTime()),
                value("updated_time", staticFile.getUpdatedTime()),
                value("destroyed_time", staticFile.getDestroyedTime())
        ).toString();
    }

    public static String deleteStaticFile(String table, long... fileIds) {
        if (fileIds.length == 1) {
            return new Sql().deleteFrom(table).where("file_id=:file_id", "file_id", fileIds[0]).toString();
        } else {
            return new Sql().deleteFrom(table).where("file_id IN ($fileIds)", "$fileIds", fileIds).toString();
        }
    }

    public static String getStaticFiles(String table, long... staticFileIds) {
        if (staticFileIds.length == 1) {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("file_id =:staticFileIds and destroyed_time = 0 ", "staticFileIds", staticFileIds[0]).orderBy("created_time", "DESC")
                    .toString();
        } else {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("file_id IN ($staticFileId)  and destroyed_time = 0 ", "staticFileId", StringHelper.join(staticFileIds, ",")).orderBy("created_time", "DESC")
                    .toString();
        }
    }


    public static String getStaticFileByUserId(String table, long... userIds) {
        if (userIds.length == 1) {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("user_id =:userId and destroyed_time = 0 ", "userId", userIds[0]).orderBy("created_time", "DESC")
                    .toString();

        } else {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("user_id IN ($userId)  and destroyed_time = 0 ", "userId", StringHelper.join(userIds, ",")).orderBy("created_time", "DESC")
                    .toString();
        }

    }
}
