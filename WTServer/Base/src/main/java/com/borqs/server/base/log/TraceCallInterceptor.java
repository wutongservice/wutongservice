package com.borqs.server.base.log;


import com.borqs.server.base.context.Context;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TraceCallInterceptor implements MethodInterceptor {

    public static final TraceCallInterceptor INSTANCE = new TraceCallInterceptor();

    protected TraceCallInterceptor() {
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        TraceCall traceCall = method.getAnnotation(TraceCall.class);
        if (traceCall == null) {
            return methodProxy.invokeSuper(o, args);
        } else {
            Class clazz = o.getClass().getSuperclass();
            Logger L = null;
            try {
                Field f = clazz.getDeclaredField("L");
                f.setAccessible(true);
                L = (Logger) f.get(null);
            } catch (Exception ignored) {
            }


            String methodName = method.getName();
            Context ctx = null;
            if (args.length > 0 && args[0] instanceof Context) {
                ctx = (Context) args[0];
            }

            if (L != null) {
                Object[] args1;
                if (args.length > 0) {
                    args1 = ArrayUtils.subarray(args, 1, args.length);
                } else {
                    args1 = new Object[]{};
                }
                L.traceStartCall(ctx, methodName, args1);
            }

            Object r = methodProxy.invokeSuper(o, args);

            if (L != null)
                L.traceEndCall(ctx, methodName);
            return r;
        }
    }
}
