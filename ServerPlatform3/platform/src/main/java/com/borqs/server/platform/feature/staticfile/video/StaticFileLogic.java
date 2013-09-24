package com.borqs.server.platform.feature.staticfile.video;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.sfs.SFS;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public interface StaticFileLogic extends Logic {
    boolean saveStaticFile(Context ctx,StaticFile staticFile);
    List<StaticFile> getStaticFile(Context ctx,long userId, boolean asc, Page page) ;
    List<StaticFile> getStaticFileById(Context ctx,long... staticFileId);
    boolean deleteStaticFile(Context ctx,long... staticFileId);
    boolean uploadStaticFile(Context ctx, SFS sfs, FileItem fileItem, String name);
}
