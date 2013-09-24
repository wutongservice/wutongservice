package com.borqs.server.wutong.task;


import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.log.Logger;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.notif.IInformationsService;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.lang.ObjectUtils;

import java.net.URI;

public class NotifTest {
    private static final Logger L = Logger.getLogger(NotifTest.class);

    public static void main(String[] args) {
        //String confPath = "/home/zhengwei/workWT/dist-r3-distribution/etc/test.config.properties";
        String confPath = "F:\\work\\refactProduct\\Dist\\src\\main\\etc\\test.config.properties";
        if ((args != null) && (args.length > 0)) {
            confPath = args[0];
        }
        GlobalConfig.loadFiles(confPath);
        GlobalLogics.init();

        Context ctx = new Context();
        try {
            URI uri = new URI("avro://10.200.60.49:8083");
            //URI uri = new URI("avro://192.168.110.129:8083");
            Transceiver trans = Ipc.createTransceiver(uri);
            IInformationsService service = SpecificRequestor.getClient(IInformationsService.class, trans);
            String users = GlobalConfig.get().getString("notif.test.user","10405,10001");
            String result3 = ObjectUtils.toString(service.query("1", "ntf.other_share", users, ""));
            System.out.println("result=====================" + result3);
            // mark message state to 'processed'
            service.process("3");
            trans.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
