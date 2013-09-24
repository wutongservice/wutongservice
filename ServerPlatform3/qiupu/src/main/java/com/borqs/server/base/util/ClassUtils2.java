package com.borqs.server.base.util;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassUtils2 {
    public static Class forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ClassException(e, "Not found class %s", className);
        }
    }

    public static Class forNameSafe(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Object newInstance(Class clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ClassException(e);
        } catch (IllegalAccessException e) {
            throw new ClassException(e);
        }
    }

    public static Object newInstance(Class clazz, Configuration args) {
        try {
            Constructor ctor;
            try {
                ctor = clazz.getConstructor(Configuration.class);
            } catch (NoSuchMethodException e) {
                ctor = clazz.getConstructor(Map.class);
            }
            return ctor.newInstance(args);
        } catch (InstantiationException e) {
            throw new ClassException(e);
        } catch (IllegalAccessException e) {
            throw new ClassException(e);
        } catch (NoSuchMethodException e) {
            throw new ClassException(e);
        } catch (InvocationTargetException e) {
            throw new ClassException(e);
        }
    }


    public static Object newInstance(String classNameWithInitArg) {
        JsonNode jn;
        try {
            jn = JsonUtils.parse(classNameWithInitArg);
        } catch (Exception e) {
            jn = null;
        }

        if (jn != null) {
            try {
                String className = jn.path("class").getTextValue();
                if (jn.has("args")) {
                    Configuration args = Configuration.loadJson(jn.path("args"));
                    return newInstance(forName(className), args);
                } else {
                    return newInstance(forName(className));
                }
            } catch (Exception e) {
                throw new ClassException("Create new instance where init arguments error", e);
            }
        } else {
            return newInstance(forName(classNameWithInitArg));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> newInstances(Class<T> clazz, String classNameWithInitArgList) {
        if (StringUtils.isBlank(classNameWithInitArgList)) {
            return (List<T>)(new ArrayList());
        }

        JsonNode jn;
        try {
            jn = JsonUtils.parse(classNameWithInitArgList);
        } catch (Exception e) {
            jn = null;
        }

        ArrayList l = new ArrayList();
        if (jn != null) {
            if (jn.isObject()) {
                l.add(newInstance(jn.toString()));
            } else if (jn.isArray()) {
                for (int i = 0; i < jn.size(); i++)
                    l.add(newInstance(jn.get(i).toString()));
            } else {
                throw new ClassException("Create new instance where init arguments error");
            }
        } else {
            List<String> classNames = StringUtils2.splitList(classNameWithInitArgList, ",", true);
            for (String className : classNames)
                l.add(newInstance(forName(className)));
        }

        for (Object o : l) {
            if (!(clazz.isAssignableFrom(o.getClass())))
                throw new ClassCastException();
        }

        return (List<T>)l;
    }

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Method getMethod(Class clazz, String method, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(method, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new ClassException(e);
        }
    }

    public static Method getMethodNoThrow(Class clazz, String method, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(method, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Object invoke(Method method, Object instance, Object... params) {
        try {
            return method.invoke(instance, params);
        } catch (Throwable t) {
            throw new ClassException(t);
        }
    }

    public static Object invokeNoThrow(Method method, Object instance, Object... params) {
        try {
            return method.invoke(instance, params);
        } catch (Throwable t) {
            return null;
        }
    }
}
