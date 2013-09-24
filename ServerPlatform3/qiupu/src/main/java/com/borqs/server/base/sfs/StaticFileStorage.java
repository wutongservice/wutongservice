package com.borqs.server.base.sfs;


import com.borqs.server.base.util.Initializable;

import java.io.InputStream;
import java.io.OutputStream;

public interface StaticFileStorage extends Initializable {
    OutputStream create(String file);
    OutputStream createNoThrow(String file);
    InputStream read(String file);
    InputStream readNoThrow(String file);
    boolean delete(String file);
    boolean exists(String file);
}
