package com.borqs.server.base.mq.kestrel;


import com.borqs.server.base.mq.memcacheq.MemcachedCompatible;

public class Kestrel extends MemcachedCompatible {
    public Kestrel(String configPrefix) {
        super(configPrefix);
    }
}
