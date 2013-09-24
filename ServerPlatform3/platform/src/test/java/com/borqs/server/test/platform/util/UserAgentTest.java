package com.borqs.server.test.platform.util;


import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.UserAgent;
import junit.framework.TestCase;

public class UserAgentTest extends TestCase {



    public void testUserAgent() {
        UserAgent ua = UserAgent.parse("Mozilla/5.0 (Windows NT 5.1) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.162 Safari/535.19");
        System.out.println(JsonHelper.toJson(ua, true));
    }

}
