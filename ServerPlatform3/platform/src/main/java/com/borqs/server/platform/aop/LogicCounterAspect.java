package com.borqs.server.platform.aop;


import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.util.ClassHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ManagedResource(description = "Logic performance counter")
public class LogicCounterAspect extends CounterAspect {
    private Map<String, String> methodNamesCache = new ConcurrentHashMap<String, String>();

    public LogicCounterAspect() {
    }

    @Override
    protected String getCountName(ProceedingJoinPoint pjp) {
        String key = pjp.getSignature().toLongString();
        String name = methodNamesCache.get(key);
        if (name != null)
            return name;

        Method method = AspectHelper.getMethod(pjp);
        if (method != null) {
            Class clazz = method.getDeclaringClass();
            for (Class itfClass : clazz.getInterfaces()) {
                if (!ArrayUtils.contains(itfClass.getInterfaces(), Logic.class))
                    continue;

                Method superMethod = ClassHelper.getMethodNoThrow(itfClass, method.getName(), method.getParameterTypes());
                if (superMethod != null) {
                    name = getLogicMethodName(superMethod);
                    break;
                }
            }
        }

        if (name == null)
            name = key;

        methodNamesCache.put(key, name);
        return name;
    }

    private static String getLogicMethodName(Method itfMethod) {
        Class clazz = itfMethod.getDeclaringClass();
        int sameCount = 0;
        for (Method m : clazz.getDeclaredMethods()) {
            if (StringUtils.equals(m.getName(), itfMethod.getName()))
                sameCount++;
        }

        StringBuilder buff = new StringBuilder();
        buff.append(clazz.getSimpleName()).append(".").append(itfMethod.getName());
        if (sameCount > 1) {
            buff.append("(");
            Class[] paramTypes = itfMethod.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                Class pt = paramTypes[i];
                if (i > 0)
                    buff.append(",");
                buff.append(pt.getSimpleName());
            }
            buff.append(")");
        }

        return buff.toString();
    }
}
