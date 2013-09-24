package com.borqs.server.platform.feature.photo;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostExpansion;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class PhotoPostExpansion implements PostExpansion {
    private static final Logger L = Logger.get(PhotoPostExpansion.class);
    private static String prefix;
    private PhotoLogic photo;

    public PhotoPostExpansion() {
    }


    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setPhoto(PhotoLogic photo) {
        this.photo = photo;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, Post.COL_ATTACHMENTS))
            expandPhotoAttachments(ctx, data);
    }

    private void expandPhotoAttachments(Context ctx, Posts posts) {
        try {
            for (Post post : posts) {
                if (post.getType() != Post.POST_PHOTO)
                    continue;

                String attachments = post.getAttachments();
                if (StringUtils.isEmpty(attachments) || StringUtils.equals(attachments, "[]")) {

                    //new version
                    String[] ids = post.getAttachmentIds();
                    if (ArrayUtils.isEmpty(ids))
                        continue;

                    long[] longs = new long[ids.length];

                    int i = 0;
                    for (String str : ids) {

                        Target t = Target.parseCompatibleString(str);
                        longs[i++] = t.getIdAsLong();
                    }
                    Photos photoList = photo.getPhotos(ctx, longs);
                    converterPhoto(photoList);
                    post.setAttachments(photoList.toJson(Photo.FULL_COLUMNS,false));


                }
            }
        } catch (Exception e) {
            L.warn(ctx, e, "Expand link image url error");
        }
    }

    private void converterPhoto( final Photos photos) {
        for(Photo p:photos){
            p.setThumbnail_url(addPrefix(p.getThumbnail_url(),null));
        }
    }

     private static String addPrefix(String photoName, String style) {
        if (StringUtils.isNotEmpty(photoName)) {
            String name = "";
            if(StringUtils.isNotEmpty(style))
                name = StringUtils.replace(photoName, "_S", style);
            else
                name = photoName;
            return prefix + "/" + name;
        }
        return "";
    }
}
