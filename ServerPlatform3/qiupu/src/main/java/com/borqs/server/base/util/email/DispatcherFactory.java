package com.borqs.server.base.util.email;

public final class DispatcherFactory {
 
    public DispatcherFactory() {
        
    }

    /**
     * returns a Dispatcher instance.
     *
     * @return dispatcher instance
     */
    public Dispatcher getInstance() {
    	return new DispatcherImpl();
    }
}
