package com.borqs.server.platform.app;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class GlobalSpringAppContext {
    static ApplicationContext instance;

    public static ApplicationContext getInstance() {
        return instance;
    }

    public static Object getBean(String name) throws BeansException {
        return instance.getBean(name);
    }

    public static Object getBean(String name, Class requiredType) throws BeansException {
        return instance.getBean(name, requiredType);
    }

    public static Object getBean(String name, Object[] args) throws BeansException {
        return instance.getBean(name, args);
    }

    public static boolean containsBean(String name) {
        return instance.containsBean(name);
    }
}
