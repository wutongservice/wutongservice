package com.borqs.server.platform.feature.video;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.sfs.SFS;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public interface VideoLogic extends Logic {
    boolean saveVideo(Context ctx,Video video);
    List<Video> getVideo(Context ctx,long userId, boolean asc, Page page) ;
    List<Video> getVideoById(Context ctx,long... videoId);
    boolean deleteVideo(Context ctx,long... videoId);
    boolean uploadVideo(Context ctx, SFS sfs, FileItem fileItem, String name);

}
