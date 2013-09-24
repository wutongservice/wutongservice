package com.borqs.server.platform.counter;


public abstract class AbstractCounter implements Counter {
    protected AbstractCounter() {
    }

    @Override
    public long decrease(String key, long n, long init) {
        return increase(key, -n, init);
    }

    @Override
    public long increase(String key, long n) {
        return increase(key, n, 0);
    }

    @Override
    public long decrease(String key, long n) {
        return increase(key, -n, 0);
    }

    @Override
    public long increase(String key) {
        return increase(key, 1, 0);
    }

    @Override
    public long decrease(String key) {
        return increase(key, -1, 0);
    }
}
