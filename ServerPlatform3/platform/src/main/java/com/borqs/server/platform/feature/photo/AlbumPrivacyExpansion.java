package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.Logger;

public class AlbumPrivacyExpansion implements AlbumExpansion {
    private static final Logger L = Logger.get(AlbumPrivacyExpansion.class);

    @Override
    public void expand(Context ctx, String[] expCols, Albums data) {
        if (data == null || data.size() < 1)
            return;

        Albums albums = new Albums();
        albums.addAll(data);
        for (Album album : data) {
            if (ctx.getSession("userId")!=null && ctx.getViewer() != (Long) ctx.getSession("userId")) {
                if (album.getPrivacy())
                    albums.remove(album);
            }
        }
        if (albums.size() > 0){
            data.clear();
            data.addAll(albums);
        }else{
            data.clear();
        }
    }
}
