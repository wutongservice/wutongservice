package com.borqs.server.impl.migration.photo;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhotoMigDb extends SqlSupport {
    private static final Logger L = Logger.get(PhotoMigDb.class);

    private Map<Long, String> userIdMap;

    // table
    private Table oldAlbumTable;
    private Table oldPhotoTable;

    public PhotoMigDb() {
    }

    public void setUserIdMap(Map<Long, String> userIdMap) {
        this.userIdMap = userIdMap;
    }


    public Table getOldAlbumTable() {
        return oldAlbumTable;
    }

    public void setOldAlbumTable(Table oldAlbumTable) {
        this.oldAlbumTable = oldAlbumTable;
    }

    public Table getOldPhotoTable() {
        return oldPhotoTable;
    }

    public void setOldPhotoTable(Table oldPhotoTable) {
        this.oldPhotoTable = oldPhotoTable;
    }

    private ShardResult shardPhoto() {
        return oldPhotoTable.getShard(0);
    }

    private ShardResult shardAlbum() {
        return oldAlbumTable.getShard(0);
    }
    public List<Album> getAlbums(final Context ctx) {
        final ShardResult albumSR = shardAlbum();

        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<List<Album>>() {
            @Override
            protected List<Album> handleConnection(Connection conn) {

                final List<Album> conversationList = new ArrayList<Album>();
                String sql = PhotoMigSql.getAlbums(ctx, albumSR.table);
                SqlExecutor.executeList(ctx, conn, sql, conversationList, new ResultSetReader<Album>() {
                    @Override
                    public Album read(ResultSet rs, Album reuse) throws SQLException {
                        return PhotoMigRs.readAlbum(rs, null, userIdMap);
                    }
                });
                return conversationList;
            }
        });
    }

    public List<Photo> getPhotos(final Context ctx) {
        final ShardResult photoSR = shardPhoto();

        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<List<Photo>>() {
            @Override
            protected List<Photo> handleConnection(Connection conn) {

                final List<Photo> photoList = new ArrayList<Photo>();
                String sql = PhotoMigSql.getPhotoes(ctx, photoSR.table);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoMigRs.readPhoto(rs, null, userIdMap);
                    }
                });
                return photoList;
            }
        });
    }

}

