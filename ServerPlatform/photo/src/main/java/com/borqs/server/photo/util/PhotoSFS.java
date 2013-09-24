package com.borqs.server.photo.util;

import java.io.OutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sfs.local.LocalSFS;

public class PhotoSFS extends LocalSFS {

	public PhotoSFS(Configuration conf) {
		super(conf);
	}

	public PhotoSFS(String file) {
		super(file);
	}
	
	@Override
	public OutputStream create(String file) {
		Validate.notNull(file);
		checkFile(file);
		return super.create(file);
	}
	
	@Override
	protected boolean isValidFile(String file) {
//    	String dir = StringUtils.substringBeforeLast(file, "/");
//    	
//    	if (!album.exists(dir)){
//    		return false;
//    	}
    	return super.isValidFile(StringUtils.substringAfterLast(file, "/"));
    }
    
}
