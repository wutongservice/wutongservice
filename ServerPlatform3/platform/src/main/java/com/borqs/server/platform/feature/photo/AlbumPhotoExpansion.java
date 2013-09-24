package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class AlbumPhotoExpansion implements PhotoExpansion {
    private static final Logger L = Logger.get(AlbumPhotoExpansion.class);
    private String prefix;
    private PhotoLogic photo;

    public AlbumPhotoExpansion() {
    }

    public void setPhoto(PhotoLogic photo) {
        this.photo = photo;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Photos data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, Photo.COL_ALBUM_ID))
            expandPhotoAttachments(ctx, data);
    }

    private void expandPhotoAttachments(Context ctx, List<Photo> photos) {
        try {
            for (Photo p : photos) {
                //TODO Recursion is here
                List<Album> al = photo.getAlbums(ctx, false, p.getAlbum_id());
                if (!al.isEmpty()) {
                    Album a = al.get(0);
                    p.setAddon("album_id", a.getAlbum_id());
                    p.setAddon("album_name", a.getTitle());
                    p.setAddon("album_photo_count", a.getNum_photos());
                    p.setAddon("album_cover_photo_id", a.getCover_photo_id());
                    p.setAddon("album_cover_photo_middle", addPrefix(a.getThumbnail_url(), "_O"));
                    p.setAddon("album_cover_photo_big", addPrefix(a.getThumbnail_url(), "_L"));
                    p.setAddon("album_cover_photo_small", addPrefix(a.getThumbnail_url(), "_S"));
                    p.setAddon("album_description", a.getSummary());
                    p.setAddon("album_visible", a.getPrivacy());
                }
            }
        } catch (Exception e) {
            L.warn(ctx, e, "Expand photo error");
        }
    }

    private String addPrefix(String photoName, String style) {
        if (StringUtils.isNotEmpty(photoName)) {
            String name = StringUtils.replace(photoName, "_S", style);
            return prefix + name;
        }
        return "";
    }
}
