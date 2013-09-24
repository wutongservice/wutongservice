package com.borqs.notifications.jms.listener;

import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.borqs.information.util.ConfigPathUtil;

public class JMSListenerServiceLauncher {
	private DefaultMessageListenerContainer jmsContainer;
	private String[] contextPaths;
	
	public static class ShutdownHookThread extends Thread {
		private DefaultMessageListenerContainer jmsContainer;
		public ShutdownHookThread(DefaultMessageListenerContainer jmsContainer) {
			this.jmsContainer = jmsContainer;
		}
		
		@Override
		public void run() {
			if(null!=this.jmsContainer) {
				this.jmsContainer.stop();
			}
		}
		
	}
	
	public void init(String[] args) throws Exception {
		contextPaths = new String[] {
				"applicationContext.xml",
				"applicationContext-jms.xml",
				"applicationContext-push.xml"
				};
		contextPaths = ConfigPathUtil.absolutePaths(contextPaths);
		
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(contextPaths);
		jmsContainer = (DefaultMessageListenerContainer) context.getBean("jmsContainer");
	}

	public void start() throws Exception {
		System.out.println("execute start method！");
		if(null != jmsContainer) {
	        jmsContainer.start();
	        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(jmsContainer));
		} else {
			System.err.println("failed to execute start method！");
		}
	}

	public void stop() throws Exception {
		System.out.println("execute stop method！");
		if(null != jmsContainer) {
			jmsContainer.stop();
		}
	}

	public void destroy() throws Exception {
		System.out.println("execute destroy method!");
	}
}
