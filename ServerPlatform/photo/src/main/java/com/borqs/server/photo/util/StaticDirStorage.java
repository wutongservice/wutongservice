package com.borqs.server.photo.util;

import com.borqs.server.base.util.Initializable;

public interface StaticDirStorage extends Initializable {
	boolean create(String dir);
    boolean delete(String dir);
    boolean exists(String dir);
}
