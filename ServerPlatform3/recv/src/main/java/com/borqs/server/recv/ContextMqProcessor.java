package com.borqs.server.recv;



import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.platform.mq.MqProcessor;
import org.apache.commons.lang.ObjectUtils;

public abstract class ContextMqProcessor implements MqProcessor {

    private static final Logger L = Logger.get(ContextMqProcessor.class);

    protected ContextMqProcessor() {
    }

    @Override
    public void process(String queue, Object o) {
        if (!(o instanceof ContextObject)) {
            L.warn(null, "Received object is not a instance of ContextObject " + ObjectUtils.toString(o));
            return;
        }

        processContextObject(queue, (ContextObject)o);
    }

    protected abstract void processContextObject(String queue, ContextObject ctxObj);
}
