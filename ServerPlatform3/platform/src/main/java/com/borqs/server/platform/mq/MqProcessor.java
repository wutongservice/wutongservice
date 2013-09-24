package com.borqs.server.platform.mq;


public interface MqProcessor {
    void process(String queue, Object o);
}
