package com.borqs.server.qiupu.util.market;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.local.LocalSFS;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.email.ThreadPoolManager;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.qiupu.ApkId;
import com.borqs.server.qiupu.Qiupu;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class AppendScreenshots implements AsyncTaskListener {
	
	private StaticFileStorage apkStorage;
	private static final Logger L = LoggerFactory.getLogger(AppendScreenshots.class);
	
	public AppendScreenshots(Configuration conf)
	{
		apkStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.apkStorage", LocalSFS.class.getName() + "|~/.apk"));
        apkStorage.init();
	}
	
	private void downloadScreenshot(InputStream is, String screenshot) throws IOException
	{
		 BufferedInputStream bis = new BufferedInputStream(is);   
         
         BufferedImage bm = ImageIO.read(bis);   
         ByteArrayOutputStream bos = new ByteArrayOutputStream();   
         ImageIO.write(bm, "jpg", bos);   
         bos.flush();   
         byte[] data = bos.toByteArray();   
                                 
         if(apkStorage.exists(screenshot))
         {
         	apkStorage.delete(screenshot);
         }
         SFSUtils.saveBytes(data, apkStorage, screenshot);
         bos.close();
	}
	
	 public void getDetail(Record apk, boolean isEn, RecordHandler handler)
	    {    
		    String package_ = apk.getString("package");
		    int versionCode = (int)apk.getInt("version_code");
		    int arch = (int)apk.getInt("architecture");
		    String apkId = ApkId.of(package_, versionCode, arch).toString();
		    
		    String lang = isEn ? "&hl=en_us" : "&hl=zh_cn";
		    String marketUrl = "https://market.android.com/details?id=" + package_ + lang;
		    
	        List<String> imageUrls = new ArrayList<String>();
	        try
	        {                
	            String inputLine;
	            
	            String regexfindimage = "screenshot-carousel-content-container\">";
	            String regeximage = "<img src=\"";	            
	            
	            //open url and read the inputstream
	            URL url = new URL(marketUrl);            
	            URLConnection con = url.openConnection();
	            BufferedReader in = new BufferedReader(new InputStreamReader
	(con.getInputStream()));
	            
	            boolean startImage = false;
	            
	            while((inputLine = in.readLine())!= null)
	            {	                	                	            		                	                
	                //image	            	
	            	if((startImage == false) && StringUtils.contains(inputLine, regexfindimage))
	            	{	            			            		
	            		if(!StringUtils.contains(inputLine, "</div>"))
	            		{
	            			if(StringUtils.contains(inputLine, regeximage))
		            			imageUrls.add(StringUtils.substringBetween(inputLine, regeximage, "\""));
	            			startImage = true;
	            			continue;
	            		}
	            		else
	            		{
	            			String temp = StringUtils.substringBetween(inputLine, regexfindimage, "</div>");	            			
	            			String screenshot = StringUtils.substringBetween(temp, regeximage, "\"");
	            			while(StringUtils.isNotBlank(screenshot))
	            			{
	            				imageUrls.add(screenshot);
	            				temp = StringUtils.substringAfter(temp, screenshot);
	            				screenshot = StringUtils.substringBetween(temp, regeximage, "\"");	            				
	            			}
	            		}
	            				     
	            	}
	            	
	            	if((startImage == true) && !StringUtils.contains(inputLine, "</div>"))
	            	{
	            		if(StringUtils.contains(inputLine, regeximage))
	            			imageUrls.add(StringUtils.substringBetween(inputLine, regeximage, "\""));	            		
	            	}
	            	
	            	if((startImage == true) && StringUtils.contains(inputLine, "</div>"))
	            	{
	            		if(StringUtils.contains(inputLine, regeximage))
	            			imageUrls.add(StringUtils.substringBetween(inputLine, regeximage, "\""));
	            		
	            		startImage = false;	            		
	            	}
	                
	            }    
	            //close buffer
	            in.close();
               
                //download screenshots
                List<String> screenshots = new ArrayList<String>();
               
                int index = 1;
                long time = DateUtils.nowMillis();
                for(String imageUrl : imageUrls)
                {
                    URL imgUrl = new URL(imageUrl);    
                    URLConnection con2 = imgUrl.openConnection();
                    // input stream
                    InputStream is = con2.getInputStream();
                    String screenshot = apkId + ".screenshot" + index + ".jpg";
                    if (apkStorage instanceof OssSFS)
                        screenshot = apkId + ".screenshot" + index + "." + time + ".jpg";
                    downloadScreenshot(is, screenshot);
                    screenshot = apkId + ".screenshot" + index + "." + time + ".jpg";
                    screenshots.add(screenshot);
        		    index++;
                }
                
                if(screenshots.size() > 0)
    		    {
    		    	apk.put("screenshots_urls", JsonUtils.toJson(screenshots, false));
    		    	apk.putMissing("source", Qiupu.SOURCE_GOOGLE_MARKET_PAGE);
    		    } 
                	            
	            //save to db
	            handler.handle(apk);
	        }
	        catch (Exception e)
	        {	        	   		
	    		L.debug(package_ + "_" + versionCode + " get screenshots failed due to " + e.getMessage());	            
	        }
	        L.debug(package_ + "_" + versionCode + " get screenshots success.");
	    }
 
	 @Override
	 public void asyncRead(final Record apk, RecordHandler handler)
	 {
		 getDetail(apk, false, handler);
	 }
	 
  public static void main(String[] args) throws Exception
  {
	  GenericTransceiverFactory tf = new GenericTransceiverFactory();
  	  
      String confPath = "/home/zhengwei/work2/dist/etc/test_web_server.properties";
  	  
  	  if((args != null) && (args.length > 0))
  	  {
  		  confPath = args[0];
  	  }

      GlobalConfig.loadFiles(confPath);
      GlobalConfig.get().expandMacros();
	  Configuration conf = GlobalConfig.get();

//	  tf.setConfig(Configuration.loadFiles("Z:/workspace2/test/src/test/PlatformWebServer.properties"));
  	  tf.init();
      final Context ctx = Context.dummy();
  	  final Qiupu q = new Qiupu();
  	  
  	  RecordSet recs = q.loadNeedExinfoApks(ctx, true);
  	  
  	  for(Record rec : recs)
  	  {
//  	  Record rec = new Record();
//  	  rec.put("package", "com.msocial.freefb");
//  	  rec.put("app_name", "Social Network");
//  	  rec.put("version_code", 65);
//  	  rec.put("architecture", 1);
  	  ThreadPoolManager.getThreadPool().dispatch(
				new AsyncTask(new AppendScreenshots(conf), new Object[]{rec})
		        {
		            public void invoke(AsyncTaskListener listener,Object[] args)
		            {
		                listener.asyncRead((Record)args[0], new RecordHandler() {

							@Override
							public void handle(Record rec) {
								try {
									q.updateApk(ctx, rec);
								} catch (Exception e) {
									
								}
							}		                	
		                });
		            }
		        });
  	  }
  }
}
