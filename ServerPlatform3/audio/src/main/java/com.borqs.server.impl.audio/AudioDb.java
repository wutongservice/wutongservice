package com.borqs.server.impl.audio;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.audio.Audio;
import com.borqs.server.platform.sql.*;
import org.apache.commons.lang.Validate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AudioDb extends SqlSupport {
    private Table audioTable;

    public AudioDb() {
    }

    public Table getAudioTable() {
        return audioTable;
    }

    public void setAudioTable(Table audioTable) {
        if (audioTable != null)
            Validate.isTrue(audioTable.getShardCount() == 1);
        this.audioTable = audioTable;
    }

    private ShardResult shard() {
        return audioTable.getShard(0);
    }


    public long saveAudio(final Context ctx, final Audio audio) {
        final ShardResult audioSR = shard();
        return sqlExecutor.openConnection(audioSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = AudioSql.insertAudio(audioSR.table, audio);
                return SqlExecutor.executeUpdate(ctx, conn, sql);
            }
        });
    }

    public List<Audio> getAudios(final Context ctx, final long... audioIds) {

        final List<Audio> audioList = new ArrayList<Audio>();
        final ShardResult audioSR = shard();

        return sqlExecutor.openConnection(audioSR.db, new SingleConnectionHandler<List<Audio>>() {
            @Override
            protected List<Audio> handleConnection(Connection conn) {
                String sql = AudioSql.getAudios(audioSR.table, audioIds);
                SqlExecutor.executeList(ctx, conn, sql, audioList, new ResultSetReader<Audio>() {
                    @Override
                    public Audio read(ResultSet rs, Audio reuse) throws SQLException {
                        return AudioRs.readAudio(rs, null);
                    }
                });
                return audioList;
            }
        });
    }

    public List<Audio> getAudioByUserIds(final Context ctx, final long... userId) {

        final List<Audio> audioList = new ArrayList<Audio>();
        final ShardResult audioSR = shard();

        return sqlExecutor.openConnection(audioSR.db, new SingleConnectionHandler<List<Audio>>() {
            @Override
            protected List<Audio> handleConnection(Connection conn) {
                String sql = AudioSql.getAudioByUserId(audioSR.table, userId);
                SqlExecutor.executeList(ctx, conn, sql, audioList, new ResultSetReader<Audio>() {
                    @Override
                    public Audio read(ResultSet rs, Audio reuse) throws SQLException {
                        return AudioRs.readAudio(rs, null);
                    }
                });
                return audioList;
            }
        });
    }
    
    public boolean deleteAudio(final Context ctx, final long... audio) {
        final ShardResult audioSR = shard();
        return sqlExecutor.openConnection(audioSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = AudioSql.deleteAudio(audioSR.table, audio);
                long n = SqlExecutor.executeUpdate(ctx, conn, sql);
                return n == 1;
            }
        });
    }
}
