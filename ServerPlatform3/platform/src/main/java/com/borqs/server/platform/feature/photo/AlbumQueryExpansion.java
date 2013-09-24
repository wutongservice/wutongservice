package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.log.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class AlbumQueryExpansion implements AlbumExpansion {
    private static final Logger L = Logger.get(AlbumQueryExpansion.class);
    private PhotoLogic photo;
    private String prefix;

    public void setPhoto(PhotoLogic photo) {
        this.photo = photo;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Albums data) {
        // add photos id
        if (data == null || data.size() < 1)
            return;
        if (!(Boolean) ctx.getSession("with_photos"))
            return;

        Page page = new Page(0, 20);
        Photos photos = photo.getPhotosByAlbum(ctx, page, ArrayUtils.toPrimitive(data.geAlbumIds()));
        for (Album album : data) {
            Photos ps = photos.getPhotosByAlbum(album.getAlbum_id());
            album.setAddon("photo_count", ps.getPhotoSizeByAlbum(album.getAlbum_id()));

            album.setAddon("photo_ids", ps.getPhotoIds());
            if (photos.size() > 0) {
                Photo p = photos.get(0);
                album.setAddon("album_cover_photo_small", String.format(prefix, p.getThumbnail_url()));
                album.setAddon("album_cover_photo_big", StringUtils.replace(p.getThumbnail_url(), "_S", "_L"));
                album.setAddon("album_cover_photo_middle", StringUtils.replace(p.getThumbnail_url(), "_S", "_O"));
            }
        }
    }
}
