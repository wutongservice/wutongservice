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
import com.borqs.server.qiupu.ApkCategory;
import com.borqs.server.qiupu.ApkId;
import com.borqs.server.qiupu.Qiupu;
import com.borqs.server.qiupu.QiupuLogics;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.MarketSession.Callback;
import com.gc.android.market.api.model.Market.*;
import com.gc.android.market.api.model.Market.App.ExtendedInfo;
import com.gc.android.market.api.model.Market.GetImageRequest.AppImageUsage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppMarketInfoReader implements AsyncTaskListener {
	private static long count = 0;
	private static String[] gAccount = {"borqs2007@gmail.com","qiupu2011@gmail.com"};
	private static String password = "borqsoms";
	private StaticFileStorage apkStorage;
	private static final Logger L = LoggerFactory.getLogger(AppMarketInfoReader.class);
	
	public AppMarketInfoReader(Configuration conf)
	{
		apkStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.apkStorage", LocalSFS.class.getName() + "|~/.apk"));
        apkStorage.init();
	}
	
	private void readApkExtendedInfo(final MarketSession session, AppsRequest appsRequest, final Record apk)
	{
		session.append(appsRequest, new Callback<AppsResponse>() {
	         public void onResult(ResponseContext context, AppsResponse response) {	                 
	        	 boolean isEn = session.getContext().getUserLanguage().equalsIgnoreCase("en");
        	     String key = "";  
	        	 List<App> list = response.getAppList();
	        	 for(App app : list)
	        	 {
	        	     ExtendedInfo exInfo = app.getExtendedInfo();
	        		 String appPackageName = app.getPackageName();
//	        	     String appTitle = app.getTitle();
	        		 String nowPackageName = apk.getString("package");
//	        		 String nowTitle = apk.getString("app_name");
	        	     boolean con1 = appPackageName.contains(nowPackageName);
	        		 boolean con2 = nowPackageName.contains(appPackageName);
//	        		 boolean con3 = appTitle.contains(nowTitle);
//	        		 boolean con4 = nowTitle.contains(appTitle);	        		
	        		 
	        		 if((con1 || con2) /* && (con3 || con4)*/)
	        	     {
	        			 apk.putMissing("market_appid", app.hasId() ? app.getId() : "0");		        	 
	        	    	 
	        			 if(app.hasId())
	        			 {
	        				 apk.putMissing("source", Qiupu.SOURCE_GOOGLE_MARKET_API);
	        			 }
	        			 
	        			 if(app.hasRating())
	        	    	 {
	        	    		 apk.putMissing("rating", app.getRating());			        	 
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasDescription())
	        	    	 {
	        	    		 key = isEn ? "description_en" : "description";
	        	    		 apk.putMissing(key, exInfo.getDescription());
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasScreenshotsCount())
	        	    	 {
	        	    		 apk.putMissing("screenshots_count", exInfo.getScreenshotsCount());
	        	    	 }
	        	    	 else
	        	    	 {
	        	    		 apk.putMissing("screenshots_count", 0);
	        	    	 }
	        	    	 
	        	    	 if(app.hasCreator())
	        	    	 {
	        	    		 apk.putMissing("developer", app.getCreator());
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasRecentChanges())
	        	    	 {
	        	    		 key = isEn ? "recent_change_en" : "recent_change";
	        	    		 apk.putMissing(key, exInfo.getRecentChanges());
	        	    	 }
	        	    	 
	        	    	 if(app.hasPrice())
	        	    	 {
	        	    		 apk.putMissing("price", app.getPrice());
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasContactEmail())
	        	    	 {
	        	    		 apk.putMissing("developer_email", exInfo.getContactEmail());
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasContactPhone())
	        	    	 {
	        	    		 apk.putMissing("developer_phone", exInfo.getContactPhone());
	        	    	 }
	        	    	 
	        	    	 if(exInfo.hasContactWebsite())
	        	    	 {
	        	    		 apk.putMissing("developer_website", exInfo.getContactWebsite());
	        	    	 }
	        	    	 
	        	    	 String category = app.hasAppType() ? app.getAppType().name() : "APPLICATION";
	        	    	 
	        	         long categoryId = ApkCategory.APPTYPE_APPLICATION;
	        	    	 if(category.equals("GAME"))
	        	    		 categoryId = ApkCategory.APPTYPE_GAME;
	        	    	 apk.putMissing("category", categoryId);
	        	    	 
	        	    	 long subcategoryId = ApkCategory.CATEGORY_DEFAULT;
	        	    	 
	        	    	 key = isEn ? "Default" : "其它";
	        	    	 String subcategory = exInfo.hasCategory() ? exInfo.getCategory() : key;	        	    	 
	        	         if(categoryId == ApkCategory.APPTYPE_APPLICATION)
	        	    	 {
	        	        	 subcategoryId = ApkCategory.getCategoryidByAppName(subcategory, isEn);
	        	    	 }
	        	    	 else if(categoryId == ApkCategory.APPTYPE_GAME)
	        	    	 {
	        	    		 subcategoryId = ApkCategory.getCategoryidByGameName(subcategory, isEn);
	        	    	 }	        	         
	        	         apk.putMissing("sub_category", subcategoryId);
	        	    	 
	        	         if(isEn && app.hasTitle())
	        	         {
	        	        	 apk.putMissing("app_name_en", app.getTitle());
	        	         }
	        	         
	        	    	 break;
	        	     }
	        	 }
	         }
	});			
	try {
		session.flush();
	}
	catch(Exception e)
	{
		long version_code = apk.getInt("version_code");
		String package_ = apk.getString("package");
		L.debug(package_ + "_" + version_code + " get extended information failed due to " + e.getMessage());
	}
	}
	
	public void asyncRead(final Record apk, final RecordHandler handler) {
		MarketSession session = new MarketSession("zh", "CN");		
		
		int index = (int) ((count++) % 2);
		try {
			session.login(gAccount[index], password);
		}
		catch(Exception e)
		{
			L.debug("Can not connect google server due to " + e.getMessage());
		}

		String query = "pname:" + apk.getString("package");		
		
		AppsRequest appsRequest = AppsRequest.newBuilder()
		                                .setQuery(query)
		                                .setStartIndex(0).setEntriesCount(10)
		                                .setWithExtendedInfo(true)
		                                .build();
		
		readApkExtendedInfo(session, appsRequest, apk);
			
		session.setLocale(new Locale("en", "US"));
		readApkExtendedInfo(session, appsRequest, apk);
		
		String apkDesc = apk.getString("description", "");		
		if(StringUtils.isBlank(apkDesc))
		{
			apkDesc = apk.getString("description_en", "");
			if(StringUtils.isNotBlank(apkDesc))
			{
				apk.put("description", apkDesc);
			}
			apkDesc = apk.getString("recent_change_en", "");
			if(StringUtils.isNotBlank(apkDesc))
			{
				apk.put("recent_change", apkDesc);
			}
		}
	
	GetImageRequest imgReq = null;
	int screenshotCount = (int)apk.getInt("screenshots_count");
	String packageName = apk.getString("package");
    int versionCode = (int)apk.getInt("version_code");
    int arch = (int)apk.getInt("architecture");
    final String apkId = ApkId.of(packageName, versionCode, arch).toString();
    final List<String> screenshots = new ArrayList<String>();
    String market_appid = apk.getString("market_appid");
    
	for(int i = 1; i <= screenshotCount; i++)
	{
		apk.put("index", i);
		imgReq = GetImageRequest.newBuilder().setAppId(market_appid)
		.setImageUsage(AppImageUsage.SCREENSHOT)
		.setImageId(String.valueOf(i))
		.build();
	
		session.append(imgReq, new Callback<GetImageResponse>() {
		
			public void onResult(ResponseContext context, GetImageResponse response) {				    				    
				    String fieldName = "screenshot" + apk.getInt("index");
				    long time = DateUtils.nowMillis();
                    String screenshot = apkId + "." + fieldName + ".jpg";
                    if (apkStorage instanceof OssSFS)
                        screenshot = apkId + "." + fieldName + "." + time + ".jpg";
				    
				    com.google.protobuf.ByteString imageData = response.getImageData();
				    if ((imageData != null)
				    		&& (imageData != com.google.protobuf.ByteString.EMPTY)
				    		&& apkStorage.exists(screenshot))
	        		{
	        			apkStorage.delete(screenshot);
	        			try {
	        				SFSUtils.saveBytes(imageData.toByteArray(), apkStorage, screenshot);
                            screenshot = apkId + "." + fieldName + "." + time + ".jpg";
                            screenshots.add(screenshot);
	        			} 
	        			catch(Exception e)
	        			{
	        				long version_code = apk.getInt("version_code");
	        				String package_ = apk.getString("package");
	        				L.debug(package_ + "_" + version_code + " save screenshots failed due to " + e.getMessage());
	        			}	        				        					        			
	        		}
				    if(screenshots.size() > 0)
    			    {
    			    	apk.put("screenshots_urls", JsonUtils.toJson(screenshots, false));
    			    }
    				
    				apk.remove("market_appid");
    				apk.remove("screenshots_count");
    				apk.remove("index");
    				handler.handle(apk);								
			}
		});
		try {
			session.flush();
			
			if(screenshots.size() > 0)
		    {
		    	apk.put("screenshots_urls", JsonUtils.toJson(screenshots, false));
		    }
			
			apk.remove("market_appid");
			apk.remove("screenshots_count");
			apk.remove("index");
			handler.handle(apk);
		}
		catch(Exception e)
		{
			long version_code = apk.getInt("version_code");
			String package_ = apk.getString("package");
			L.debug(package_ + "_" + version_code + " get screenshots failed due to " + e.getMessage());
		}
	}
		
  }
	
  public static void main(String[] args) throws Exception
  {
	  GenericTransceiverFactory tf = new GenericTransceiverFactory();
  	  
      String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
  	  
  	  if((args != null) && (args.length > 0))
  	  {
  		  confPath = args[0];
  	  }

      GlobalConfig.loadFiles(confPath);
      GlobalConfig.get().expandMacros();
      Configuration conf = GlobalConfig.get();
//	  tf.setConfig(Configuration.loadFiles("Z:/workspace2/test/src/test/PlatformWebServer.properties"));
  	  tf.init();
      QiupuLogics.init();
      final Context ctx = Context.dummy();
  	  final Qiupu q = new Qiupu();
  	  
  	  RecordSet recs = q.loadNeedExinfoApks(ctx, false);
  	  
  	  for(Record rec : recs)
  	  {
//  	  Record rec = new Record();
//  	  rec.put("package", "com.facebook.katana");
//  	  rec.put("app_name", "Facebook for Android");
//  	  rec.put("version_code", 5278);
//  	  rec.put("architecture", 1);
  	  ThreadPoolManager.getThreadPool().dispatch(
				new AsyncTask(new AppMarketInfoReader(conf), new Object[]{rec})
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
