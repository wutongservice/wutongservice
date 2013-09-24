package com.borqs.server.platform.feature.audio;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.sfs.SFS;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public interface AudioLogic extends Logic {
    boolean saveAudio(Context ctx,Audio audio);
    List<Audio> getAudio(Context ctx,long userId, boolean asc,Page page);
    List<Audio> getAudiosById(Context ctx, long... audioId);
    boolean deleteAudio(Context ctx,long... audioId);
    boolean uploadAudio(Context ctx, SFS sfs, FileItem fileItem, String name);
}
