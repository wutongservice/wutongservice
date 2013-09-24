package com.borqs.server.qiupu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ApkCategory
{
	public static HashMap<String, Long> appmap = new HashMap<String, Long>();
	public static HashMap<String, Long> gamemap = new HashMap<String, Long>();
	private static HashMap<Long, String> appvaluekey = new HashMap<Long, String>();
	private static HashMap<Long, String> gamevaluekey = new HashMap<Long, String>();
	
	public static HashMap<String, Long> appmap_en = new HashMap<String, Long>();
	public static HashMap<String, Long> gamemap_en = new HashMap<String, Long>();
	private static HashMap<Long, String> appvaluekey_en = new HashMap<Long, String>();
	private static HashMap<Long, String> gamevaluekey_en = new HashMap<Long, String>();
	
	public static long APPTYPE_APPLICATION = 0x100;
	
	public static long CATEGORY_PERSONAL = 0x101;
	public static long CATEGORY_TRAFFIC = 0x102;
	public static long CATEGORY_SPORT = 0x103;
	public static long CATEGORY_HEALTH = 0x104;
	public static long CATEGORY_CORP = 0x105;
	public static long CATEGORY_WALLPAPER = 0x106;
	public static long CATEGORY_ANIMATION = 0x107;
	public static long CATEGORY_MEDICINE = 0x108;
	public static long CATEGORY_BOOK = 0x109;
	public static long CATEGORY_WEATHER = 0x10A;
	public static long CATEGORY_ENTERTAINMENT = 0x10B;
	public static long CATEGORY_VIDEO = 0x10C;
	public static long CATEGORY_TOOLKIT = 0x10D;
	public static long CATEGORY_PHOTOGRAPH = 0x10E;
	public static long CATEGORY_EFFICIENCY = 0x10F;
	public static long CATEGORY_EDUCATION = 0x110;
	public static long CATEGORY_NEWS = 0x111;
	public static long CATEGORY_TOUR = 0x112;
	public static long CATEGORY_LIFE = 0x113;
	public static long CATEGORY_SNS = 0x114;
	public static long CATEGORY_WIDGET = 0x115;
	public static long CATEGORY_FINANCE = 0x116;
	public static long CATEGORY_SHOPPING = 0x117;
	public static long CATEGORY_COMMUNICATION = 0x118;
	public static long CATEGORY_MUSIC = 0x119;
	public static long CATEGORY_COLLECT = 0x11A;
	public static long CATEGORY_DEFAULT = 0x11B;
	
	public static long APPTYPE_GAME = 0x200;
	
	public static long CATEGORY_LEISURE = 0x201;
	public static long CATEGORY_SPORTGAME = 0x202;
	public static long CATEGORY_GWALLPAPER = 0x203;
	public static long CATEGORY_MATCH = 0x204;
	public static long CATEGORY_GAME = 0x205;
	public static long CATEGORY_longELLIGENCE = 0x206;
	public static long CATEGORY_GWIDGET = 0x207;
	public static long CATEGORY_CARD = 0x208;
		 
	
	static
	{		
		//应用
		appmap.put("个性化", CATEGORY_PERSONAL);
		appmap.put("交通", CATEGORY_TRAFFIC);
		appmap.put("体育", CATEGORY_SPORT);
		appmap.put("保健与健身", CATEGORY_HEALTH);
		appmap.put("公司", CATEGORY_CORP);
		appmap.put("动态壁纸", CATEGORY_WALLPAPER);
		appmap.put("动漫", CATEGORY_ANIMATION);
		appmap.put("医药", CATEGORY_MEDICINE);
		appmap.put("图书与工具书", CATEGORY_BOOK);
		appmap.put("天气", CATEGORY_WEATHER);
		appmap.put("娱乐", CATEGORY_ENTERTAINMENT);
		appmap.put("媒体与视频", CATEGORY_VIDEO);
		appmap.put("工具", CATEGORY_TOOLKIT);
		appmap.put("摄影", CATEGORY_PHOTOGRAPH);
		appmap.put("效率", CATEGORY_EFFICIENCY);
		appmap.put("教育", CATEGORY_EDUCATION);
		appmap.put("新闻杂志", CATEGORY_NEWS);
		appmap.put("旅游与本地出行", CATEGORY_TOUR);
		appmap.put("生活方式", CATEGORY_LIFE);
		appmap.put("社交", CATEGORY_SNS);
		appmap.put("窗口小部件", CATEGORY_WIDGET);
		appmap.put("财经", CATEGORY_FINANCE);
		appmap.put("购物", CATEGORY_SHOPPING);
		appmap.put("通信", CATEGORY_COMMUNICATION);
		appmap.put("音乐与音频", CATEGORY_MUSIC);
		appmap.put("个人收藏与展示", CATEGORY_COLLECT);
		appmap.put("其它", CATEGORY_DEFAULT);
		
		//游戏
		gamemap.put("休闲游戏", CATEGORY_LEISURE);
		gamemap.put("体育游戏", CATEGORY_SPORTGAME);
		gamemap.put("动态壁纸", CATEGORY_GWALLPAPER);
		gamemap.put("比赛", CATEGORY_MATCH);
		gamemap.put("游戏", CATEGORY_GAME);
		gamemap.put("益智类游戏", CATEGORY_longELLIGENCE);
		gamemap.put("窗口小部件", CATEGORY_GWIDGET);
		gamemap.put("纸牌游戏", CATEGORY_CARD);
		
		//application
		appmap_en.put("Personalization", CATEGORY_PERSONAL);
		appmap_en.put("Transportation", CATEGORY_TRAFFIC);
		appmap_en.put("Sports", CATEGORY_SPORT);
		appmap_en.put("Health & Fitness", CATEGORY_HEALTH);
		appmap_en.put("Business", CATEGORY_CORP);
		appmap_en.put("Live Wallpaper", CATEGORY_WALLPAPER);
		appmap_en.put("Comics", CATEGORY_ANIMATION);
		appmap_en.put("Medical", CATEGORY_MEDICINE);
		appmap_en.put("Books & Reference", CATEGORY_BOOK);
		appmap_en.put("Weather", CATEGORY_WEATHER);
		appmap_en.put("Entertainment", CATEGORY_ENTERTAINMENT);
		appmap_en.put("Media & Video", CATEGORY_VIDEO);
		appmap_en.put("Tools", CATEGORY_TOOLKIT);
		appmap_en.put("Photography", CATEGORY_PHOTOGRAPH);
		appmap_en.put("Productivity", CATEGORY_EFFICIENCY);
		appmap_en.put("Education", CATEGORY_EDUCATION);
		appmap_en.put("News & Magazines", CATEGORY_NEWS);
		appmap_en.put("Travel & Local", CATEGORY_TOUR);
		appmap_en.put("Lifestyle", CATEGORY_LIFE);
		appmap_en.put("Social", CATEGORY_SNS);
		appmap_en.put("Widgets", CATEGORY_WIDGET);
		appmap_en.put("Finance", CATEGORY_FINANCE);
		appmap_en.put("Shopping", CATEGORY_SHOPPING);
		appmap_en.put("Communication", CATEGORY_COMMUNICATION);
		appmap_en.put("Music & Audio", CATEGORY_MUSIC);
		appmap_en.put("Libraries & Demo", CATEGORY_COLLECT);
		appmap_en.put("Default", CATEGORY_DEFAULT);
				
		//game
		gamemap_en.put("Casual", CATEGORY_LEISURE);
		gamemap_en.put("Sports Games", CATEGORY_SPORTGAME);
		gamemap_en.put("Live Wallpaper", CATEGORY_GWALLPAPER);
		gamemap_en.put("Racing", CATEGORY_MATCH);
		gamemap_en.put("Arcade & Action", CATEGORY_GAME);
		gamemap_en.put("Brain & Puzzle", CATEGORY_longELLIGENCE);
		gamemap_en.put("Widgets", CATEGORY_GWIDGET);
		gamemap_en.put("Cards & Casino", CATEGORY_CARD);		
		
		//应用value-key
		Iterator iter = appmap.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			Long value = (Long)entry.getValue();
			appvaluekey.put(value, key);
		}
		
		//游戏value-key
		iter = gamemap.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			Long value = (Long)entry.getValue();
			gamevaluekey.put(value, key);
		}
		
		//application value-key
		iter = appmap_en.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			Long value = (Long)entry.getValue();
			appvaluekey_en.put(value, key);
		}
				
		//game value-key
		iter = gamemap_en.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			Long value = (Long)entry.getValue();
			gamevaluekey_en.put(value, key);
		}
	}
	
	public static long getCategoryidByAppName(String category, boolean isEn)
	{
		try
		{
			return isEn ? appmap_en.get(category) : appmap.get(category);
		}
		catch(Exception ex)
		{			
			return CATEGORY_DEFAULT;
		}
	}
	
	public static long getCategoryidByGameName(String category, boolean isEn)
	{
		try
		{
			return isEn ? gamemap_en.get(category) : gamemap.get(category);
		}
		catch(Exception ex)
		{
			return CATEGORY_DEFAULT;
		}
	}
	
	public static String getAppNameByCategoryid(long id, boolean isEn)
	{
		try
		{
			return isEn ? appvaluekey_en.get(id) : appvaluekey.get(id);
		}
		catch(Exception ex)
		{
			return isEn ? "Default" : "其它";
		}
	}
	
	public static String getGameNameByCategoryid(long id, boolean isEn)
	{
		try
		{
			return isEn ? gamevaluekey_en.get(id) : gamevaluekey.get(id);
		}
		catch(Exception ex)
		{
			return isEn ? "Default" : "其它";
		}
	}
}