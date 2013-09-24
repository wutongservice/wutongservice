package com.borqs.server.platform.mq;


import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryMQ implements MQ {
    private final Map<String, BlockingQueue<Object>> queues = new HashMap<String, java.util.concurrent.BlockingQueue<Object>>();
    private final ReadWriteLock QUEUE_LOCK = new ReentrantReadWriteLock();

    public MemoryMQ() {
    }

    private BlockingQueue<Object> getQueue(String q) {
        try {
            QUEUE_LOCK.readLock().lock();
            return queues.get(q);
        } finally {
            QUEUE_LOCK.readLock().unlock();
        }
    }

    private BlockingQueue<Object> ensureQueue(String q) {
        try {
            QUEUE_LOCK.writeLock().lock();
            BlockingQueue<Object> queue = queues.get(q);
            if (queue == null) {
                queue = new LinkedBlockingQueue<Object>(Integer.MAX_VALUE);
                queues.put(q, queue);
            }
            return queue;
        } finally {
            QUEUE_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void send(String queue, Object o) {
        Validate.notNull(queue);
        BlockingQueue<Object> q = ensureQueue(queue);
        try {
            q.put(o);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object receive(String queue) {
        BlockingQueue<Object> q = getQueue(queue);
        return q != null ? q.poll() : null;
    }
}
