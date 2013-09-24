package com.borqs.information.rest.controler;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.borqs.information.rest.bean.CountResponse;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.information.rest.bean.SendStatusResponse;
import com.borqs.information.rest.bean.StatusResponse;
import com.borqs.information.rpc.client.IInformationsClient;
import com.borqs.information.rpc.service.Info;

@Controller
public class InformationsController {
	private static final String VIEW_NAME = "informations";
	
	private IInformationsClient client;
	
	@Required
	@Resource(name = "informationsSerivce")
	public void setClient(IInformationsClient client) {
		this.client = client;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/informations/list")
	public ModelAndView list(
			@RequestParam(required=true) String ticket,
			@RequestParam(required=false) String status, 
			@RequestParam(required=false) Long from,
			@RequestParam(required=false) Integer size) {
		InformationList informations = client.list(ticket, status, from, size);
		return new ModelAndView(VIEW_NAME, "result", informations);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/informations/listbyid")
	public ModelAndView listbyid(
			@RequestParam(required=true) String ticket,
			@RequestParam(required=false) String status, 
			@RequestParam(required=false) Long mid,
			@RequestParam(required=false) Integer count) {
		InformationList informations = client.listById(ticket, status, mid, count);
		return new ModelAndView(VIEW_NAME, "result", informations);
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/informations/listbytime")
	public ModelAndView listbytime(
			@RequestParam(required=true) String ticket,
			@RequestParam(required=false) String status, 
			@RequestParam(required=false) Long from,
			@RequestParam(required=false) Integer count) {
		InformationList informations = client.listbytime(ticket, status, from, count);		
		return new ModelAndView(VIEW_NAME, "result", informations);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/informations/top")
	public ModelAndView top(
			@RequestParam(required=true) String ticket,
			@RequestParam(required=false) String status, 
			@RequestParam(required=false) Integer topn) {
		InformationList informations = client.top(ticket, status, topn);
		return new ModelAndView(VIEW_NAME, "result", informations);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/informations/count")
	public ModelAndView count(
			@RequestParam(required=true) String ticket,
			@RequestParam(required=false) String status) {
		CountResponse result = client.count(ticket, status);
		return new ModelAndView(VIEW_NAME, "result", result);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/informations/send")
	public ModelAndView send(
			@RequestParam String ticket,
			@RequestBody Info msg) {
		SendStatusResponse result = client.send(ticket, msg);
		return new ModelAndView(VIEW_NAME, "result", result);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/informations/done")
	public ModelAndView done(@RequestParam String ticket,
			@RequestParam String mid) {
		StatusResponse result = client.markProcessed(ticket, mid);
		return new ModelAndView(VIEW_NAME, "result", result);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/informations/read")
	public ModelAndView read(@RequestParam String ticket,
			@RequestParam String mid) {
		StatusResponse result = client.markRead(ticket, mid);
		return new ModelAndView(VIEW_NAME, "result", result);
	}
}
