package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.commons.Commons;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;

public class MailMQReceiver
{
	private static final Logger L = Logger.getLogger(MailMQReceiver.class);
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
		  	  if((args != null) && (args.length > 0))
		  	  {
		  		  confPath = args[0];
		  	  }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
//		  	Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            GlobalConfig.loadFiles(confPath);

			MQCollection.initMQs();
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
				Record user = Record.fromJson(jn.path("user").getTextValue());
				String target = jn.path("target").getTextValue();
				String message = jn.path("message").getTextValue();

                //context cols
                long viewerId = jn.path("viewerId").getLongValue();
                String app = jn.path("app").getTextValue();
                String ua = jn.path("ua").getTextValue();
                String location = jn.path("location").getTextValue();
                String language = jn.path("language").getTextValue();
                Context ctx = new Context();
                ctx.setViewerId(viewerId);
                ctx.setAppId(app);
                ctx.setUa(ua);
                ctx.setLocation(location);
                ctx.setLanguage(language);

				try
				{
					if(type == Constants.APK_OBJECT)
					{
						Commons.sendApkCommentOrLikeEmail(ctx, user, target, message);
					}
					else if(type == Constants.POST_OBJECT)
					{
						Commons.sendStreamCommentOrLikeEmail(ctx, user, target, message);
					}
				}
				catch (Exception e) {					
					L.debug(null, "Send mail failed due to " + e.getMessage());
					L.debug(null, "Type: " + type + "  Message: " + message);
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