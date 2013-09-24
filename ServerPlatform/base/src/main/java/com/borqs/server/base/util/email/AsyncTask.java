package com.borqs.server.base.util.email;

public abstract class AsyncTask implements Runnable {
    	AsyncTaskListener listener;
        Object[] args;        
        boolean stoped;
        public void setStoped(boolean stoped)
        {
        	this.stoped = stoped;
        }
        public boolean Stoped()
        {
        	return stoped;
        }
        public AsyncTask(AsyncTaskListener listener, Object[] args) 
        {           
            this.listener = listener;
            this.args = args;
        }

        public abstract void invoke(AsyncTaskListener listener,Object[] args);

        public void run() 
        {            
        	invoke(listener,args);
            listener = null;
            args = null;           
        }
    }