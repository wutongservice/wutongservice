package com.borqs.server.qiupu;


import com.borqs.server.base.log.TraceCallInterceptor;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.InitUtils;
import com.borqs.server.base.web.JettyServer;

public class QiupuLogics {

    private static QiupuLogic qiupu = (QiupuLogic) ClassUtils2.newInstance(QiupuImpl.class, TraceCallInterceptor.INSTANCE);


    public static void init() {
        InitUtils.init(qiupu);

    }

    public static void destroy() {
        InitUtils.destroy(qiupu);
    }

    public static QiupuLogic getQiubpu() {
        return qiupu;
    }

    public static class ServerLifeCycle implements JettyServer.LifeCycle {
        @Override
        public void before() throws Exception {
            QiupuLogics.init();
        }

        @Override
        public void after() throws Exception {
            QiupuLogics.destroy();
        }
    }
}
