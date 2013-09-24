package com.borqs.server.platform.aop;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

public class AspectHelper {
    public static Method getMethod(JoinPoint jp) {
        Signature sig = jp.getSignature();
        return sig instanceof MethodSignature ? ((MethodSignature) sig).getMethod() : null;
    }
}
