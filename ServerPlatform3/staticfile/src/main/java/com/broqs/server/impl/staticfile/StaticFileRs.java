package com.broqs.server.impl.staticfile;


import com.borqs.server.platform.feature.staticfile.video.StaticFile;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StaticFileRs {

    public static StaticFile readStaticFile(ResultSet rs, StaticFile staticFile) throws SQLException {
        if (staticFile == null)
            staticFile = new StaticFile();
        staticFile.setFileId(rs.getLong("file_id"));
        staticFile.setTitle(rs.getString("title"));
        staticFile.setSummary(rs.getString("summary"));
        staticFile.setDescription(rs.getString("description"));
        staticFile.setFileSize(rs.getInt("file_size"));
        staticFile.setUserId(rs.getLong("user_id"));
        staticFile.setExpName(rs.getString("exp_name"));
        staticFile.setHtmlUrl(rs.getString("html_url"));
        staticFile.setContentType(rs.getString("content_type"));
        staticFile.setNewFileName(rs.getString("new_file_name"));
        staticFile.setCreatedTime(rs.getLong("created_time"));
        staticFile.setUpdatedTime(rs.getLong("updated_time"));
        staticFile.setDestroyedTime(rs.getLong("destroyed_time"));
        return staticFile;
    }
}
