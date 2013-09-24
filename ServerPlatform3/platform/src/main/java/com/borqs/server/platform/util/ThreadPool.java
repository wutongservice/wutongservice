package com.borqs.server.platform.util;


import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPool implements Executor, Initializable {

    private static final int DEFAULT_CLOSE_AWAIT_SECONDS = 20;

    private ExecutorService pool;
    private int threadCount;
    private int closeAwaitSeconds = DEFAULT_CLOSE_AWAIT_SECONDS;

    public ThreadPool() {
        this(1);
    }

    public ThreadPool(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getCloseAwaitSeconds() {
        return closeAwaitSeconds;
    }

    public void setCloseAwaitSeconds(int closeAwaitSeconds) {
        this.closeAwaitSeconds = closeAwaitSeconds >= 1 ? closeAwaitSeconds : DEFAULT_CLOSE_AWAIT_SECONDS;
    }

    @Override
    public void init() throws Exception {
        if (threadCount <= 0)
            pool = Executors.newCachedThreadPool();
        else if (threadCount == 1)
            pool = Executors.newSingleThreadExecutor();
        else // threadCount > 1
            pool = Executors.newFixedThreadPool(threadCount);
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool, int closeAwaitSeconds) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(closeAwaitSeconds, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(closeAwaitSeconds, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        shutdownAndAwaitTermination(pool, closeAwaitSeconds);
    }

    @Override
    public void execute(Runnable command) {
        pool.execute(command);
    }
}
