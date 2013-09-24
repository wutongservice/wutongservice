package com.borqs.server.platform.mq.receiver;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UserGetPerhapsName {
    private static final Logger L = LoggerFactory.getLogger(UserGetPerhapsName.class);

    public static void main(String[] args) throws IOException {
        try {

            GenericTransceiverFactory tf = new GenericTransceiverFactory();

			String confPath = "/home/wutong/work2/dist/etc/test_web_server.properties";
//            String confPath = "file://D:\\2workspace\\BorqsServerPlatform2\\mytest\\src\\main\\java\\company\\test\\PlatformWebServerTest.properties";

            if ((args != null) && (args.length > 0)) {
                confPath = args[0];
            }
//			Configuration conf = Configuration.loadFiles("/home/b516/BorqsServerPlatform2/test/src/test/MQReceiver.properties").expandMacros();
            Configuration conf = Configuration.loadFiles(confPath).expandMacros();
            tf.setConfig(conf);
            tf.init();
            final Platform p = new Platform(tf);
            p.setConfig(conf);

            RecordSet recs = p.findAllUserIds(true);

            for (Record rec : recs) {
                String user_id = rec.getString("user_id");
                String perhapsName = p.getPerhapsName(p.formatUrl(user_id));
//                System.out.println(user_id + "=" + perhapsName);
                Record r = new Record();
                r.put("perhaps_name",perhapsName);
                p.updateAccount(user_id,r,"");
            }
        } finally {
            MQCollection.destroyMQs();
        }
    }


}