package com.borqs.server.platform.elearning.convertor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StreamGobbler extends Thread {
	private InputStream is;
	private String type;
	private Log log ;
	private static int i = 1;
	private static int j = 1;

	StreamGobbler(InputStream is, String type, String tag) {
		this.is = is;
		this.type = type;
		this.log = LogFactory.getLog(tag);
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			i++;
			j++;
			String line = null;
			while ((line = br.readLine()) != null) {
				if (type.equals("Error")) {
					log.error(line);
				}else{
					log.info(line);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}