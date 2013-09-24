package com.borqs.notifications.thrift.client;

import com.borqs.information.rpc.service.ThriftServiceLauncher4Test;
import com.borqs.notifications.thrift.INotificationsThriftService;
import com.borqs.notifications.thrift.Info;
import com.borqs.notifications.thrift.StateResult;
import junit.framework.Assert;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class NotificationsThriftClientTest {
	private static final String HOST = "127.0.0.1";
	private static final int PORT = 8084;
	
	TTransport transport;
	TProtocol protocol;
	
	
    ThriftServiceLauncher4Test launcher = new ThriftServiceLauncher4Test();
	Thread serviceThread = new Thread(new Runnable(){
		@Override
		public void run() {
			try {
				launcher.init(null);
				launcher.start();
				System.out.println("service is stop!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	});
	
	@Before
	public void init() throws Exception {
		serviceThread.start();
		while(true) {
			try {
				transport = new TSocket(HOST, PORT);
				protocol = new TBinaryProtocol(transport);
				transport.open();
				break;
			} catch (TTransportException e) {
				System.out.println("retry connect to thrift!");
//				e.printStackTrace();
//				throw new Exception(e);
			}
		}
	}
	
	@Test
	public void testList() {
		INotificationsThriftService.Client client = new INotificationsThriftService.Client(protocol);
		
		try {
			List<Info> result = client.listAllOfApp("10", "10208", "0", 0L, 10);
			Assert.assertNotNull(result);
			Assert.assertTrue(result.size() > 0);
			for(Info info : result) {
				System.out.println(info.getId());
			}
		} catch (TException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testSend() {
		INotificationsThriftService.Client client = new INotificationsThriftService.Client(protocol);
		Info info = new Info();
		info.setAction("thrift.test.action");
		info.setAppId("222");
		info.setBody("this is a thrift test body!");
		info.setBodyHtml("<div>this is html body!</div>");
		info.setDate(System.currentTimeMillis());
		info.setData("this is thrift data.");
		info.setGuid("thrift.guid.1");
		info.setLastModified(System.currentTimeMillis());
		info.setObjectId("thrift.objectid.1");
		info.setProcessed(false);
		info.setRead(false);
		info.setReceiverId("10214");
		info.setSenderId("10214");
		info.setTitle("this is a title!");
		info.setTitleHtml("<div>this is a title!</div>");
		info.setType("thrift.type");
		info.setUri("http://thrfit.uri");
//		info.setPush(true);
		try {
			StateResult res = client.sendInf(info);
			
			System.out.println(res.getStatus()+"->"+res.getMid());
		} catch (TException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@After
	public void destroy() {
		if(null != transport) {
			transport.close();
		}
		try {
			launcher.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
