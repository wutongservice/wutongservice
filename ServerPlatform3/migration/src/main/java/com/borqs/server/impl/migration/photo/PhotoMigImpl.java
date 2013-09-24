package com.borqs.server.impl.migration.photo;


import com.borqs.server.impl.migration.CMDRunner;
import com.borqs.server.impl.migration.account.AccountMigImpl;
import com.borqs.server.impl.photo.PhotoDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sql.SqlExecutor;
import com.borqs.server.platform.sql.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PhotoMigImpl implements CMDRunner {

    private static final Logger L = Logger.get(PhotoMigImpl.class);

    private final PhotoMigDb db_migration = new PhotoMigDb();
    private final PhotoDb dbNewPhoto = new PhotoDb();

    private AccountMigImpl account;

    public void setAccount(AccountMigImpl account) {
        this.account = account;
    }

    public void setSqlExecutor(SqlExecutor sqlExecutor) {
        dbNewPhoto.setSqlExecutor(sqlExecutor);
        db_migration.setSqlExecutor(sqlExecutor);
    }

    public void setNewPhotoTable(Table newPhotoTable) {
        dbNewPhoto.setPhotoTable(newPhotoTable);
    }
    public void setNewAlbumTable(Table newAlbumTable) {
        dbNewPhoto.setAlbumTable(newAlbumTable);
    }


    public void setOldPhotoTable(Table oldPhotoTable) {
        db_migration.setOldPhotoTable(oldPhotoTable);
    }
    public void setOldAlbumTable(Table oldAlbumTable) {
        db_migration.setOldAlbumTable(oldAlbumTable);
    }

    @Override
    public List<String> getDependencies() {
        List<String> list = new ArrayList<String>();
        list.add("account.mig");
        return list;
    }

    @Override
    public void run(String cmd, Properties config) {
        if (cmd.equals("photo.mig")) {
            photoMigration(Context.create());
        }
    }

    public void photoMigration(Context ctx) {

        final LogCall LC = LogCall.startCall(L, PhotoMigImpl.class, "photoMigration", ctx);

        List<Album> albums = null;

        try {

            db_migration.setUserIdMap(getAllUserIdMap(ctx));


            albums = db_migration.getAlbums(ctx);
            List<Photo> photos = db_migration.getPhotos(ctx);

            for (Album album : albums) {
                try {
                    if (album != null) {
                        dbNewPhoto.createAlbum(ctx, album);
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }
            for (Photo photo : photos) {
                try {
                    if (photo != null) {
                        dbNewPhoto.createPhoto(ctx, photo);
                    }
                } catch (RuntimeException e) {
                    LC.endCall();
                    throw e;
                }
            }
            LC.endCall();
        } catch (RuntimeException e) {
            LC.endCall();
            throw e;
        }
    }


    private Map<Long, String> getAllUserIdMap(Context ctx) {
        return account.getAllUserIdMap(ctx);
    }



}
