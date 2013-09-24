package com.borqs.server.impl.audio;


import com.borqs.server.platform.feature.audio.Audio;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AudioRs {

    public static Audio readAudio(ResultSet rs, Audio audio) throws SQLException {
        if (audio == null)
            audio = new Audio();
        audio.setAudioId(rs.getLong("audio_id"));
        audio.setTitle(rs.getString("title"));
        audio.setSummary(rs.getString("summary"));
        audio.setDescription(rs.getString("description"));
        audio.setLevel(rs.getInt("level"));
        audio.setSchools(rs.getInt("schools"));
        audio.setFileSize(rs.getInt("file_size"));
        audio.setAudioTimeLength(rs.getInt("audio_time_length"));
        audio.setAudioBitRate(rs.getInt("audio_bit_rate"));
        audio.setAudioBitRate(rs.getInt("audio_bit_rate"));
        audio.setAuthor(rs.getString("author"));
        audio.setAudioArtists(rs.getString("audio_artists"));
        audio.setRecord(rs.getString("record"));
        audio.setRecordAuthor(rs.getString("record_author"));
        audio.setRecordYear(rs.getString("record_year"));
        audio.setCodingType(rs.getString("coding_type"));
        audio.setCompressionType(rs.getString("compression_type"));
        audio.setUserId(rs.getLong("user_id"));
        audio.setExpName(rs.getString("exp_name"));
        audio.setHtmlUrl(rs.getString("html_url"));
        audio.setContentType(rs.getString("content_type"));
        audio.setNewFileName(rs.getString("new_file_name"));
        audio.setUpdatedTime(rs.getLong("updated_time"));
        audio.setCreatedTime(rs.getLong("created_time"));
        audio.setDestroyedTime(rs.getLong("destroyed_time"));
        return audio;
    }
}
