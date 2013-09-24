package com.borqs.server.platform.mq;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.io.RW;
import com.borqs.server.platform.io.Writable;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ContextObject implements Writable{
    public static final int TYPE_CREATE = 1;
    public static final int TYPE_UPDATE = 2;
    public static final int TYPE_DESTROY = 3;

    public Context context;
    public int type;
    public Object object;

    public ContextObject() {
    }

    public ContextObject(Context context, int type, Object object) {
        this.context = context != null ? context.copy() : null;
        this.type = type;
        this.object = object;
    }

    public void sendThisWith(MQ mq, String queue) {
        mq.send(queue, this);
    }

    public void sendThisWith(QueueName queue) {
        sendThisWith(queue.getQueue(), queue.getName());
    }

    @Override
    public void write(Encoder out, boolean flush) throws IOException {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("context", context);
        m.put("type", type);
        m.put("object", object);
        RW.write(out, m, flush);
    }

    @Override
    public void readIn(Decoder in) throws IOException {
        Map<String, Object> m = (Map<String, Object>) RW.read(in);
        context = (Context)m.get("context");
        type = (Integer)m.get("type");
        object = (Object)m.get("object");
    }
}
