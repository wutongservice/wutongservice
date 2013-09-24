package com.borqs.notifications.mq.listener;

import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.borqs.information.util.ConfigPathUtil;

public class RedisMQListenerServiceLauncher {
	private DefaultMessageListenerContainer listenerContainer;
	private String[] contextPaths;
	
	public static class ShutdownHookThread extends Thread {
		private DefaultMessageListenerContainer listenerContainer;
		public ShutdownHookThread(DefaultMessageListenerContainer listenerContainer) {
			this.listenerContainer = listenerContainer;
		}
		
		@Override
		public void run() {
			if(null!=this.listenerContainer) {
				this.listenerContainer.stop();
			}
		}
		
	}
	
	public void init(String[] args) throws Exception {
		contextPaths = new String[] {
				"applicationContext.xml",
				"applicationContext-redis.xml",
				"applicationContext-push.xml"
				};
		contextPaths = ConfigPathUtil.absolutePaths(contextPaths);
		
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(contextPaths);
		listenerContainer = (DefaultMessageListenerContainer) context.getBean("redisMQContainer");
	}

	public void start() throws Exception {
		System.out.println("execute start method！");
		if(null != listenerContainer) {
	        listenerContainer.start();
	        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(listenerContainer));
		} else {
			System.err.println("failed to execute start method！");
		}
	}

	public void stop() throws Exception {
		System.out.println("execute stop method！");
		if(null != listenerContainer) {
			listenerContainer.stop();
		}
	}

	public void destroy() throws Exception {
		System.out.println("execute destroy method!");
	}
}
