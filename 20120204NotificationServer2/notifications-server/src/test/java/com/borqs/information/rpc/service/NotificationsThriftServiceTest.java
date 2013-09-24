package com.borqs.information.rpc.service;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.notifications.thrift.INotificationsThriftService;
import com.borqs.notifications.thrift.Info;
import com.borqs.notifications.thrift.StateResult;
import junit.framework.Assert;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class NotificationsThriftServiceTest {
	private static final String DEFAULT_SENDER_ID = "10208";
	
	private static final String HOST = "192.168.110.129";
	private static final int PORT = 8084;

	TTransport transport;
	TProtocol protocol;

	INotificationsThriftService.Client service;
	
	@Test
	public void testSendAndProcessedInfo()
			throws Exception {
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("updated title.");
		info.setType("avro.test.testSendAndProcessedInfo");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testSendAndProcessedInfo_objectId");
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
		
		state = service.markProcessed(state.getMid());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->markProcessed->"+state.getMid()+","+state.getStatus());
	}

	@Test
	public void testSendAndReadInfo()
			throws Exception {
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testSendAndReadedInfo title.");
		info.setType("avro.test.testSendAndReadedInfo");
//		info.setProcessMethod(2);
//		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a testSendAndReadedInfo data");
		info.setObjectId("testSendAndProcessedInfo_objectId");
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
		
		state = service.markRead(state.getMid());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->markReaded->"+state.getMid()+","+state.getStatus());
	}
	
	@Test
	public void testBatchSendInfo()
			throws Exception {
		List<Info> infos = new ArrayList<Info>();
		
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testBatchSendInfo title 1");
		info.setType("avro.test.send");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testBatchSendInfo_objectId_1");
		infos.add(info);
		
		info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testBatchSendInfo title 2");
		info.setType("avro.test.send");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testBatchSendInfo_objectId_2");
		infos.add(info);
		
		StateResult state = service.batchSendInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertNotNull(state.getMid());
		Assert.assertTrue(state.getMid().split(",").length==2);
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
	}
	
	@Test
	public void testQueryInfo() throws Exception {
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("updated title.");
		info.setType("avro.test.queryInfo");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testQueryInfo_objectId");
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
		
		List<Info> result = service.queryInfo(info.getAppId(), info.getType(), info.getReceiverId(), info.getObjectId());
		Assert.assertNotNull(result);
		System.out.println(result.size());
		Assert.assertTrue(result.size()>=1);
		Info si = result.get(0);
		Assert.assertEquals(info.getAppId(), si.getAppId());
		Assert.assertEquals(info.getType(), si.getType());
		Assert.assertEquals(info.getReceiverId(), si.getReceiverId());
		Assert.assertEquals(state.getMid(), String.valueOf(si.getId()));
	}

	@Test
	public void testReplaceInfo()
			throws Exception {
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testReplaceInfo title.");
		info.setType("avro.test.testReplaceInfo");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testReplaceInfo_objectId");
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
		
		info.setTitle("testReplaceInfo replaced title");
		state = service.replaceInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->replaceInfo->"+state.getMid()+","+state.getStatus());
	}

	@Test
	public void testBatchReplaceInfo()
			throws Exception {
		List<Info> infos = new ArrayList<Info>();
		
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testBatchSendInfo title 1");
		info.setType("avro.test.batchReplaceInfo.1");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testBatchReplaceInfo_objectId");
		infos.add(info);
		
		info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testBatchSendInfo title 2");
		info.setType("avro.test.batchReplaceInfo.2");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testBatchReplaceInfo_objectId");
		infos.add(info);
		
		StateResult state = service.batchSendInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertNotNull(state.getMid());
		Assert.assertTrue(state.getMid().split(",").length==2);
		
		infos.get(0).setTitle("testBatchSendInfo replaced title 1");
		infos.get(1).setTitle("testBatchSendInfo replaced title 2");
		
		state = service.batchReplaceInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->replaceInfo->"+state.getMid()+","+state.getStatus());
	}
	
	protected StateResult fromJson(String s) throws Exception {
		StringReader sr = new StringReader(s);
		ObjectMapper mapper = new ObjectMapper();
		StateResult state = mapper.readValue(sr, StateResult.class);
		return state;
	}
	
	@Test
	public void testSendByJson()
			throws Exception {
		// send a information
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testSendByJson title\", \"type\":\"json.test.send\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"guid\":\"myguid\", \"objectId\":\"testSendByJson_objectId\"}";
		String result = service.send(information);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->send:"+result);
		
		// mark information state to 'processed'
		result = service.process(state.getMid());
		Assert.assertNotNull(result);
		state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendByJson:"+result);
	}
	
	@Test
	public void testBatchSendByJson()
			throws Exception {
		// send a information
		String information1 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchSendByJson title 1\", \"type\":\"json.test.batchSend.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testSendByJson_objectId_1\"}";
		String information2 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchSendByJson title 2\", \"type\":\"json.test.batchSend.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testSendByJson_objectId_2\"}";
		String informations = "["+information1+","+information2+"]";
		String result = service.batchSend(informations);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertTrue(state.getMid().split(",").length==2);
		System.out.println("NotificationsThriftServiceTest->testBatchSendByJson:"+result);
	}
	
	@Test
	public void testQueryByJson() throws Exception {
		Info info = new Info();
		info.setAppId("testapp");
		info.setSenderId(DEFAULT_SENDER_ID);
		info.setReceiverId(DEFAULT_SENDER_ID);
		info.setTitle("testQueryByJson title.");
		info.setType("json.test.testQueryByJson");
		info.setProcessMethod(2);
		info.setImportance(10);
		info.setUri("http://uri");
		info.setData("this is a data");
		info.setObjectId("testQueryByJson_objectid");
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());
		
		String json = service.query(info.getAppId(), info.getType(), info.getReceiverId(), info.getObjectId());
		Assert.assertNotNull(json);
		
		InformationList result = fromJsonArray(json.toString());
		Assert.assertNotNull(result);
		Assert.assertTrue(result.getCount()>=1);
		Information inf = result.getInformations().get(0);
		Assert.assertEquals(info.getAppId(), inf.getAppId());
		Assert.assertEquals(info.getType(), inf.getType());
		Assert.assertEquals(info.getReceiverId(), inf.getReceiverId());
		Assert.assertEquals(state.getMid(), String.valueOf(inf.getId()));
	}
	
	protected InformationList fromJsonArray(String informations) throws Exception {
		StringReader sr = new StringReader(informations);
		ObjectMapper mapper = new ObjectMapper();
		InformationList result = mapper.readValue(sr, InformationList.class);
		return result;
	}
	
	@Test
	public void testReplaceByJson() throws Exception {
		// send a information
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testReplaceByJson title 1\", \"type\":\"json.test.replace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		
		String result = service.send(information);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertTrue(state.getMid().split(",").length==1);
		System.out.println("NotificationsThriftServiceTest->testBatchSendByJson:"+result);

		information = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testReplaceByJson replaced title 1\", \"type\":\"json.test.replace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		result = service.replace(information);
		Assert.assertNotNull(result);
		System.out.println("NotificationsThriftServiceTest->replaceByJson:"+result);		
	}
	
	@Test
	public void testBatchReplaceByJson() throws Exception {
		// send a information
		String information1 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchReplaceByJson title 1\", \"type\":\"json.test.batchReplace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		String information2 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchReplaceByJson title 2\", \"type\":\"json.test.batchReplace.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		String informations = "["+information1+","+information2+"]";
		
		String result = service.batchSend(informations);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertTrue(state.getMid().split(",").length==2);
		System.out.println("NotificationsThriftServiceTest->testBatchSendByJson:"+result);

		information1 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchReplaceByJson replaced title 1\", \"type\":\"json.test.batchReplace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		information2 = "{\"appId\":\"testapp\",\"senderId\":\"10208\",\"receiverId\":\"10208\"," +
				"\"title\":\"testBatchReplaceByJson replaced title 2\", \"type\":\"json.test.batchReplace.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		result = service.batchReplace(informations);
		Assert.assertNotNull(result);
		state = fromJson(result);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.getStatus());
		Assert.assertTrue(state.getMid().split(",").length>=2);
		System.out.println("NotificationsThriftServiceTest->testBatchSendByJson:"+result);
	}
	
	@Test
	public void testListAll() {
		try {
			Info info = new Info();
			info.setAppId("testapp");
			info.setSenderId(DEFAULT_SENDER_ID);
			info.setReceiverId(DEFAULT_SENDER_ID);
			info.setTitle("testListAll title.");
			info.setType("avro.test.listAll");
			info.setProcessMethod(2);
			info.setImportance(10);
			info.setUri("http://uri");
			info.setData("this is a data");
			info.setObjectId("testListAll_objectId");
			StateResult state = service.sendInf(info);
			Assert.assertNotNull(state);
			Assert.assertEquals("success", state.getStatus());
			System.out.println("NotificationsThriftServiceTest->testListAll->"+state.getMid()+","+state.getStatus());
			
			List<Info> result = service.listAll(DEFAULT_SENDER_ID, "0", 0, 100);
			Assert.assertNotNull(result);
			Assert.assertTrue(result.size()>0);
			System.out.println("NotificationsThriftServiceTest->testListByTime:"+result);
		} catch (TException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testListByTime() {
		try {
			Info info = new Info();
			info.setAppId("testapp");
			info.setSenderId(DEFAULT_SENDER_ID);
			info.setReceiverId(DEFAULT_SENDER_ID);
			info.setTitle("testListByTime title.");
			info.setType("avro.test.testListByTime");
			info.setProcessMethod(2);
			info.setImportance(10);
			info.setUri("http://uri");
			info.setData("this is a data");
			info.setObjectId("testListByTime_objectId");
			StateResult state = service.sendInf(info);
			Assert.assertNotNull(state);
			Assert.assertEquals("success", state.getStatus());
			System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());

			long currTime = System.currentTimeMillis()-10*60*1000;
			List<Info> result = service.listByTime(DEFAULT_SENDER_ID, "0", currTime, 0);
			Assert.assertNotNull(result);
			Assert.assertTrue(result.size()>0);
			System.out.println("NotificationsThriftServiceTest->testListByTime:"+result);
		} catch (TException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testListById() {
		try {
			Info info = new Info();
			info.setAppId("testapp");
			info.setSenderId(DEFAULT_SENDER_ID);
			info.setReceiverId(DEFAULT_SENDER_ID);
			info.setTitle("testListById title.");
			info.setType("avro.test.listById");
			info.setProcessMethod(2);
			info.setImportance(10);
			info.setUri("http://uri");
			info.setData("this is a data");
			info.setObjectId("testListById_objectId");
			StateResult state = service.sendInf(info);
			Assert.assertNotNull(state);
			Assert.assertEquals("success", state.getStatus());
			System.out.println("NotificationsThriftServiceTest->sendInfo->"+state.getMid()+","+state.getStatus());

			List<Info> result = service.listById(DEFAULT_SENDER_ID, "0", 0, 100);
			Assert.assertNotNull(result);
			Assert.assertTrue(result.size()>0);
			System.out.println("NotificationsThriftServiceTest->testListById:"+result);
		} catch (TException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Before
	public void setUp() throws Exception {
		try {
			transport = new TSocket(HOST, PORT);
			protocol = new TBinaryProtocol(transport);
			transport.open();
			service = new INotificationsThriftService.Client(protocol);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		if(null != transport) {
			transport.close();
		}
	}

}
