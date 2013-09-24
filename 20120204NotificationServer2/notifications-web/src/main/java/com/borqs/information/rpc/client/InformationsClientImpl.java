package com.borqs.information.rpc.client;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

import javax.annotation.Resource;

import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import com.borqs.information.auth.TicketManager;
import com.borqs.information.rest.bean.CountResponse;
import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationHelper;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.information.rest.bean.SendStatusResponse;
import com.borqs.information.rest.bean.StatusResponse;
import com.borqs.information.rpc.service.IInformationsService;
import com.borqs.information.rpc.service.Info;

public class InformationsClientImpl implements IInformationsClient {
	private static Logger logger = LoggerFactory.getLogger(InformationsClientImpl.class);
	
	private Transceiver trans;
	private IInformationsService service;
	private TicketManager ticketManager;
	
	private String scheme = "avro";
	private String host = "127.0.0.1";
	private int port = 8083;
	
	@Required
	@Resource(name="ticketManager")
	public void setTicketManager(TicketManager ticketManager) {
		this.ticketManager = ticketManager;
	}
	
	public void init() {
		logger.info("start to init avro service connection!");
		try {
			URI uri = new URI(scheme+"://"+host+":"+port);
			trans = Ipc.createTransceiver(uri);
			service = SpecificRequestor.getClient(IInformationsService.class, trans);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
	
	public void destory() {
		if(null != trans && trans.isConnected()) {
			try {
				trans.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("avro connection has been destroy!");
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InformationList list(String ticket, String status, Long from,
			Integer size) {
		InformationList informations = null;
		try {
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				
				// default to read unprocessed items
				if(null==status || "".equals(status.trim())) {
					status = "0";
				}
				
				// default start item is from zero position
				if(null==from) {
					from = 0L;
				}
				
				// default to read twenty items
				if(null==size) {
					size = 20;
				}
				
				List<Info> result = service.list(receiverId, status, from, size);
				List<Information> infos = InformationHelper.convertToInformations(result);
				informations = new InformationList(infos);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if(null==informations) { 
				informations = new InformationList();
			}
		}
		
		logger.info("list ->"+informations.description());
		
		return informations;
	}

	public InformationList listById(String ticket, String status, Long mid,
			Integer count) {
		InformationList informations = null;
		try {
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				
				// default to read unprocessed items
				if(null==status || "".equals(status.trim())) {
					status = "0";
				}
				
				// default start item is from zero position
				if(null==mid) {
					mid = 0L;					
				}
				
				// default start item is from zero position
				if(null==count) {
					count = 0;					
				}
				
				List<Info> result = service.listById(receiverId, status, mid, count);
				List<Information> infos = InformationHelper.convertToInformations(result);
				informations = new InformationList(infos);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if(null==informations) { 
				informations = new InformationList();
			}
		}
		
		logger.info("listbyid by http service ->"+informations.description());
		
		return informations;
	}

	public InformationList listbytime(String ticket, String status, Long from,
			Integer count) {
		InformationList informations = null;
		try {
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				// default to read unprocessed items
				if(null==status || "".equals(status.trim())) {
					status = "0";
				}
				
				// default start item is from zero position
				if(null==from) {
					from = 0L;					
				}
				
				// default start item is from zero position
				if(null==count) {
					count = 0;					
				}
				
				List<Info> result = service.listByTime(receiverId, status, from, count);
				List<Information> infos = InformationHelper.convertToInformations(result);
				informations = new InformationList(infos);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if(null==informations) { 
				informations = new InformationList();
			}
		}
		
		logger.info("listbyid ->"+informations.description());
		
		return informations;
	}
	
	public InformationList top(String ticket, String status, Integer topn) {
		InformationList informations = null;
		try {
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				// default to read unprocessed items
				if(null==status || "".equals(status.trim())) {
					status = "0";
				}
				
				// default start item is from zero position
				if(null==topn) {
					topn = 5;					
				}
				
				List<Info> result = service.top(receiverId, status, topn);
				List<Information> infos = InformationHelper.convertToInformations(result);
				informations = new InformationList(infos);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if(null==informations) { 
				informations = new InformationList();
			}
		}
		
		logger.info("top ->"+informations.description());
		
		return informations;
	}

	public CountResponse count(String ticket, String status) {
		CountResponse result = null;
		try {
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				// default to read unprocessed items
				if(null==status || "".equals(status.trim())) {
					status = "0";
				}

				int count = service.count(receiverId, status);
				result = new CountResponse(count);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		logger.info("count ->"+result.toString());
		
		return result;
	}
	
	public SendStatusResponse send(String ticket, Info msg) {
		// save to database
		SendStatusResponse result = new SendStatusResponse();
		try {
			if(null == msg.senderId || "".equals(msg.senderId)) {
				throw new Exception("SenderID can not be null or blank!");
			}
			String receiverId = ticketManager.ticket2ID(ticket);
			if (null != receiverId) {
				service.sendInf(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		logger.info("send ->"+result.toString());
		
		return result;
	}
	
	public StatusResponse markProcessed(String ticket, String mid) {
		// save to database
		StatusResponse result = new StatusResponse();
		try {
			String id = ticketManager.ticket2ID(ticket);
			if (null != id) {
				String[] msgIds = mid.split(",");
				for(String msgId : msgIds) {
					service.markProcessed(msgId);
				}
				result.setStatus("success");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		logger.info("setProcessed ->"+result.toString());
		
		return result;
	}

	public StatusResponse markRead(String ticket, String mid) {
		// save to database
		StatusResponse result = new StatusResponse();
		try {
			String id = ticketManager.ticket2ID(ticket);
			if (null != id) {
				String[] msgIds = mid.split(",");
				for(String msgId : msgIds) {
					service.markRead(msgId);
				}
				result.setStatus("success");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		logger.info("done by http service ->"+result.toString());
		
		return result;
	}
	
	protected String toJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, o);
		return sw.toString();
	}
	
	protected String toJsonArray(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, o);
		return sw.toString();
	}
}
