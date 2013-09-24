package com.borqs.server.platform.cache.ehcache;


import com.borqs.server.platform.cache.AbstractCache;
import com.borqs.server.platform.cache.CacheElement;
import com.borqs.server.platform.util.Initializable;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.commons.lang.Validate;

public class Ehcache extends AbstractCache implements Initializable {
    private volatile net.sf.ehcache.Cache cache;
    private final CacheConfiguration config = new CacheConfiguration();

    static {
        CacheManager.create();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                CacheManager.getInstance().shutdown();
            }
        });
    }

    public Ehcache() {
        config.setName("cache_" + System.identityHashCode(this));
        setupDefaultCacheConfig(config);
    }

    private static void setupDefaultCacheConfig(CacheConfiguration cc) {
        // In Memory Mode
        cc.maxElementsInMemory(1000);
        cc.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU);
        cc.setOverflowToDisk(false);
        cc.eternal(true);
        cc.diskPersistent(false);
    }

    public void setOverflowToOffHeap(boolean overflowToOffHeap) {
        config.setOverflowToOffHeap(overflowToOffHeap);
    }

    public void setMaxMemoryOffHeap(String maxMemoryOffHeap) {
        config.setMaxMemoryOffHeap(maxMemoryOffHeap);
    }

    public void setMaxElementsInMemory(int maxElementsInMemory) {
        config.setMaxElementsInMemory(maxElementsInMemory);
    }

    public void setCacheLoaderTimeoutMillis(long cacheLoaderTimeoutMillis) {
        config.setCacheLoaderTimeoutMillis(cacheLoaderTimeoutMillis);
    }

    public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        config.setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
    }

    public void setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        config.setMemoryStoreEvictionPolicyFromObject(memoryStoreEvictionPolicy);
    }

    public void setClearOnFlush(boolean clearOnFlush) {
        config.setClearOnFlush(clearOnFlush);
    }

    public void setEternal(boolean eternal) {
        config.setEternal(eternal);
    }

    public void setTimeToIdleSeconds(long timeToIdleSeconds) {
        config.setTimeToIdleSeconds(timeToIdleSeconds);
    }

    public void setTimeToLiveSeconds(long timeToLiveSeconds) {
        config.setTimeToLiveSeconds(timeToLiveSeconds);
    }

    public void setOverflowToDisk(boolean overflowToDisk) {
        config.setOverflowToDisk(overflowToDisk);
    }

    public void setDiskPersistent(boolean diskPersistent) {
        config.setDiskPersistent(diskPersistent);
    }

    public void setDiskStorePath(String diskStorePath) {
        config.setDiskStorePath(diskStorePath);
    }

    public void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        config.setDiskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
    }

    public void setDiskAccessStripes(int stripes) {
        config.setDiskAccessStripes(stripes);
    }

    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        config.setMaxElementsOnDisk(maxElementsOnDisk);
    }

    public void setDiskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        config.setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
    }

    public boolean isOverflowToOffHeap() {
        return config.isOverflowToOffHeap();
    }

    public int getMaxElementsInMemory() {
        return config.getMaxElementsInMemory();
    }

    public long getCacheLoaderTimeoutMillis() {
        return config.getCacheLoaderTimeoutMillis();
    }

    public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return config.getMemoryStoreEvictionPolicy();
    }

    public boolean isClearOnFlush() {
        return config.isClearOnFlush();
    }

    public boolean isEternal() {
        return config.isEternal();
    }

    public long getTimeToIdleSeconds() {
        return config.getTimeToIdleSeconds();
    }

    public long getTimeToLiveSeconds() {
        return config.getTimeToLiveSeconds();
    }

    public boolean isOverflowToDisk() {
        return config.isOverflowToDisk();
    }

    public boolean isDiskPersistent() {
        return config.isDiskPersistent();
    }

    public String getDiskStorePath() {
        return config.getDiskStorePath();
    }

    public int getDiskSpoolBufferSizeMB() {
        return config.getDiskSpoolBufferSizeMB();
    }

    public int getDiskAccessStripes() {
        return config.getDiskAccessStripes();
    }

    public int getMaxElementsOnDisk() {
        return config.getMaxElementsOnDisk();
    }

    public long getDiskExpiryThreadIntervalSeconds() {
        return config.getDiskExpiryThreadIntervalSeconds();
    }

    @Override
    public void init() throws Exception {
        if (cache != null)
            throw new IllegalStateException();

        cache = new net.sf.ehcache.Cache(config);
        CacheManager.getInstance().addCache(cache);
    }

    @Override
    public void destroy() {
        if (cache != null) {
            cache.dispose();
            CacheManager.getInstance().removeCache(config.getName());
            cache = null;
        }
    }

    public Cache getEhcache() {
        return cache;
    }

    @Override
    public void put(CacheElement value) {
        Validate.notNull(value);
        Element elem = value.hasExpiry()
                ? new Element(value.getKey(), value.getValue(), false, 0, (int) value.getExpirySeconds())
                : new Element(value.getKey(), value.getValue(), true, 0, 0);
        cache.put(elem);
    }

    @Override
    public CacheElement get(String key) {
        Element e = cache.get(key);
        return CacheElement.forResult(key, e != null ? e.getObjectValue() : null);
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }
}
