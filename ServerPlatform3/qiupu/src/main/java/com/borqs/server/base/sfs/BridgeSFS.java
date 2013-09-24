package com.borqs.server.base.sfs;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.platform.app.GlobalSpringAppContext;
import com.borqs.server.platform.sfs.SFS;


import java.io.*;

public class BridgeSFS extends AbstractSFS {
    private final String sfsBeanId;

    public BridgeSFS(Configuration conf) {
        this(conf.checkGetString("sfsBean"));
    }

    public BridgeSFS(String sfsBeanId) {
        this.sfsBeanId = sfsBeanId;
    }


    public String getSfsBeanId() {
        return sfsBeanId;
    }

    @Override
    public OutputStream create(String file) {
        SFS sfs = (SFS)GlobalSpringAppContext.getBean(sfsBeanId);
        return new BufferOutputStream(sfs, file);
    }

    private class BufferOutputStream extends ByteArrayOutputStream {
        private SFS sfs;
        private String file;

        private BufferOutputStream(SFS sfs, String file) {
            this.sfs = sfs;
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            byte[] buff = toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(buff);
            sfs.write(file, in);
            in.close();
        }
    }

    @Override
    public InputStream read(String file) {
        SFS sfs = (SFS)GlobalSpringAppContext.getBean(sfsBeanId);
        try {
            return sfs.read(file);
        } catch (IOException e) {
            throw new SFSException(e, "Read file error (%s)", file);
        }
    }

    @Override
    public boolean delete(String file) {
        SFS sfs = (SFS)GlobalSpringAppContext.getBean(sfsBeanId);
        try {
            sfs.delete(file);
        } catch (IOException ignored) {
        }
        return true;
    }

    @Override
    public boolean exists(String file) {
        SFS sfs = (SFS)GlobalSpringAppContext.getBean(sfsBeanId);
        return sfs.exists(file);
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }
}
