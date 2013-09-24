package com.borqs.server.photo.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sfs.SFSException;
import com.borqs.server.base.util.FileUtils2;
import com.borqs.server.base.util.StringUtils2;

public class AlbumSDS implements StaticDirStorage {
	private String rootDir;
	public AlbumSDS(Configuration conf){
		this(conf.checkGetString("dir"));
	}
	
	public AlbumSDS(String rootdir){
		Validate.notNull(rootdir);
		rootdir = FileUtils2.expandPath(rootdir.trim());
        if (!rootdir.endsWith("/"))
        	rootdir += "/";
        
        try {
            FileUtils.forceMkdir(new File(rootdir));
        } catch (IOException e) {
            throw new SFSException(e);
        }
        this.rootDir = rootdir;
	}

    private String joinPath(String path) {
        path = StringUtils2.joinIgnoreNull(rootDir, path);
        if (!path.endsWith("/"))
        	path += "/";
        
        return path;
    }
	@Override
	public void init() {

	}

	@Override
	public void destroy() {

	}

	@Override
	public boolean create(String dir) {
        Validate.notNull(dir);
        checkFile(dir);
        
        String path = joinPath(dir);
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
        	throw new SFSException("dir has already created");
        }
        try {
        	FileUtils.forceMkdir(new File(dir));
        } catch (IOException e) {
        	throw new SFSException("create dir failure");
        }
		return true;
	}

	@Override
	public boolean delete(String dir) {
		Validate.notNull(dir);
		
		try {
			FileUtils.deleteDirectory(new File(joinPath(dir)));
		} catch (IOException e) {
			throw new SFSException("delete dir failure");
		}
		return true;
	}

	@Override
	public boolean exists(String dir) {
		Validate.notNull(dir);
		
		String path = joinPath(dir);
		File f = new File(path);
		
		return f.exists() && f.isDirectory();
	}
    protected boolean isValidFile(String file) {
        return file.matches("^(\\w|\\.|\\,|-)+$");
    }

    protected final void checkFile(String file) {
        if (!isValidFile(file))
            throw new SFSException("Invalid file path (%s)", file);
    }
}
