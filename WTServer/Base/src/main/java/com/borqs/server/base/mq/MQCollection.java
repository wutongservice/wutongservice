package com.borqs.server.base.mq;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.mq.memcacheq.MemcacheQ;
import com.borqs.server.base.util.Initializable;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MQCollection {
    private static volatile Map<String, MQ> MQS;

    public static void initMQs() {
        if (MQS != null)
            return;

        Configuration conf = GlobalConfig.get();
        HashMap<String, MemcacheQ> mqs = new HashMap<String, MemcacheQ>();

        Set<String> keys = conf.keySet();
        for (String key : keys) {
            if (key.startsWith("mq.")) {
                MemcacheQ mq = new MemcacheQ(key);
                mqs.put(StringUtils.removeStart(key, "mq."), mq);
            }

        }

        for (MemcacheQ mq : mqs.values()) {
            if (mq != null) {
                mq.init();
            }
        }

        MQS = new HashMap<String, MQ>(mqs);
    }

    public static void destroyMQs() {
        if (MQS != null) {
            try {
                for (MQ mq : MQS.values()) {
                    if (mq instanceof Initializable)
                        ((Initializable) mq).destroy();
                }
                MQS.clear();
            } finally {
                MQS = null;
            }
        }
    }

    public static MQ getMQ(String name) {
        return MapUtils.isNotEmpty(MQS) ? MQS.get(name) : null;
    }
}
