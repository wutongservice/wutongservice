package com.borqs.server.impl.video;


import com.borqs.server.platform.feature.video.Video;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;

import static com.borqs.server.platform.sql.Sql.value;

public class VideoSql {

    public static String insertVideo(String table, Video video) {
        return new Sql().insertInto(table).values(
                value("video_id", video.getVideoId()),
                value("title", video.getTitle()),
                value("summary", video.getSummary()),
                value("description", video.getDescription()),
                value("level", video.getLevel()),
                value("file_size", video.getFileSize()),
                value("video_time_length", video.getVideoTimeLength()),
                value("artists", video.getArtists()),
                value("coding_type", video.getCodingType()),
                value("compression_type", video.getCompressionType()),
                value("user_id", video.getUserId()),
                value("exp_name", video.getExpName()),
                value("html_url", video.getHtmlUrl()),
                value("content_type", video.getContentType()),
                value("new_file_name", video.getNewFileName()),
                value("created_time", video.getCreatedTime()),
                value("updated_time", video.getUpdatedTime()),
                value("destroyed_time", video.getDestroyedTime())
        ).toString();
    }

    public static String deleteVideo(String table, long... videoIds) {
        if (videoIds.length == 1) {
            return new Sql().deleteFrom(table).where("video_id=:videoId", "videoId", videoIds[0]).toString();
        } else {
            return  new Sql().deleteFrom(table).where("video_id IN ($videoIds)", "$videoIds", videoIds).toString();
        }
    }

    public static String getVideos(String table, long... videoIds) {
        if (videoIds.length == 1) {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("video_id =:videoIds and destroyed_time = 0 ", "videoIds", videoIds[0]).orderBy("created_time", "DESC")
                    .toString();
        } else {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("video_id IN ($videoIds)  and destroyed_time = 0 ", "videoIds", StringHelper.join(videoIds, ",")).orderBy("created_time", "DESC")
                    .toString();
        }
    }


    public static String getVideoByUserId(String table, long... userIds) {
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
