package com.borqs.server.platform.cache.tt;


import com.borqs.server.platform.cache.memcached.Memcached;
import net.rubyeye.xmemcached.transcoders.TokyoTyrantTranscoder;

public class TT extends Memcached {
    public TT() {
        setTranscoder(new TokyoTyrantTranscoder(1024 * 1024 * 48));
    }
}
