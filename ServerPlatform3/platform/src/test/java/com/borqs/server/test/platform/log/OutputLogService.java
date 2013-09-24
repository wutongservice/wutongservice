package com.borqs.server.test.platform.log;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.log.LogCall;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.service.Service;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.ThreadHelper;

public class OutputLogService implements Service, Runnable {
    private static final Logger L = Logger.get(OutputLogService.class);

    private volatile long counter = 0;
    private Thread thread;
    private volatile boolean flag = true;

    @Override
    public void start() {
        if (thread == null) {
            counter = 0;
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void stop() {
        if (thread != null) {
            try {
                flag = false;
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            } finally {
                thread = null;
                flag = true;
            }
        }
    }

    @Override
    public boolean isStarted() {
        return thread != null;
    }

    @Override
    public void run() {
        while (flag) {
            long viewer = RandomHelper.randomSelect(10020, 10021, 10022);
            Context ctx = Context.createForViewer(viewer);
            ctx.setApp(1);
            ctx.setRemote("192.168.5.20");
            final LogCall LC = LogCall.startCall(L, OutputLogService.class, "run", ctx);
            L.debug(ctx, "Debug message" + counter);
            L.oper(ctx, "remove_friend", null);
            L.info(ctx, "Info message" + counter);
            L.warn(ctx, "Warn message" + counter);
            try {
                try {
                    throw new ServerException(E.CLASS, "ERELLL");
                } catch (ServerException e) {
                    throw new ServerException(E.CLASS, e, "ERELLL2");
                }
            } catch (Exception e) {
                L.error(ctx, e, "error message" + counter);
            }
            LC.endCall();
            counter++;

            ThreadHelper.sleepSilent(5000);
        }
    }
}
