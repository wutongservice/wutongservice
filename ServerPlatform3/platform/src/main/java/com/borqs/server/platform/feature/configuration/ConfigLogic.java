package com.borqs.server.platform.feature.configuration;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.sfs.SFS;
import org.apache.commons.fileupload.FileItem;

import java.util.List;

public interface ConfigLogic extends Logic {
    boolean saveConfig(Context ctx, Config audio);
    List<Config> getConfig(Context ctx, long userId, String key, int version_code);
    List<Config> getConfigsById(Context ctx, long userId);
    boolean deleteConfig(Context ctx, long userId,String key, int version_code);
    boolean uploadConfig(Context ctx, SFS sfs, FileItem fileItem, String name);
}
