package com.borqs.server.impl.photo;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Albums;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.feature.photo.Photos;
import com.borqs.server.platform.sql.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PhotoDb extends SqlSupport {
    // table
    private Table albumTable;
    private Table photoTable;

    public PhotoDb() {
    }

    public Table getAlbumTable() {
        return albumTable;
    }

    public void setAlbumTable(Table albumTable) {
        this.albumTable = albumTable;
    }

    public Table getPhotoTable() {
        return photoTable;
    }

    public void setPhotoTable(Table photoTable) {
        this.photoTable = photoTable;
    }

    private ShardResult shardAlbum(long albumId) {
        return albumTable.shard(albumId);
    }

    private ShardResult shardAlbum() {
        return albumTable.getShard(0);
    }

    private ShardResult shardPhoto(long photoId) {
        return photoTable.shard(photoId);
    }

    private ShardResult shardPhoto() {
        return photoTable.getShard(0);
    }


    public Album createAlbum(final Context ctx, final Album album) {
        final ShardResult albumSR = shardAlbum();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Album>() {
            @Override
            protected Album handleConnection(Connection conn) {
                //album.setAlbum_id(RandomHelper.generateId());
                String sql = PhotoSql.insertAlbum(albumSR.table, album);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return album;
            }
        });
    }

    public boolean destroyAlbums(final Context ctx, final long userId, final long... albumId) {
        final ShardResult albumSR = shardAlbum();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = PhotoSql.disableAlbum(albumSR.table, userId, albumId);
                long commentId = SqlExecutor.executeUpdate(ctx, conn, sql);

                return true;
            }
        });
    }

    public boolean updateAlbum(final Context ctx, final Album album) {
        final ShardResult albumSR = shardAlbum();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                long isUpdate = 0;
                String updateAlbumSql = PhotoSql.updateAlbum(albumSR.table, album);
                isUpdate = SqlExecutor.executeUpdate(ctx, conn, updateAlbumSql);
                return true;
            }
        });
    }

    public Map<Long, Album[]> getUserAlbums(final Context ctx, final int albumType, final long userId) {
        final ShardResult albumSR = shardAlbum();
        final Map<Long, Album[]> map = new HashMap<Long, Album[]>();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Map<Long, Album[]>>() {
            @Override
            protected Map<Long, Album[]> handleConnection(Connection conn) {
                final Albums albumList = new Albums();
                String sql = PhotoSql.getUserAlbums(albumSR.table, userId, albumType,null);
                SqlExecutor.executeList(ctx, conn, sql, albumList, new ResultSetReader<Album>() {
                    @Override
                    public Album read(ResultSet rs, Album reuse) throws SQLException {
                        return PhotoRs.readAlbum(rs);
                    }
                });
                Album[] albums = new Album[albumList.size()];
                albumList.toArray(albums);
                map.put(userId, albums);
                return map;
            }
        });
    }


    public Albums getAlbums(final Context ctx, final long... albumIds) {
        final ShardResult albumSR = shardAlbum();
        final Albums albumList = new Albums();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Albums>() {
            @Override
            protected Albums handleConnection(Connection conn) {
                String sql = PhotoSql.getAlbums(albumSR.table, albumIds);
                SqlExecutor.executeList(ctx, conn, sql, albumList, new ResultSetReader<Album>() {
                    @Override
                    public Album read(ResultSet rs, Album reuse) throws SQLException {
                        return PhotoRs.readAlbum(rs);
                    }
                });
                return albumList;
            }
        });
    }

    public Albums getUserAlbum(final Context ctx, final int albumType, final long userId) {
        final ShardResult albumSR = shardAlbum();
        final Albums albumList = new Albums();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Albums>() {
            @Override
            protected Albums handleConnection(Connection conn) {
                String sql = PhotoSql.getUserAlbums(albumSR.table, userId, albumType,null);
                SqlExecutor.executeList(ctx, conn, sql, albumList, new ResultSetReader<Album>() {
                    @Override
                    public Album read(ResultSet rs, Album reuse) throws SQLException {
                        return PhotoRs.readAlbum(rs);
                    }
                });
                return albumList;
            }
        });
    }

    public Long ifExistsAlbumName(final Context ctx, final long userId, final String albumName, final int albumType) {
        final ShardResult albumSR = shardAlbum();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Long>() {
            @Override
            protected Long handleConnection(Connection conn) {
                String sql = PhotoSql.ifExistsAlbumName(albumSR.table, userId, albumName, albumType);
                Albums albumList = new Albums();
                SqlExecutor.executeList(ctx, conn, sql, albumList, new ResultSetReader<Album>() {
                    @Override
                    public Album read(ResultSet rs, Album reuse) throws SQLException {
                        return PhotoRs.readAlbum(rs);
                    }
                });
                return albumList.size() > 0 ? albumList.get(0).getAlbum_id() : 0;
            }
        });
    }

    public boolean createPhoto(final Context ctx, final Photo photo) {
        final ShardResult photoSR = shardPhoto();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                //photo.setPhoto_id(RandomHelper.generateId());
                String sql = PhotoSql.insertPhoto(photoSR.table, photo);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return true;
            }
        });
    }

    public boolean destroyPhoto(final Context ctx, final long userId, final long... photoId) {
        final ShardResult photoSR = shardPhoto();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = PhotoSql.disablePhoto(photoSR.table, userId, photoId);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return true;
            }
        });
    }

    public boolean updatePhoto(final Context ctx, final Photo photo) {
        final ShardResult photoSR = shardPhoto();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = PhotoSql.updatePhoto(photoSR.table, photo);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return true;
            }
        });
    }

    public boolean tagPhoto(final Context ctx, final Photo photo) {
        final ShardResult photoSR = shardPhoto();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = PhotoSql.updatePhoto(photoSR.table, photo);
                SqlExecutor.executeUpdate(ctx, conn, sql);
                return true;
            }
        });
    }

    public Photos getPhotos(final Context ctx, final long... photoId) {
        final ShardResult photoSR = shardPhoto();
        final Photos photoList = new Photos();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
            @Override
            protected Photos handleConnection(Connection conn) {
                String sql = PhotoSql.getPhotos(photoSR.table, photoId);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoRs.readPhoto(rs);
                    }
                });
                return photoList;
            }
        });
    }

    public Photos getPhotosByAlbumId(final Context ctx,final Page page, final long... albumIds) {
        final ShardResult photoSR = shardPhoto();
        final Photos photoList = new Photos();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
            @Override
            protected Photos handleConnection(Connection conn) {
                String sql = PhotoSql.getPhotosByAlbumId(photoSR.table, page, albumIds);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoRs.readPhoto(rs);
                    }
                });
                return photoList;
            }
        });
    }

    public Photos searchPhotos(final Context ctx, final String keyWords) {
        final ShardResult photoSR = shardPhoto();
        final Photos photoList = new Photos();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
            @Override
            protected Photos handleConnection(Connection conn) {
                String sql = PhotoSql.searchPhotos(photoSR.table, keyWords);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoRs.readPhoto(rs);
                    }
                });
                return photoList;
            }
        });
    }

    public Photos getPhotosNearBy(final Context ctx, final String longitude, final String latitude) {
        final ShardResult photoSR = shardPhoto();
        final Photos photoList = new Photos();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
            @Override
            protected Photos handleConnection(Connection conn) {
                String sql = PhotoSql.getPhotosNearBy(photoSR.table, longitude, latitude);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoRs.readPhoto(rs);
                    }
                });
                return photoList;
            }
        });
    }

    public Photos getPhotosIncludeMe(final Context ctx, final long userId) {
        final ShardResult photoSR = shardPhoto();
        final Photos photoList = new Photos();
        return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
            @Override
            protected Photos handleConnection(Connection conn) {
                String sql = PhotoSql.getPhotosIncludeMe(photoSR.table, userId);
                SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                    @Override
                    public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                        return PhotoRs.readPhoto(rs);
                    }
                });
                return photoList;
            }
        });
    }

    public boolean isMyAlbumExist(final Context ctx, final long userId, final int albumType,final String albumTitle) {
        final ShardResult albumSR = shardAlbum();
        return sqlExecutor.openConnection(albumSR.db, new SingleConnectionHandler<Boolean>() {
            @Override
            protected Boolean handleConnection(Connection conn) {
                String sql = PhotoSql.getUserAlbums(albumSR.table, userId, albumType, albumTitle);
                long num = SqlExecutor.executeInt(ctx,conn,sql,-1);
                if(num > 0)
                    return true;
                return false;
            }
        });
    }
    public Photos getPhotosByUserId(final Context ctx, final long userId) {
            final ShardResult photoSR = shardPhoto();
            final Photos photoList = new Photos();
            return sqlExecutor.openConnection(photoSR.db, new SingleConnectionHandler<Photos>() {
                @Override
                protected Photos handleConnection(Connection conn) {
                    String sql = PhotoSql.getPhotosByUserId(photoSR.table, userId);
                    SqlExecutor.executeList(ctx, conn, sql, photoList, new ResultSetReader<Photo>() {
                        @Override
                        public Photo read(ResultSet rs, Photo reuse) throws SQLException {
                            return PhotoRs.readPhoto(rs);
                        }
                    });
                    return photoList;
                }
            });
        }
}

