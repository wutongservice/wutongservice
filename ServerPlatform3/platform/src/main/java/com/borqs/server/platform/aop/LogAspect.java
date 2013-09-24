package com.borqs.server.platform.aop;


import com.borqs.server.platform.log.Logger;
import org.aspectj.lang.JoinPoint;

public abstract class LogAspect implements Advices.Before, Advices.After, Advices.Throwing {
    private static final Logger L = Logger.get(LogAspect.class);

    @Override
    public void after(JoinPoint jp) {
        doLog(jp);
    }

    @Override
    public void before(JoinPoint jp) {
        doLog(jp);
    }

    @Override
    public void afterThrowing(JoinPoint jp, Throwable t) {
        doLog(jp);
    }

    protected void doLog(JoinPoint jp) {
        Logger l = getLogger(jp);
        log(l, jp);
    }

    protected Logger getLogger(JoinPoint jp) {
        return L;
    }

    protected abstract void log(Logger l, JoinPoint jp);
}
