package com.borqs.server.base.util;


public class InitUtils {
    public static void init(Object o) {
        if (o instanceof Initializable)
            ((Initializable) o).init();
    }

    public static void destroy(Object o) {
        if (o instanceof Initializable)
            ((Initializable) o).destroy();
    }

    public static void batchInit(Object... objs) {
        for (Object o : objs)
            init(o);
    }

    public static void batchDestroy(Object... objs) {
        for (Object o : objs)
            destroy(o);
    }
}
