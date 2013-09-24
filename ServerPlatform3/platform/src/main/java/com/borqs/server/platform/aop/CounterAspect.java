package com.borqs.server.platform.aop;


import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.PerformanceCounter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public abstract class CounterAspect implements Advices.Around, PerformanceCounter {
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Map<String, CountEntity> entities = new HashMap<String, CountEntity>();
    private volatile boolean timing = true;

    protected CounterAspect() {
    }

    public boolean isTiming() {
        return timing;
    }

    public void setTiming(boolean timing) {
        this.timing = timing;
    }

    @Override
    public List<String> getNames() {
        try {
            rwl.readLock().lock();
            return new ArrayList<String>(entities.keySet());
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public List<Count> getCounts() {
        ArrayList<Count> counts = new ArrayList<Count>();
        for (String name : entities.keySet()) {
            Count count = getCount(name);
            if (count != null)
                counts.add(count);
        }
        return counts;
    }

    @ManagedAttribute(description = "Get all counts")
    public List<String> getCountsStrings() {
        ArrayList<String> l = new ArrayList<String>();
        for (Count count : getCounts()) {
            l.add(count.toString());
        }
        return l;
    }

    @Override
    public Count getCount(String name) {
        try {
            rwl.readLock().lock();
            CountEntity entity = entities.get(name);
            return entity != null
                    ? new Count(name, entity.beforeCount.get(), entity.afterCount.get(), entity.throwingCount.get(), entity.elapsed.get())
                    : null;
        } finally {
            rwl.readLock().unlock();
        }

    }

    @Override
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        String name = getCountName(pjp);
        CountEntity entity;

        try {
            rwl.writeLock().lock();
            entity = entities.get(name);
            if (entity == null) {
                entity = new CountEntity();
                entities.put(name, entity);
            }
        } finally {
            rwl.writeLock().unlock();
        }


        long start = 0;
        try {
            entity.beforeCount.incrementAndGet();
            start = timing ? DateHelper.nowMillis() : 0L;
            Object r = pjp.proceed();
            if (timing)
                entity.elapsed.addAndGet(DateHelper.nowMillis() - start);
            entity.afterCount.incrementAndGet();
            return r;
        } catch (Throwable t) {
            if (timing)
                entity.elapsed.addAndGet(DateHelper.nowMillis() - start);
            entity.throwingCount.incrementAndGet();
            throw t;
        }
    }

    protected abstract String getCountName(ProceedingJoinPoint pjp);

    private static class CountEntity {
        private final AtomicLong beforeCount = new AtomicLong(0);
        private final AtomicLong afterCount = new AtomicLong(0);
        private final AtomicLong throwingCount = new AtomicLong(0);
        private final AtomicLong elapsed = new AtomicLong(0);
    }
}
