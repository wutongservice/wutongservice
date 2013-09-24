package com.borqs.server.base.util.email;

import com.borqs.server.base.util.threadpool.QueuedThreadPool;

public class ThreadPoolManager
{
	public static QueuedThreadPool threadpool;
    private static final int NetworkPoolSize=50;
    public static QueuedThreadPool getThreadPool()
    {
        synchronized(QueuedThreadPool.class)
        {
            if(null == threadpool){
                threadpool = new QueuedThreadPool(NetworkPoolSize);
                threadpool.setName("Async-thread-pool");
                try 
                {
                    threadpool.start();
                } catch (Exception e) {}
                
                Runtime.getRuntime().addShutdownHook(new Thread() 
                {
                    public void run() 
                    {
                        stopThreadPool();
                    }
                });
            }
        }
        return threadpool;
    }
    
    public static void stopThreadPool()
    {
        if(threadpool != null)
            try {
                threadpool.stop();
            } catch (Exception e) {}
    }
}