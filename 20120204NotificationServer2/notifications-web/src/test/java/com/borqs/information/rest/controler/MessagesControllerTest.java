package com.borqs.information.rest.controler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;

import javax.annotation.Resource;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

import com.borqs.information.rest.bean.SendStatusResponse;
import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/applicationContext*.xml" })
public class MessagesControllerTest {
	private static final String VIEW_NAME = "informations";
	private static final String ticket = "MTAyMTRfMTMxODMyNDQ0MjQyNF8yOTY1";

	@Resource
	private ApplicationContext applicationContext;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private AnnotationMethodHandlerAdapter handlerAdapter;

	@Resource
	private InformationsController controller;

	@Before
	public void before() {
		init();
		handlerAdapter = applicationContext
				.getBean(AnnotationMethodHandlerAdapter.class);
	}

	@Test
	public void testTop() throws Exception {
		jsonGetInit();
		
		request.setRequestURI("/informations/top.json");
		request.setParameter("ticket", ticket);
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
	}
	
	@Test
	public void testList() throws Exception {
		jsonGetInit();
		
		request.setRequestURI("/informations/list.json");
		request.setParameter("ticket", ticket);
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
	}
	
	@Test
	public void testListById() throws Exception {
		jsonGetInit();
		
		request.setRequestURI("/informations/listbyid.json");
		request.setParameter("ticket", ticket);
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
	}
	
	@Test
	public void testCount() throws Exception {
		jsonGetInit();
		
		request.setRequestURI("/informations/count.json");
		request.setParameter("ticket", ticket);
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
	}
	
	@Test
	public void testSendAndDone() throws Exception {
		jsonPostInit();
		
		request.setRequestURI("/informations/send.json");
		request.setParameter("ticket", ticket);
		
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"10214\"," +
				"\"title\":\"this is a test title\", \"type\":\"type1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"guid\":\"myguid.com\"}";

		request.setContent(information.getBytes());
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
		
		SendStatusResponse res = (SendStatusResponse) result;
		String mid = res.getMid();
		
		// test done
		jsonPutInit();
		request.setRequestURI("/informations/done.json");
		request.setParameter("ticket", ticket);
		request.setParameter("mid", mid);
		
		mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		result = modelMap.get("result");
		assertNotNull(result);
	}
	
	@Test
	public void testBatchSend() throws Exception {
		jsonPostInit();
		
		request.setRequestURI("/informations/send.json");
		request.setParameter("ticket", ticket);
		
		String information = "{\"appId\":\"testapp\",\"senderId\":\"10214\",\"receiverId\":\"test0,test1\"," +
				"\"title\":\"this is a test title update\", \"type\":\"type1\", " +
				"\"processMethod\":\"2\", \"importance\":\"10\", \"guid\":\"myguid.com\"}";

		request.setContent(information.getBytes());
		ModelAndView mav = handlerAdapter.handle(request, response, controller);
		assertNotNull(mav);
		assertEquals(mav.getViewName(), VIEW_NAME);
		
		ModelMap modelMap = mav.getModelMap();
		assertNotNull(modelMap);
		
		Object result = modelMap.get("result");
		assertNotNull(result);
	}
	
	protected void jsonPostInit() {
		addJsonHeader();
		request.setMethod(RequestMethod.POST.toString());
	}

	protected void jsonGetInit() {
		addJsonHeader();
		request.setMethod(RequestMethod.GET.toString());
	}

	protected void jsonPutInit() {
		addJsonHeader();
		request.setMethod(RequestMethod.PUT.toString());
	}

	protected String toJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, o);
		return sw.toString();
	}

	protected String toXml(Object o) throws Exception {
		XStream xstream = new XStream();
		xstream.autodetectAnnotations(true);
		return xstream.toXML(o);
	}

	private void addJsonHeader() {
		init();
		request.addHeader("Accept", "application/json, text/javascript, */*");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
	}

	private void addXmlHeader() {
		init();
		request.addHeader("Accept", "application/xml, text/xml, */*");
		request.addHeader("Content-Type", "application/xml; charset=UTF-8");
	}

	private void init() {
		request = new MockHttpServletRequest();
		request.setCharacterEncoding("UTF-8");
		response = new MockHttpServletResponse();
	}

	protected void xmlGetInit() {
		addXmlHeader();
		request.setMethod(RequestMethod.GET.toString());
	}

	protected void xmlPostInit() {
		addXmlHeader();
		request.setMethod(RequestMethod.POST.toString());
	}

	protected void xmlPutInit() {
		addXmlHeader();
		request.setMethod(RequestMethod.PUT.toString());
	}
}
