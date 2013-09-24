package org.notifications.avro.server;

import com.borqs.information.rpc.service.IInformationsService;
import com.borqs.information.rpc.service.Info;
import com.borqs.information.rpc.service.SendInfo;
import com.borqs.information.rpc.service.StateResult;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.commons.lang.ObjectUtils;

import java.net.URI;
import java.util.List;
import java.util.UUID;


public class TestC {
    public static void main(String[] args) {
        for(int j = 0 ;j<10 ;j++)
            for(int i = 10;i<20 ;i++)
                genData(i+"");
    }

    private static void genData(String scene) {
        try {
            //URI uri = new URI("avro://10.200.60.49:8083");
            URI uri = new URI("avro://192.168.110.129:8083");
            Transceiver trans = Ipc.createTransceiver(uri);
            IInformationsService service = SpecificRequestor.getClient(IInformationsService.class, trans);

            // send a message
            String message = "{\"appId\":\"testapp\",\"senderId\":\"123\",\"receiverId\":\"10214\",\"title\":\"this is a test title\", \"type\":\"type1\"}";
            Info info = new Info();
            SendInfo sendInfo = new SendInfo();
            sendInfo.appId = "9";
            sendInfo.body="E/AndroidRuntime(18464): at android.os.Looper.loop(Looper.java:137)\n" +
                    "E/AndroidRuntime(18464): at android.app.ActivityThread.main(ActivityThread.java:4439)\n" +
                    "E/AndroidRuntime(18464): at java.lang.reflect.Method.invokeNative(Native Method)\n" +
                    "E/AndroidRuntime(18464): at java.lang.reflect.Method.invoke(Method.java:511)\n" +
                    "E/AndroidRuntime(18464): at";
            sendInfo.data="data";
            sendInfo.guid= UUID.randomUUID()+"";
            sendInfo.receiverId="10405";
            sendInfo.senderId="10001";
            sendInfo.objectId="2212";
            sendInfo.type="ntf.other_share";
            sendInfo.scene=scene;
            StateResult result = service.sendInfo(sendInfo);//.sendInf(info);

            List<Info> result3 = service.queryInfo("9","ntf.other_share","10405","222");

            String result2 = ObjectUtils.toString(service.query("9", "ntf.other_share", "10405", "222"));
            //String result2 = (String)service.query("1","ntf.other_share","234,2344,22222,233234","234");
//            System.out.println(result);

            // mark message state to 'processed'
            service.process("3");
            System.out.println(result);


            trans.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
