package com.borqs.server.platform.mq;


public class QueueName {
    private MQ queue;
    private String name;

    public QueueName() {
        this(null, null);
    }

    public QueueName(MQ queue, String name) {
        this.queue = queue;
        this.name = name;
    }

    public MQ getQueue() {
        return queue;
    }

    public void setQueue(MQ queue) {
        this.queue = queue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
