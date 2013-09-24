package com.borqs.server.base.mq;


import com.borqs.server.base.util.Initializable;

public interface MQ extends Initializable {
    void send(String queue, String s);
    String receive(String queue);
    String receiveBlocked(String queue);

    void send(String queue, Object o);

    Object receiveBlockedObject(String queue);
}
