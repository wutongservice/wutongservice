package com.borqs.server.test.platform.util;

import com.borqs.server.platform.cache.redis.Redis;
import com.borqs.server.platform.cache.redis.SingleRedis;
import com.borqs.server.platform.feature.account.NameInfo;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.sfs.ftp.FtpSFS;
import com.borqs.server.platform.sfs.local.LocalSFS;
import com.borqs.server.platform.sfs.vfs.VfsSFS;
import com.borqs.server.platform.util.*;
import com.borqs.server.platform.util.sender.email.AsyncMailSender;
import com.borqs.server.platform.util.sender.email.Mail;
import com.borqs.server.platform.util.sender.notif.AsyncNotifSender;
import com.borqs.server.platform.util.sender.notif.Notification;
import com.borqs.server.platform.util.sender.sms.AsyncProxyMessageSender;
import com.borqs.server.platform.util.sender.sms.Message;
import com.borqs.server.platform.web.WebHelper;
import junit.framework.TestCase;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class PipedTest extends TestCase  {
    public void testAA() throws Exception {
//        ThreadPool pool = new ThreadPool(2);
//        pool.init();
////        AsyncMailSender sender = new AsyncMailSender(pool);
////        sender.setSmtpAddress("smtp.bizmail.yahoo.com:465");
////        sender.setSmtpUsername("borqs.support@borqs.com");
////        sender.setSmtpPassword("borqsbpc");
////        sender.asyncSend(new Mail().setFrom("rongxin.gao@borqs.com").setTo("rongxin.gao@borqs.com").setSubject("test..").setMessage("HELLO TEST MAIL!"));
//        AsyncProxyMessageSender sender = new AsyncProxyMessageSender(pool);
//        sender.setHost("http://api.borqs.com/sync/smsserver/sendsms");
//        sender.asyncSend((Message.forSend("13810757927", "爱你，宝宝")));
//        pool.destroy();
        //System.out.println(SystemHelper.getPathInTempDir("eee/ff"));
        //String[] a = FeedbackParams.fromSegmentedBase64("spRujNhd5Mo2E6u4DfVQHltB23Ne1MB0cz71N/Sevi7plubKME/iqCaWCFXB56dvUrK9MSoyWD6l4pNwdJSfn3/x+kkN801VzMwmavGRVXk=", "/");
        //System.out.println(a.length);
        // A6011340DF8E01846E0873704DAEACE6
        //System.out.println(Encoders.md5Hex("303581"));
        //System.out.println(Encoders.md5Hex("123456"));


        //System.out.println(RandomHelper.getTimestamp(2796012750487696896L));
        //System.out.println(1333242774241L);

        //long ts = 1330587713255L;
        //System.out.println(ts);
        //System.out.println(RandomHelper.getTimestamp(RandomHelper.generateId(ts)));
        //System.out.println(2790444679414720280L);
        //System.out.println(RandomHelper.getTimestamp(2790444679414720280L));
        //System.out.println(RandomHelper.timestampToId(1330587713255L));
        //System.out.println(RandomHelper.timestampToId(1330587713255L) | (2097152 - 1));
        //SFS sfs = new VfsSFS("ftp://zhengwei:borqs.com@192.168.5.22/data3/photo");
        //SFS sfs = new FtpSFS("ftp://zhengwei:borqs.com@192.168.5.22/data3/photo");
        //System.out.println(sfs.exists("profile_10058_1341572817826_S.jpg"));
        //sfs.writeFile("ssss.jpg", "E:\\Documents\\My Pictures\\Wallpapers\\02002_theopallake_1280x1024.jpg");
//        SingleRedis redis = new SingleRedis();
//        redis.setServer("192.168.5.22:10017");
//        redis.init();
//        redis.open(new Redis.BinaryHandler() {
//            @Override
//            public Object handle(BinaryJedisCommands cmd) {
//
//            }
//        })
//        redis.destroy();
//        Jedis jedis = new Jedis("192.168.5.22", 10097);
//        jedis.subscribe(new JedisPubSub() {
//            @Override
//            public void onMessage(String channel, String message) {
//                System.out.println("channel=" + channel);
//                System.out.println(message);
//            }
//
//            @Override
//            public void onPMessage(String pattern, String channel, String message) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void onSubscribe(String channel, int subscribedChannels) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void onUnsubscribe(String channel, int subscribedChannels) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void onPUnsubscribe(String pattern, int subscribedChannels) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            @Override
//            public void onPSubscribe(String pattern, int subscribedChannels) {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//        }, "PlatformHook.onUserCreated", "PlatformHook.onUserDestroyed", "PlatformHook.onUserProfileChanged", "PlatformHook.onFriendshipChange");


//        String stopWords = "有限公司 股份有限公司 公司";
//        String s = "";
//        long start = System.currentTimeMillis();
//        //for (int i = 0; i < 1000000; i++) {
//            s = ChineseSegmentHelper.segmentNameString("姜长胜");
//        //}
//        System.out.println(System.currentTimeMillis() - start);
//        System.out.println(s);
        //System.out.println(WebHelper.getMimeTypeByFileExt(".PNG"));

//        Notification n = Notification.forSend("102", App.APP_NONE, "action2");
//        AsyncNotifSender sender = new AsyncNotifSender();
//        sender.setExecutor(new ThreadPool(1));
//        sender.setServer("192.168.7.144:8083");
//        String s = sender.syncSend(n);
//        System.out.println(s);

        URL url = new URL("http://localhost:9999/mmm?a=b&c=d");
        System.out.println(url.getPath());
        System.out.println(url.getQuery());
        System.out.println(url.getAuthority());
        //UrlEncoded query = new UrlEncoded();
        MultiMap<String> m = new MultiMap<String>();
        UrlEncoded.decodeTo("a=b&c=d&a=c", m, Charsets.DEFAULT);
        for (Map.Entry<String, Object> e : m.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue() + " " + e.getValue().getClass().getName());
        }
//        String s = URLDecoder.decode("a=b&c=d", Charsets.DEFAULT);
//        System.out.println(s);
    }
}
