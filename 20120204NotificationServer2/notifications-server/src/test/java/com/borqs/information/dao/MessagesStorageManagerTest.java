package com.borqs.information.dao;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.UUID;

import javax.annotation.Resource;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.util.HtmlUtils;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;

public class MessagesStorageManagerTest {
	private MongoDBStorage  dao;

	@Before
	public void setUp() throws Exception {
		dao = new MongoDBStorage();
		dao.clearCollections();
		
		String receiverId = "10214";
		String type = "type.test";
		String appId = "123";
		
		for(int i=0;i<100;i++)
		{
			Information info = new Information();
			info.setAppId(appId);
			info.setSenderId("00_"+i);
			info.setReceiverId(receiverId);
			info.setType(type);
			info.setAction("ketian's action");
			info.setTitle("ketian's title");
			info.setDate(System.currentTimeMillis());
			info.setUri("ketian's url");
			info.setProcessed(false);
			info.setProcessMethod(1);
			info.setImportance(200);
			info.setTitleHtml("this is title html");
			info.setBody("this is body");
			info.setBodyHtml("this is body html");
			info.setObjectId("this is object id");
			//info.setGuid("this is guid");
			info.setRead(false);
			info.setLastModified(System.currentTimeMillis());
			dao.save(info);	
		}
		

		int oldCount = dao.count(null, null);
		System.out.println(oldCount);
		oldCount = dao.count(receiverId, null);
		System.out.println(oldCount);
	}
	@Test
	public void test1QueryByAppIdTypeReceiverId() {

		String receiverId = "10214";
		String type = "type.test";
		String appId = "123";

		int oldCount = dao.count(receiverId, null);
		
		Information information = new Information();
		information.setAppId(appId);
		information.setDate(Calendar.getInstance().getTimeInMillis());
		information.setProcessed(false);
		information.setReceiverId(receiverId);
		information.setSenderId(receiverId);
		information.setTitle("this is a unit test title");
		information.setType(type);
		information.setAction("action.test");
		information.setData("data.test");
		information.setUri("http://test");
		information.setProcessMethod(1);
		information.setImportance(3);
		String res = dao.save(information);
		
		Assert.assertNotNull(res);
		System.out.println(res);
		
		int newCount = dao.count(receiverId, null);
		assertTrue(newCount>0);
		
		assertTrue(newCount-oldCount==1);
		
		InformationList result = dao.query(appId, type, receiverId, null);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.getCount()==1);
		Information inf = result.getInformations().get(0);
		Assert.assertEquals(Long.valueOf(res), Long.valueOf(inf.getId()));
	}
	
	@Test
	public void test2Replace() {
		String receiverId = "10214";
		String type = "type.test";
		String appId = "123";

		int oldCount = dao.count(receiverId, null);
		
		Information information = new Information();
		information.setAppId(appId);
		information.setDate(Calendar.getInstance().getTimeInMillis());
		information.setProcessed(false);
		information.setReceiverId(receiverId);
		information.setSenderId(receiverId);
		information.setTitle("this is a unit test title");
		information.setType(type);
		information.setAction("action.test");
		information.setData("data.test");
		information.setUri("http://test");
		information.setProcessMethod(1);
		information.setImportance(3);
		String res = dao.save(information);
		
		Assert.assertNotNull(res);
		System.out.println(res);
		
		int newCount = dao.count(receiverId, null);
		assertTrue(newCount>0);
		
		assertTrue(newCount-oldCount==1);
		
		information.setTitle("replaced title");
		String id = dao.replace(information);
		assertTrue(id.length()>0);
	}
	
	@Test
	public void test3Save() {
		int oldCount = dao.count("10214", null);
		
		Information information = new Information();
		information.setAppId("123");
		information.setDate(Calendar.getInstance().getTimeInMillis());
		information.setProcessed(false);
		information.setReceiverId("10214");
		information.setSenderId("10214");
		information.setTitle("this is a unit test title");
		information.setType("type.test");
		information.setAction("action.test");
		information.setData("data.test");
		information.setUri("http://test");
		information.setProcessMethod(1);
		information.setImportance(3);
		String res = dao.save(information);
		
		Assert.assertNotNull(res);
		System.out.println(res);
		
		int newCount = dao.count("10214", null);
		assertTrue(newCount>0);
		
		assertTrue(newCount-oldCount==1);
	}
	
	@Test
	public void test4SaveByGuid() {
		String uuid = UUID.randomUUID().toString();
		uuid = uuid.replace("-", "").toUpperCase();

		int oldCount = dao.count("10214", null);
		
		Information information = new Information();
		information.setAppId("123");
		information.setDate(Calendar.getInstance().getTimeInMillis());
		information.setProcessed(false);
		information.setReceiverId("10214");
		information.setSenderId("10214");
		information.setTitle("this is a unit test title");
		information.setType("type.test");
		information.setAction("action.test");
		information.setData("data.test");
		information.setUri("http://test");
		information.setProcessMethod(1);
		information.setImportance(3);
		information.setGuid(uuid);
		String res = dao.save(information);
		
		Assert.assertNotNull(res);
		System.out.println(res);
		
		int newCount = dao.count("10214", null);
		assertTrue(newCount>0);
		
		assertTrue(newCount-oldCount==1);
		
		// test update
		information = new Information();
		information.setAppId("123");
		information.setDate(Calendar.getInstance().getTimeInMillis());
		information.setProcessed(false);
		information.setReceiverId("10214");
		information.setSenderId("10214");
		information.setTitle("this is a unit test title for update");
		information.setType("type.test");
		information.setAction("action.test.update");
		information.setData("data.test.update");
		information.setUri("http://test.update");
		information.setProcessMethod(1);
		information.setImportance(3);
		information.setGuid(uuid);
		information.setTitleHtml(HtmlUtils.htmlEscape("<a href='http://sohu.com'>test title html</a>"));
		information.setBody("my body");
		information.setBodyHtml(HtmlUtils.htmlEscape("<a href='http://sohu.com'>test body html</a>"));
		res = dao.save(information);
		
		Assert.assertNotNull(res);
		System.out.println(res);
		
		newCount = dao.count("10214", null);
		assertTrue(newCount>0);
		
		assertTrue(newCount-oldCount==1);
	}
	
	private void showResult(InformationList informations) {
		try {
			System.out.println(toJson(informations));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void test5ListStringStringIntegerInteger() {
		InformationList result = dao.list("10214", null, 0l, 10);
		System.out.println(result.getCount());
		assertTrue(result.getCount()>0);
	}
	
	@Test
	public void test6ListById() {
		InformationList result = dao.listById("10214", "0", 30l, null);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
		showResult(result);
		
		result = dao.listById("10214", "0", 30l, 15);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
		showResult(result);
		
		result = dao.listById("10214", "0", 30l, -15);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
		showResult(result);
		
		result = dao.listById("10214", "0", 30l, 5);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
		showResult(result);
		
		result = dao.listById("10214", "0", 30l, -5);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
		showResult(result);
	}

	@Test
	public void test7Top() {
		InformationList result = dao.top("10214", null, 8);
		System.out.println("result count is: "+result.getCount());
		assertTrue(result.getCount()>0);
	}

	@Test
	public void test8Count() {
		int count = dao.count("10214", null);
		System.out.println(count);
		assertTrue(count>0);
		
		count = dao.count("10214", "0");
		System.out.println(count);
		assertTrue(count>0);
		
//		count = dao.count("10214", "1");
//		System.out.println(count);
//		assertTrue(count==0);
	}
	
	protected String toJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, o);
		return sw.toString();
	}
}
