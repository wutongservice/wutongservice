package com.borqs.server.wutong.shorturl;


public interface ShortUrlLogic {
    String findLongUrl0(String short_url);

    boolean saveShortUrl0(String long_url, String short_url);
    String generalShortUrl(String long_url);
    String getLongUrl(String short_url);

    String generalShortUrlWithExpired(String long_url, String key);
}