package com.borqs.information.util;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

public class ConfigPathUtil {
	public static String[] absolutePaths(String[] fileNames) {
		String[] contextFiles = absolutePaths(fileNames, new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if(name.matches("^applicationContext.*avro\\.xml$")) {
					return false;
				}
				if(name.matches("^applicationContext.*\\.xml$")) {
					return true;
				}
				return false;
			}
		});
		
		return contextFiles;
	}
	
	public static String[] absolutePaths(String[] fileNames, FilenameFilter filenameFilter) {
		System.out.println("execute init(args) method");
		
		URL location = ConfigPathUtil.class.getProtectionDomain().getCodeSource().getLocation();
		String path = location.getPath();
		System.out.println("class location is:"+path);
		
		URI uri = new File(path).getParentFile().getParentFile().toURI();
		System.out.println("application location is:"+uri);

		// iterator configuration files
		// iterator configuration files
		File configFiles = new File(uri.getPath()+"conf");
		System.out.println("config path is "+configFiles.getAbsolutePath());
		
		String[] contextFiles = null;
		if(null == filenameFilter) {
			contextFiles = configFiles.list();
		} else {
			contextFiles = configFiles.list(filenameFilter);
		}
		
		String contextPath = uri+"conf"+File.separator;
		for(int i=0; null!=contextFiles && i<contextFiles.length; i++) {
			contextFiles[i] = contextPath + contextFiles[i];
		}
		System.out.println(Arrays.toString(contextFiles));
		
		return contextFiles;
	}
}
