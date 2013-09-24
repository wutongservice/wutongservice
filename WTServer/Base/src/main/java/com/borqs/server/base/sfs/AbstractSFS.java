package com.borqs.server.base.sfs;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractSFS implements StaticFileStorage {

    protected AbstractSFS() {
    }

    @Override
    public OutputStream createNoThrow(String file) {
        try {
            return create(file);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public InputStream readNoThrow(String file) {
        try {
            return read(file);
        } catch (Throwable t) {
            return null;
        }
    }

    protected boolean isValidFile(String file) {
        return file.matches("^(\\w|\\.|\\,|-)+$");
    }

    protected final void checkFile(String file) {
        if (!isValidFile(file))
            throw new ServerException(BaseErrors.PLATFORM_SFS_IO_ERROR, "Invalid file path (%s)", file);
    }
}
