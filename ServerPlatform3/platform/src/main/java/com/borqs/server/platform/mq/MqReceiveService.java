package com.borqs.server.platform.mq;


import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.service.Service;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.ThreadHelper;
import org.apache.commons.lang.StringUtils;

import java.util.concurrent.Executor;

public class MqReceiveService implements Service, Initializable {
    private static final Logger L = Logger.get(MqReceiveService.class);

    private String name = "";
    private MQ mq;
    private volatile boolean stop = false;
    private volatile Thread receiveThread;
    private Executor workThreadPool;
    private String queue;
    private MqProcessor processor;


    public MqReceiveService() {
    }

    public MqReceiveService(String name, MQ mq, Executor workThreadPool, String queue, MqProcessor processor) {
        this.name = name;
        this.mq = mq;
        this.workThreadPool = workThreadPool;
        this.queue = queue;
        this.processor = processor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MQ getMq() {
        return mq;
    }

    public void setMq(MQ mq) {
        this.mq = mq;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public MqProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(MqProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void init() throws Exception {
        if (StringUtils.isEmpty(name))
            name = RandomHelper.generateUuidString();

        if (queue == null)
            throw new IllegalStateException("Missing queue");
    }

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void start() {
        if (isStarted())
            return;

        receiveThread = new Thread(new ReceiveWorker(), "ReceiveService:" + name);
        receiveThread.start();
        stop = false;
    }

    @Override
    public void stop() {
        try {
            if (receiveThread != null && !stop) {
                stop = true;
                while (stop) {
                    ThreadHelper.sleepSilent(50);
                }
                try {
                    receiveThread.join();
                } catch (InterruptedException ignored) {
                }
            }

        } finally {
            stop = false;
            receiveThread = null;
        }
    }

    @Override
    public boolean isStarted() {
        return receiveThread != null && !stop;
    }

    private class ReceiveWorker implements Runnable {

        void processReceived(Object o) {
            try {
                MqReceiveService.this.process(o);
            } catch (Exception e) {
                L.warn(null, "Process received object error", e);
            }
        }

        @Override
        public void run() {
            Executor exec = workThreadPool;
            MQ mq = MqReceiveService.this.mq;
            String queue = MqReceiveService.this.queue;

            try {
                while (!MqReceiveService.this.stop) {
                    try {
                        final Object o = mq.receive(queue);
                        if (o != null) {
                            if (exec != null) {
                                exec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        processReceived(o);
                                    }
                                });
                            } else {
                                processReceived(o);
                            }
                        }
                    } catch (Throwable t) {
                        L.warn(null, "Receive error", t);
                    }
                }
            } finally {
                MqReceiveService.this.stop = false;
            }
        }
    }

    protected void process(Object o) {
        //L.debug(null, "Receive object" + ObjectUtils.toString(o));
        if (processor != null)
            processor.process(queue, o);
    }
}
