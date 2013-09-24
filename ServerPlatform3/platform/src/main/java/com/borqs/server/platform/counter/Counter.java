package com.borqs.server.platform.counter;


public interface Counter {
    long increase(String key, long n, long init);
    long decrease(String key, long n, long init);

    long increase(String key, long n);
    long decrease(String key, long n);

    long increase(String key);
    long decrease(String key);

    long get(String key);
}
