package com.borqs.server.test.photo.test1;


import com.borqs.server.impl.photo.PhotoDb;
import com.borqs.server.impl.photo.PhotoImpl;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.photo.Album;
import com.borqs.server.platform.feature.photo.Albums;
import com.borqs.server.platform.feature.photo.Photo;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;
import com.borqs.server.platform.util.DateHelper;

import java.util.List;

public class PhotoTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(PhotoDb.class);
    }

    private PhotoImpl getPhotoImplLogic() {
        return (PhotoImpl) getBean("logic.photo");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testCreateAlbum() {
        PhotoImpl photo = this.getPhotoImplLogic();
         Album album = new Album();
        album.setTitle("title");
        album.setAlbum_type(0);
        album.setBytes_used(0);
        album.setCan_upload(1);
        album.setCreated_time(DateHelper.nowMillis());
        album.setSummary("summary");
        album.setLocation("");
        photo.createAlbum(ctx, album);

    }

    public void testGetAlbums() {
        PhotoImpl photo = this.getPhotoImplLogic();
        Albums a = photo.getUserAlbum(ctx, 10001,0,false);
        int s = a.size();
    }

    public void testDeleteAlbums() {
        PhotoImpl photo = this.getPhotoImplLogic();
         long[] longs ={Long.parseLong("2811471417529426271")};
        photo.destroyAlbum(ctx,longs );
    }

    public void testUpdateAlbums() {
        PhotoImpl photo = this.getPhotoImplLogic();
        Album album = new Album();

        album.setAlbum_id(Long.parseLong("2811471102849397692"));
        album.setAlbum_type(0);
        album.setUser_id(ctx.getViewer());
        album.setTitle("new titile");
        album.setSummary("new summary");
        album.setCover_photo_id(0);
        album.setPrivacy(false);
        album.setCan_upload(0);
        album.setNum_photos(0);
        album.setLocation("");
        album.setHtml_page_url("");
        album.setThumbnail_url("");
        album.setBytes_used(0);
        album.setCreated_time(DateHelper.nowMillis());
        album.setUpdated_time(DateHelper.nowMillis());
        album.setPublish_time(DateHelper.nowMillis());
        album.setPhotos_etag("");
        album.setPhotos_dirty(0);

        photo.updateAlbum(ctx, album);
    }

    public void testGetAlbumsById() {
        PhotoImpl photo = this.getPhotoImplLogic();
//         boolean withPhotos = req.getBoolean("with_photos", false);
        long[] longs = {Long.parseLong("2811471102849397692")};
        List<Album> listAlbums = photo.getAlbums(ctx, false, longs);
        int s = listAlbums.size();
    }

     public void testPhotoUpload() {
        PhotoImpl photo = this.getPhotoImplLogic();
        long[] longs = {Long.parseLong("2811471102849397692")};
        List<Album> listAlbums = photo.getAlbums(ctx, false, longs);
        int s = listAlbums.size();
    }
    public void testPhotoGet(){
        PhotoImpl photo = this.getPhotoImplLogic();
        List<Photo> p = photo.getPhotos(ctx,new long[]{2816895089270407232l});
        int s = 3;
    }
}
