package com.borqs.server.impl.audio;


import com.borqs.server.platform.feature.audio.Audio;
import com.borqs.server.platform.sql.Sql;
import com.borqs.server.platform.util.StringHelper;

import static com.borqs.server.platform.sql.Sql.value;

public class AudioSql {
    public static String insertAudio(String table, Audio audio) {
        return new Sql().insertInto(table).values(
                value("audio_id", audio.getAudioId()),
                value("title", audio.getTitle()),
                value("summary", audio.getSummary()),
                value("description", audio.getDescription()),
                value("level", audio.getLevel()),
                value("schools", audio.getSchools()),
                value("file_size", audio.getFileSize()),
                value("audio_time_length", audio.getAudioTimeLength()),
                value("author", audio.getAuthor()),
                value("audio_artists", audio.getAudioArtists()),
                value("record", audio.getRecord()),
                value("record_author", audio.getRecordAuthor()),
                value("record_year", audio.getRecordYear()),
                value("coding_type", audio.getCodingType()),
                value("compression_type", audio.getCompressionType()),
                value("user_id", audio.getUserId()),
                value("exp_name", audio.getExpName()),
                value("html_url", audio.getHtmlUrl()),
                value("content_type", audio.getContentType()),
                value("new_file_name", audio.getNewFileName()),
                value("created_time", audio.getCreatedTime()),
                value("updated_time", audio.getUpdatedTime()),
                value("destroyed_time", audio.getDestroyedTime())
        ).toString();
    }

    public static String deleteAudio(String table, long... audioIds) {
        if (audioIds.length == 1) {
            return new Sql().deleteFrom(table).where("audio_id=:audio_id", "audio_id", audioIds[0]).toString();
        } else {
            return new Sql().deleteFrom(table).where("audio_id IN ($audioIds)", "$audioIds", audioIds).toString();
        }
    }

    public static String getAudios(String table, long... audioIds) {
        if (audioIds.length == 1) {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("audio_id =:audioIds and destroyed_time = 0 ", "audioIds", audioIds[0]).orderBy("created_time", "DESC")
                    .toString();
        } else {
            return new Sql()
                    .select("* ")
                    .from(table)
                    .where("audio_id IN ($audioIds)  and destroyed_time = 0 ", "audioIds", StringHelper.join(audioIds, ",")).orderBy("created_time", "DESC")
                    .toString();
        }
    }


    public static String getAudioByUserId(String table, long... userIds) {
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
