package com.borqs.server.platform.util;


import com.borqs.server.platform.log.Logger;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskExecutor implements Executor, Initializable {
    private static final Logger L = Logger.get(TaskExecutor.class);

    private int threadCount = 0;
    private ExecutorService executorService;

    public TaskExecutor() {
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public synchronized void init() {
        if (executorService != null)
            return;

        if (threadCount == 1) {
            executorService = Executors.newSingleThreadExecutor();
        } else if (threadCount > 1) {
            executorService = Executors.newFixedThreadPool(threadCount);
        } else { // threadCount <= 0
            executorService = Executors.newCachedThreadPool();
        }
    }

    @Override
    public synchronized void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                        L.warn(null, "Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executorService = null;
        }
    }

    @Override
    public void execute(Runnable task) {
        executorService.execute(task);
    }
}
