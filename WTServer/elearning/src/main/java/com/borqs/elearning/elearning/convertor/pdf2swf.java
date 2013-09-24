package com.borqs.elearning.elearning.convertor;

import java.io.File;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class pdf2swf {
	
	public static final String PDF2SWF = "pdf2swf";
	public static final String CONVERTCMD = "./pdf2swf -o {outpath} {origfile} -s flashversion=9";
    public static final String PDF2SWFWINCMD = "pdf2swf.exe -o \"%?%\" -s flashversion=9 \"";
    private static Log log = LogFactory.getLog(PDF2SWF);
    
    public static void main(String[] args) {
    	execPdfToSwf(args[0],args[1],args[2]);
    }
 
	public static void execPdfToSwf(String param, String orginName, String savePath) {
		Process process = null;
		String ext = param.substring(param.lastIndexOf(".") + 1);
		if (ext != null && !"".equals(ext) && "done".equals(ext)) {
		    File tmp = new File(param);
		    if (tmp != null && tmp.exists()) {
			   tmp.renameTo(new File(param.substring(0, param.lastIndexOf("."))));
		    }
		}
		//param = param.substring(0, param.lastIndexOf("."));
		//String cmd = CONVERTCMD + " \""+ param + "\"";
//		String cmd = CONVERTCMD;
        String cmd = PDF2SWFWINCMD;
		cmd = cmd.replace("{outpath}", savePath).replace("{origfile}", orginName);
		log.info("cmd--->" + cmd);
		try {
		    process = Runtime.getRuntime().exec(cmd);// 执行命令
		    StreamGobbler sg1 = new StreamGobbler(process.getInputStream(),"Console", PDF2SWF);
		    StreamGobbler sg2 = new StreamGobbler(process.getErrorStream(),"Error", PDF2SWF);
		    sg1.start();
		    sg2.start();
		    int result = process.waitFor();
		    if (result == 0) {
			    try {
			        //TODO:
			    } catch (Exception e) {
			        log.error(e);
			    }
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
    }
}
