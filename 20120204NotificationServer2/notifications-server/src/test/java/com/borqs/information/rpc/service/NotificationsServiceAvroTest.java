package com.borqs.information.rpc.service;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;

public class NotificationsServiceAvroTest {
	Transceiver trans;
	IInformationsService service;
	
	@Test
	public void testSendAndProcessedInfo()
			throws AvroRemoteException {
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "updated title.";
		info.type = "avro.test.testSendAndProcessedInfo";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testSendAndProcessedInfo_objectId";
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
		
//		state = service.markProcessed(state.mid);
//		Assert.assertNotNull(state);
//		Assert.assertEquals("success", state.status.toString());
//		System.out.println("AvroMessagesServiceTest->markProcessed->"+state.mid+","+state.status);
	}

	@Test
	public void testSendAndReadInfo()
			throws AvroRemoteException {
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testSendAndReadedInfo title.";
		info.type = "avro.test.testSendAndReadedInfo";
//		info.processMethod = 2;
//		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a testSendAndReadedInfo data";
		info.objectId = "testSendAndProcessedInfo_objectId";
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
		
		state = service.markRead(state.mid);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->markReaded->"+state.mid+","+state.status);
	}
	
	@Test
	public void testBatchSendInfo()
			throws AvroRemoteException {
		List<Info> infos = new ArrayList<Info>();
		
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testBatchSendInfo title 1";
		info.type = "avro.test.send";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testBatchSendInfo_objectId_1";
		infos.add(info);
		
		info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testBatchSendInfo title 2";
		info.type = "avro.test.send";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testBatchSendInfo_objectId_2";
		infos.add(info);
		
		StateResult state = service.batchSendInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		Assert.assertNotNull(state.mid);
		Assert.assertTrue(state.mid.toString().split(",").length==2);
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
	}
	
	@Test
	public void testQueryInfo() throws AvroRemoteException {
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "updated title.";
		info.type = "avro.test.queryInfo";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testQueryInfo_objectId";
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
		
		List<Info> result = service.queryInfo(info.appId, info.type, info.receiverId, info.objectId);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.size()>=1);
		Info si = result.get(0);
		Assert.assertEquals(info.appId.toString(), si.appId.toString());
		Assert.assertEquals(info.type.toString(), si.type.toString());
		Assert.assertEquals(info.receiverId.toString(), si.receiverId.toString());
		Assert.assertEquals(state.mid.toString(), si.id.toString());
	}

	@Test
	public void testReplaceInfo()
			throws AvroRemoteException {
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testReplaceInfo title.";
		info.type = "avro.test.testReplaceInfo";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testReplaceInfo_objectId";
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
		
		info.title = "testReplaceInfo replaced title";
		state = service.replaceInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->replaceInfo->"+state.mid+","+state.status);
	}

	@Test
	public void testBatchReplaceInfo()
			throws AvroRemoteException {
		List<Info> infos = new ArrayList<Info>();
		
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testBatchSendInfo title 1";
		info.type = "avro.test.batchReplaceInfo.1";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testBatchReplaceInfo_objectId";
		infos.add(info);
		
		info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testBatchSendInfo title 2";
		info.type = "avro.test.batchReplaceInfo.2";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testBatchReplaceInfo_objectId";
		infos.add(info);
		
		StateResult state = service.batchSendInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		Assert.assertNotNull(state.mid);
		Assert.assertTrue(state.mid.toString().split(",").length==2);
		
		infos.get(0).title = "testBatchSendInfo replaced title 1";
		infos.get(1).title = "testBatchSendInfo replaced title 2";
		
		state = service.batchReplaceInf(infos);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->replaceInfo->"+state.mid+","+state.status);
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
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testSendByJson title\", \"type\":\"json.test.send\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"guid\":\"myguid\", \"objectId\":\"testSendByJson_objectId\"}";
		CharSequence result = service.send(information);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		System.out.println("AvroMessagesServiceTest->send:"+result);
		
		// mark information state to 'processed'
		result = service.process(state.mid);
		Assert.assertNotNull(result);
		state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		System.out.println("AvroMessagesServiceTest->sendByJson:"+result);
	}
	
	@Test
	public void testBatchSendByJson()
			throws Exception {
		// send a information
		String information1 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchSendByJson title 1\", \"type\":\"json.test.batchSend.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testSendByJson_objectId_1\"}";
		String information2 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchSendByJson title 2\", \"type\":\"json.test.batchSend.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testSendByJson_objectId_2\"}";
		String informations = "["+information1+","+information2+"]";
		CharSequence result = service.batchSend(informations);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		Assert.assertTrue(state.mid.toString().split(",").length==2);
		System.out.println("AvroMessagesServiceTest->testBatchSendByJson:"+result);
	}
	
	@Test
	public void testQueryByJson() throws Exception {
		Info info = new Info();
		info.appId = "testapp";
		info.senderId = "0";
		info.receiverId = "10214";
		info.title = "testQueryByJson title.";
		info.type = "json.test.testQueryByJson";
		info.processMethod = 2;
		info.importance = 10;
		info.uri = "http://uri";
		info.data = "this is a data";
		info.objectId = "testQueryByJson_objectid";
		StateResult state = service.sendInf(info);
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status.toString());
		System.out.println("AvroMessagesServiceTest->sendInfo->"+state.mid+","+state.status);
		
		CharSequence json = service.query(info.appId, info.type, info.receiverId, info.objectId);
		Assert.assertNotNull(json);
		
		InformationList result = fromJsonArray(json.toString());
		Assert.assertNotNull(result);
		Assert.assertTrue(result.getCount()>=1);
		Information inf = result.getInformations().get(0);
		Assert.assertEquals(info.appId.toString(), inf.getAppId());
		Assert.assertEquals(info.type.toString(), inf.getType());
		Assert.assertEquals(info.receiverId.toString(), inf.getReceiverId());
		Assert.assertEquals(state.mid.toString(), String.valueOf(inf.getId()));
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
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testReplaceByJson title 1\", \"type\":\"json.test.replace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		
		CharSequence result = service.send(information);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		Assert.assertTrue(state.mid.toString().split(",").length==1);
		System.out.println("AvroMessagesServiceTest->testBatchSendByJson:"+result);

		information = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testReplaceByJson replaced title 1\", \"type\":\"json.test.replace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		result = service.replace(information);
		Assert.assertNotNull(result);
		System.out.println("AvroMessagesServiceTest->replaceByJson:"+result);		
	}
	
	@Test
	public void testBatchReplaceByJson() throws Exception {
		// send a information
		String information1 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchReplaceByJson title 1\", \"type\":\"json.test.batchReplace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		String information2 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchReplaceByJson title 2\", \"type\":\"json.test.batchReplace.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		String informations = "["+information1+","+information2+"]";
		
		CharSequence result = service.batchSend(informations);
		Assert.assertNotNull(result);
		StateResult state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		Assert.assertTrue(state.mid.toString().split(",").length==2);
		System.out.println("AvroMessagesServiceTest->testBatchSendByJson:"+result);

		information1 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchReplaceByJson replaced title 1\", \"type\":\"json.test.batchReplace.1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		information2 = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"testBatchReplaceByJson replaced title 2\", \"type\":\"json.test.batchReplace.2\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"objectId\":\"testReplaceByJson_objectid\"}";
		result = service.batchReplace(informations);
		Assert.assertNotNull(result);
		state = fromJson(result.toString());
		Assert.assertNotNull(state);
		Assert.assertEquals("success", state.status);
		Assert.assertTrue(state.mid.toString().split(",").length>=2);
		System.out.println("AvroMessagesServiceTest->testBatchSendByJson:"+result);		
	}
	
	@Before
	public void setUp() throws Exception {
		try {
//			URI uri = new URI("avro://192.168.5.209:8083");
//			URI uri = new URI("avro://192.168.5.208:8083");
			URI uri = new URI("avro://127.0.0.1:8083");
//			URI uri = new URI("avro://192.168.5.182:8083");
			trans = Ipc.createTransceiver(uri);
			service = SpecificRequestor.getClient(IInformationsService.class, trans);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		trans.close();
	}

}
