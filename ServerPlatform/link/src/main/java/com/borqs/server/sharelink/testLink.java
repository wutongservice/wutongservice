package com.borqs.server.sharelink;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import org.apache.commons.lang.ArrayUtils;

public class testLink {


    public static void main(String[] args) {
        try {
//             SimpleShareLink simpleShareLink = new SimpleShareLink();
//              simpleShareLink.saveLink("news.sina.com","artibody","","","pagelogo_img",0);

            GenericTransceiverFactory tf = new GenericTransceiverFactory();

            String path = Constants.confPath;
            if (ArrayUtils.isNotEmpty(args))
                path = args[0];

            Configuration conf = Configuration.loadFiles(path).expandMacros();
            tf.setConfig(conf);
            tf.init();
            Sharelink s = new Sharelink(tf, conf);

            String url = "http://www.aqee.net/this-photograph-is-not-free/";
            String linkImagAddr = "D:\\2workspace\\images\\";
//            RecordSet rs = s.shareLink(url, linkImagAddr);
//            System.out.println(rs.getFirstRecord().getString("title") + "\n");
//            System.out.println(rs.getFirstRecord().getString("url") + "\n");
//            System.out.println(rs.getFirstRecord().getString("host") + "\n");
//            System.out.println(rs.getFirstRecord().getString("description") + "\n");
//            System.out.println(rs.getFirstRecord().getString("img_url") + "\n");
           s.shareHtmlLink("http://news.sina.com.cn/c/2012-06-28/122024674600.shtml", "");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
