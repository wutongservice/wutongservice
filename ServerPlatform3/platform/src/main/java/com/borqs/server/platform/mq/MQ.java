package com.borqs.server.platform.mq;


public interface MQ {
    void send(String queue, Object o);
    Object receive(String queue);
}
