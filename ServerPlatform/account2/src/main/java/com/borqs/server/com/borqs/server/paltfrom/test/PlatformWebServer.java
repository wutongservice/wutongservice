package com.borqs.server.com.borqs.server.paltfrom.test;
import com.borqs.server.base.web.JettyServer;

/**
 * Created by IntelliJ IDEA.
 * User: wangpeng
 * Date: 12-3-21
 * Time: 下午5:24
 * To change this template use File | Settings | File Templates.
 */
public class PlatformWebServer {
    public static void main(String[] args) throws Exception {
            JettyServer.main(new String[]{"-c", "classpath://com/borqs/server/com/borqs/server/paltfrom/test/PlatformWebServer.properties"});
        }

}
