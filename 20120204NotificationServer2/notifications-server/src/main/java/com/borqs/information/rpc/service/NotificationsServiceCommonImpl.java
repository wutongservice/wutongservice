package com.borqs.information.rpc.service;

import java.io.StringReader;
import java.io.StringWriter;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.borqs.information.dao.IInformationsStorageManager;
import com.borqs.information.mq.IMQPublisher;
import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.information.rest.bean.SendStatusResponse;
import com.borqs.information.rest.bean.StatusResponse;

public class NotificationsServiceCommonImpl {
	private static Logger logger = LoggerFactory.getLogger(NotificationsServiceCommonImpl.class);
	
	protected IInformationsStorageManager dao;
	protected IMQPublisher mqPublisher;
	
	public void setMqPublisher(IMQPublisher mqPublisher) {
		this.mqPublisher = mqPublisher;
	}

	public void setDao(IInformationsStorageManager dao) {
		this.dao = dao;
	}
	
	protected String exeSend(String information)
			throws Exception  {
		String json = "";
		SendStatusResponse result = new SendStatusResponse();
		try {
			Information info = fromJson(information.toString());
			if(null == info.getSenderId() || info.getSenderId().equals("")) {
				throw new Exception("Sender ID can not be null or blank!");
			}
			String receiverId = info.getReceiverId();
			String[] receivers = receiverId.split(",");
			for(String rid : receivers) {
				info.setReceiverId(rid);
				
				if(null!=mqPublisher && info.isPush()) {
					logger.info("start to send message to Push server ......");
					mqPublisher.send(info);
				}
				String mid = dao.save(info);
				if(null==result.getMid() || "-1".equals(result.getMid())) {
					result.setMid(mid);
				} else {
					result.setMid(","+result.getMid());
				}
				logger.info("send by IPC->"+info.toString());
			}
			result.setStatus("success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setStatus("failed");
		} finally {
			try {
				json = toJson(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json;
	}

	protected String exeBatchSend(String messages)
			throws Exception {
		// TODO need advance performance issue
		String json = "";
		SendStatusResponse result = new SendStatusResponse();
		try {
			Information[] infomations = fromJsonArray(messages.toString());

			StringBuilder sb = new StringBuilder();
			for(Information msg : infomations) {
				if(null == msg.getSenderId() || msg.getSenderId().equals("")) {
					continue;
				}
				String receiverId = msg.getReceiverId();
				String[] receivers = receiverId.split(",");
				for(String rid : receivers) {
					msg.setReceiverId(rid);
					
					if(null!=mqPublisher && msg.isPush()) {
						logger.info("start to send message to Push server ......");
						mqPublisher.send(msg);
					}
					String mid = dao.save(msg);
					if(sb.length()==0) {
						sb.append(mid);
					} else {
						sb.append(",").append(mid);
					}
					logger.info("batchSend by IPC->"+msg.toString());
				}
			}
			result.setMid(sb.toString());
			result.setStatus("success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setStatus("failed");
		} finally {
			try {
				json = toJson(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json;
	}

	protected String exeProcess(String mid)
			throws Exception {
		String json = "";
		StatusResponse result = new StatusResponse();
		try {
			String[] msgIds = mid.toString().split(",");
			for(String msgId : msgIds) {
				dao.markProcessed(msgId);
				logger.info("process by IPC->"+msgId);
			}
			result.setStatus("success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setStatus("failed");
		} finally {
			try {
				json = toJson(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json;
	}

	protected String exeQuery(String appId, String type, String receiverId, String objectId) throws Exception {
		String json = "";
		InformationList infoList = exeQueryForList(appId.toString(), type.toString(), receiverId.toString(), objectId.toString());
		try {
			json = toJson(infoList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}

	protected InformationList exeQueryForList(String appId, String type, String receiverId, String objectId) {
		InformationList infoList = dao.query(appId, type, receiverId, objectId);
		if(null == infoList || null == infoList.getInformations()) {
			return new InformationList();
		}
		return infoList;
	}
	
	protected String exeReplace(String message)
			throws Exception {
		String json = "";
		SendStatusResponse result = new SendStatusResponse();
		try {
			Information msg = fromJson(message.toString());
			if(null==msg.getSenderId() || msg.getSenderId().equals("")) {
				throw new Exception("Sender ID can not be blank or null!");
			}
			
			String receiverId = msg.getReceiverId();
			String[] receivers = receiverId.split(",");
			for(String rid : receivers) {
				msg.setReceiverId(rid);
				
				if(null!=mqPublisher && msg.isPush()) {
					logger.info("start to send message to Push server ......");
					mqPublisher.send(msg);
				}
				String mid = dao.replace(msg);
				if(null==result.getMid() || "-1".equals(result.getMid())) {
					result.setMid(mid);
				} else {
					result.setMid(","+result.getMid());
				}
				logger.info("replace by IPC->"+msg.toString());
			}
			result.setStatus("success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setStatus("failed");
		} finally {
			try {
				json = toJson(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json;
	}

	protected String exeBatchReplace(String messages)
			throws Exception {
		String json = "";
		SendStatusResponse result = new SendStatusResponse();
		try {
			Information[] informations = fromJsonArray(messages.toString());
			
			StringBuilder sb = new StringBuilder();
			for(Information msg : informations) {
				if(null == msg.getSenderId() || msg.getSenderId().equals("")) {
					continue;
				}
				String receiverId = msg.getReceiverId();
				String[] receivers = receiverId.split(",");
				for(String rid : receivers) {
					msg.setReceiverId(rid);
					
					if(null!=mqPublisher && msg.isPush()) {
						logger.info("start to send message to Push server ......");
						mqPublisher.send(msg);
					}
					String mid = dao.replace(msg);
					if(sb.length()==0) {
						sb.append(mid);
					} else {
						sb.append(",").append(mid);
					}
					logger.info("replace by IPC->"+msg.toString());
				}
			}
			result.setMid(sb.toString());
			result.setStatus("success");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result.setStatus("failed");
		} finally {
			try {
				json = toJson(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json;
	}
	
	protected Information fromJson(String information) throws Exception {
		StringReader sr = new StringReader(information);
		ObjectMapper mapper = new ObjectMapper();
		Information msg = mapper.readValue(sr, Information.class);
		return msg;
	}
	
	protected String toJson(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//		mapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, false);
		mapper.writeValue(sw, o);
		return sw.toString();
	}
	
	protected String toJsonArray(Object o) throws Exception {
		StringWriter sw = new StringWriter();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(sw, o);
		return sw.toString();
	}
	
	protected Information[] fromJsonArray(String informations) throws Exception {
		StringReader sr = new StringReader(informations);
		ObjectMapper mapper = new ObjectMapper();
		Information[] msg = mapper.readValue(sr, Information[].class);
		return msg;
	}

	protected String send(Information info) {
		String ids = "";
		
		try {
			String receiverId = info.getReceiverId();
			String[] receivers = receiverId.split(",");
			for(String rid : receivers) {
				info.setReceiverId(rid);
				
				if(null!=mqPublisher && info.isPush()) {
					logger.info("start to send message to Push server ......");
					mqPublisher.send(info);
				}
				
				String mid = dao.save(info);
				
				if(null==ids || "".equals(ids)) {
					ids = mid;
				} else {
					ids = ","+ids;
				}
				logger.info("sendInfo by IPC->"+info.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return ids;
	}
	

	protected int exeCount(String receiverId, String status)
			throws Exception {
		return dao.count(receiverId, status);
	}
	
	protected int exeCount(String appId, String receiverId, String status)
			throws Exception {
		return dao.count(appId, receiverId, status);
	}
}
