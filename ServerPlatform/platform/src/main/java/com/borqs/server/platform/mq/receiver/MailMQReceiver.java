package com.borqs.server.platform.mq.receiver;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;

public class MailMQReceiver
{
	private static final Logger L = LoggerFactory.getLogger(MailMQReceiver.class);
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			GenericTransceiverFactory tf = new GenericTransceiverFactory();
			
			String confPath = "/home/zhengwei/work2/dist/etc/test_web_server.properties";
		  	  
		  	  if((args != null) && (args.length > 0))
		  	  {
		  		  confPath = args[0];
		  	  }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
		  	Configuration conf = Configuration.loadFiles(confPath).expandMacros();
			tf.setConfig(conf);
		  	tf.init();
		  	final Platform p = new Platform(tf);
		  	p.setConfig(conf);

			MQCollection.initMQs(conf);
			MQ mq = MQCollection.getMQ("platform");
			
			//pid
	        String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
	        File pidDir = new File(pidDirStr);
	        if(!pidDir.exists())
	        {
	        	FileUtils.forceMkdir(pidDir);
	        }
	        ProcessUtils.writeProcessId(pidDirStr + "/mail_mq_receiver.pid");
			
			while(true)
			{
				String json = mq.receiveBlocked("mail");
				JsonNode jn = JsonUtils.parse(json);
				int type = jn.path("type").getIntValue();
				String viewerId = jn.path("viewerId").getTextValue();
				Record user = Record.fromJson(jn.path("user").getTextValue());
				String target = jn.path("target").getTextValue();
				String message = jn.path("message").getTextValue();
                String lang = jn.path("lang").getTextValue();
				
				try
				{
					if(type == Constants.APK_OBJECT)
					{
						p.sendApkCommentOrLikeEmail(viewerId, user, target, message, lang);
					}
					else if(type == Constants.POST_OBJECT)
					{
						p.sendStreamCommentOrLikeEmail(viewerId, user, target, message, lang);
					}
				}
				catch (Exception e) {					
					L.debug("Send mail failed due to " + e.getMessage());
					L.debug("Type: " + type + "  Message: " + message);
					continue;
				}
			}
		}
		finally
		{
			MQCollection.destroyMQs();
		}
	}
}