package com.borqs.notifications.avro.server;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.borqs.information.rpc.service.AvroIpcServiceProxy;

public class AvroServiceLauncher {
	private AvroIpcServiceProxy proxy;
	private String[] contextPaths;
	
	public static class ShutdownHookThread extends Thread {
		private AvroIpcServiceProxy proxy;
		public ShutdownHookThread(AvroIpcServiceProxy proxy) {
			this.proxy = proxy;
		}
		
		@Override
		public void run() {
			if(null!=this.proxy) {
				this.proxy.stop();
			}
		}
		
	}

	public void init(String[] args) throws Exception {
		System.out.println("execute init(args) method");
		
		URL location = AvroServiceLauncher.class.getProtectionDomain().getCodeSource().getLocation();
		String path = location.getPath();
		System.out.println("class location is:"+path);
		
		URI uri = new File(path).getParentFile().getParentFile().toURI();
		System.out.println("application location is:"+uri);

		contextPaths = new String[] {
				uri+"/conf/applicationContext.xml",
				uri+"/conf/applicationContext-dao.xml",
				uri+"/conf/applicationContext-jms.xml",
				uri+"/conf/applicationContext-push.xml",
				uri+"/conf/applicationContext-rpc.xml"
				};
		
		FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(contextPaths);
		proxy = (AvroIpcServiceProxy) context.getBean("informationsAvroIpcServiceProxy");
	}

	public void start() throws Exception {
		System.out.println("execute start method！");
		if(null != proxy) {
	        proxy.start();
	        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(proxy));
		} else {
			System.err.println("failed to execute start method！");
		}
	}

	public void stop() throws Exception {
		System.out.println("execute stop method！");
		if(null != proxy) {
			proxy.stop();
		}
	}

	public void destroy() throws Exception {
		System.out.println("execute destroy method!");
	}
}
