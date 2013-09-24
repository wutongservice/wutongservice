package com.borqs.server.platform.util;


import java.util.List;

public interface PerformanceCounter {

    List<Count> getCounts();

    List<String> getNames();

    Count getCount(String name);

    public static class Count {
        public final String name;
        public final long beforeCount;
        public final long afterCount;
        public final long throwingCount;
        public final long elapsed;

        public Count(String name, long beforeCount, long afterCount, long throwingCount, long elapsed) {
            this.name = name;
            this.beforeCount = beforeCount;
            this.afterCount = afterCount;
            this.throwingCount = throwingCount;
            this.elapsed = elapsed;
        }

        @Override
        public String toString() {
            return String.format("%s {before:%s, after:%s, throwing:%s, elapsed:%s}", name, beforeCount, afterCount, throwingCount, elapsed);
        }
    }
}
