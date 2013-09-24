package com.borqs.server.base.util.email;

import java.util.LinkedList;
import java.util.List;

public class DispatcherImpl implements Dispatcher{

	private ExecuteThread[] threads;
    private List<Runnable> q = new LinkedList<Runnable>();
    public DispatcherImpl() {
        threads = new ExecuteThread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ExecuteThread("UUCenter Async SendMail Dispatcher", this, i);
            threads[i].setDaemon(true);
            threads[i].start();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (active) {
                    shutdown();
                }
            }
        });
    }

    public synchronized void invokeLater(Runnable task) {
        synchronized (q) {
            q.add(task);
        }
        synchronized (ticket) {
            ticket.notify();
        }
    }
    Object ticket = new Object();
    public Runnable poll(){
        while(active){
            synchronized(q){
                if (q.size() > 0) {
                    Runnable task = q.remove(0);
                    if (null != task) {
                        return task;
                    }
                }
            }
            synchronized (ticket) {
                try {
                    ticket.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
        return null;
    }

    private boolean active = true;

    public synchronized void shutdown() {
        if (active) {
            active = false;
            for (ExecuteThread thread : threads) {
                thread.shutdown();
            }
            synchronized (ticket) {
                ticket.notify();
            }
        } else {
            throw new IllegalStateException("Already shutdown");
        }
    }
}

class ExecuteThread extends Thread {
    DispatcherImpl q;
    ExecuteThread(String name, DispatcherImpl q, int index) {
        super(name + "[" + index + "]");
        this.q = q;
    }

    public void shutdown() {
        alive = false;
    }

    private boolean alive = true;
    public void run() {
        while (alive) {
            Runnable task = q.poll();
            if (null != task) {
                try {
                    task.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
