package com.borqs.server.platform.aop;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

public class Advices {
    public static interface Before {
        void before(JoinPoint jp);
    }

    public static interface After {
        void after(JoinPoint jp);
    }

    public static interface Around {
        Object around(ProceedingJoinPoint pjp) throws Throwable;
    }

    public static interface Throwing {
        void afterThrowing(JoinPoint jp, Throwable t);
    }
}
