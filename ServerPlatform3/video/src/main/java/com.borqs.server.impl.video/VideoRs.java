package com.borqs.server.impl.video;


import com.borqs.server.platform.feature.video.Video;

import java.sql.ResultSet;
import java.sql.SQLException;

public class VideoRs {

    public static Video readVideo(ResultSet rs, Video video) throws SQLException {
        if (video == null)
            video = new Video();
        video.setVideoId(rs.getLong("video_id"));
        video.setTitle(rs.getString("title"));
        video.setSummary(rs.getString("summary"));
        video.setDescription(rs.getString("description"));
        video.setLevel(rs.getInt("level"));
        video.setFileSize(rs.getInt("file_size"));
        video.setVideoTimeLength(rs.getInt("video_time_length"));
        video.setVideoHeight(rs.getInt("video_height"));
        video.setVideoWidth(rs.getInt("video_width"));
        video.setVideoDataRate(rs.getInt("video_data_rate"));
        video.setVideoBitRate(rs.getInt("video_bit_rate"));
        video.setVideoFrameRate(rs.getInt("video_frame_rate"));
        video.setAudioBitRate(rs.getInt("audio_bit_rate"));
        video.setAudioChannel(rs.getInt("audio_channel"));
        video.setAudioSamplingRate(rs.getInt("audio_sampling_rate"));
        video.setThumbnail(rs.getString("thumbnail"));
        video.setCodingType(rs.getString("coding_type"));
        video.setCompressionType(rs.getString("compression_type"));
        video.setUserId(rs.getLong("user_id"));
        video.setExpName(rs.getString("exp_name"));
        video.setHtmlUrl(rs.getString("html_url"));
        video.setArtists(rs.getString("artists"));
        video.setContentType(rs.getString("content_type"));
        video.setNewFileName(rs.getString("new_file_name"));
        video.setUpdatedTime(rs.getLong("updated_time"));
        video.setCreatedTime(rs.getLong("created_time"));
        video.setDestroyedTime(rs.getLong("destroyed_time"));
        return video;
    }
}
