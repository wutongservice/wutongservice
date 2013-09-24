package com.borqs.server.wutong.task;

import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQ;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.util.ProcessUtils;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.action.ActionLogic;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.plexus.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class ActionMQReceiver {
    private static final Logger L = Logger.getLogger(ActionMQReceiver.class);

    public static void main(String[] args) throws IOException {
        try {
            String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
            //String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
            if ((args != null) && (args.length > 0)) {
                confPath = args[0];
            }
            GlobalConfig.loadFiles(confPath);
            GlobalLogics.init();


            ActionLogic actionLogic = GlobalLogics.getAction();
            Context ctx = new Context();
            RecordSet rs = actionLogic.getAllActionConfigs(ctx);
            Map<String, Record> map = rs.toRecordMap("id");


            MQCollection.initMQs();
            MQ mq = MQCollection.getMQ("platform");

            //pid
            String pidDirStr = FileUtils.getUserDirectoryPath() + "/.bpid";
            File pidDir = new File(pidDirStr);
            if (!pidDir.exists()) {
                FileUtils.forceMkdir(pidDir);
            }
            ProcessUtils.writeProcessId(pidDirStr + "/mail_action_receiver.pid");
            while (true) {
                String json = mq.receiveBlocked("action");
                String jsonCode = URLEncoder.encode(json, "UTF-8");
                JsonNode jn = JsonUtils.parse(json);
                String appData = jn.path("app_data").getTextValue();
                JsonNode node = JsonHelper.parse(appData);
                String id = node.path("tag_id").getTextValue();
                //根据不同的类型，来调用不同的业务逻辑
                if (map.containsKey(id)) {
                    String url = map.get(id).getString("url");
                    String submitType = map.get(id).getString("submit_type");
                    if(StringUtils.equalsIgnoreCase("POST",submitType))
                        submitPost(url, "jn=" + jsonCode);
                    else if(StringUtils.equalsIgnoreCase("GET",submitType))
                        submitGet(url);
                }

            }
        } finally {
            MQCollection.destroyMQs();
        }
    }

    public static StringBuffer submitPost(String url, String paramContent) {
        StringBuffer responseMessage = null;
        java.net.URLConnection connection = null;
        java.net.URL reqUrl = null;
        OutputStreamWriter reqOut = null;
        InputStream in = null;
        BufferedReader br = null;
        String param = paramContent;
        try {
            responseMessage = new StringBuffer();
            reqUrl = new java.net.URL(url);
            connection = reqUrl.openConnection();
            connection.setDoOutput(true);
            reqOut = new OutputStreamWriter(connection.getOutputStream());
            reqOut.write(param);
            reqOut.flush();
            int charCount = -1;
            in = connection.getInputStream();

            br = new BufferedReader(new InputStreamReader(in, "GBK"));
            while ((charCount = br.read()) != -1) {
                responseMessage.append((char) charCount);
            }
        } catch (Exception ex) {
            L.info(null, "url=" + url + "?" + paramContent + "\n e=" + ex);
        } finally {
            try {
                in.close();
                reqOut.close();
            } catch (Exception e) {
                L.info(null, "url=" + url + "?" + paramContent + "\n e=" + e);
            }
        }
        return responseMessage;
    }


    public static String submitGet(String strUrl) {
        URLConnection connection = null;
        BufferedReader reader = null;
        String str = null;
        try {
            System.out.println("send getmethod=" + strUrl);
            URL url = new URL(strUrl);
            connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(false);
            // 取得输入流，并使用Reader读取
            reader = new BufferedReader(new InputStreamReader(connection
                    .getInputStream()));
            System.out
                    .println("============Contents of get request===============");
            String lines;
            StringBuffer linebuff = new StringBuffer("");
            while ((lines = reader.readLine()) != null) {
                linebuff.append(lines);
            }
            System.out.println(linebuff);
            System.out
                    .println("============Contents of get request ends==========");
            str = linebuff.toString();
        } catch (Exception e) {
            System.out.println("getmethod is err=" + e);
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return str;
    }
}