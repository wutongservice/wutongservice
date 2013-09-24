package com.borqs.information.rpc.service;

import com.borqs.information.rest.bean.Information;
import com.borqs.information.rest.bean.InformationHelper;
import com.borqs.information.rest.bean.InformationList;
import org.apache.avro.AvroRemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class InformationsServiceImpl extends NotificationsServiceCommonImpl implements IInformationsService {
	private static Logger logger = LoggerFactory.getLogger(InformationsServiceImpl.class);

	public StateResult sendInfo(SendInfo info) throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			Information information = InformationHelper.convertToInformation(info);
			if(null == information.getSenderId() || information.getSenderId().equals("")) {
				throw new Exception("Sender ID can not be null or blank!");
			}
			state.mid = send(information);
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult sendInf(Info info) throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			Information information = InformationHelper.convertToInformation(info);
			if(null == information.getSenderId() || information.getSenderId().equals("")) {
				throw new Exception("Sender ID can not be null or blank!");
			}
			state.mid = send(information);
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult batchSendInfo(List<SendInfo> infos)
			throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			StringBuilder sb = new StringBuilder();
			for(SendInfo si : infos) {
				if(null == si.senderId || si.senderId.equals("")) {
					continue;
				}
				Information information = InformationHelper.convertToInformation(si);
				String ids = send(information);
				if(0 == sb.length()) {
					sb.append(ids);
				} else if(null!=ids && ids.length()>0){
					sb.append(",").append(ids);
				}
			}
			state.mid = sb.toString();
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult batchSendInf(List<Info> infos)
			throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			StringBuilder sb = new StringBuilder();
			for(Info si : infos) {
				if(null == si.senderId || si.senderId.equals("")) {
					continue;
				}
				Information information = InformationHelper.convertToInformation(si);
				String ids = send(information);
				if(0 == sb.length()) {
					sb.append(ids);
				} else if(null!=ids && ids.length()>0){
					sb.append(",").append(ids);
				}
			}
			state.mid = sb.toString();
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}

	public StateResult markProcessed(CharSequence mid)
			throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			dao.markProcessed(mid.toString());
			state.status = "success";
			state.mid = mid;
			logger.info("markProcessed by IPC->"+state.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}

	public StateResult markRead(CharSequence mid) throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			dao.markRead(mid.toString());
			state.status = "success";
			state.mid = mid;
			logger.info("markProcessed by IPC->"+state.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public List<Info> queryInfo(CharSequence appId, CharSequence type,
			CharSequence receiverId, CharSequence objectId) throws AvroRemoteException {
		InformationList result = exeQueryForList(appId.toString(), type.toString(), 
				receiverId.toString(), objectId.toString());
		List<Info> infos = InformationHelper.convertToInfos(result.getInformations());
		return infos;
	}
	
	public StateResult replaceInfo(SendInfo info) throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			if(null==info.senderId || info.senderId.equals("")) {
				throw new Exception("Sender ID can not be blank or null!");
			}
			
			Information information = InformationHelper.convertToInformation(info);
			String receiverId = information.getReceiverId();
			String[] receivers = receiverId.split(",");
			logger.info("the splited receiver IDs:"+Arrays.toString(receivers));
			for(String rid : receivers) {
				information.setReceiverId(rid);
				
				if(null!=mqPublisher && information.isPush()) {
					try {
						mqPublisher.send(information);
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("failed to push message to user("+information.getReceiverId()+")");
					}
				}
				String mid = dao.replace(information);
				if(null==state.mid || "-1".equals(state.mid)) {
					state.mid = mid;
				} else {
					state.mid = state.mid+","+mid;
				}
				logger.info("replaceInfo by IPC->"+information.toString());
			}
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult replaceInf(Info info) throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			if(null==info.senderId || info.senderId.equals("")) {
				throw new Exception("Sender ID can not be blank or null!");
			}
			
			Information information = InformationHelper.convertToInformation(info);
			String receiverId = information.getReceiverId();
			String[] receivers = receiverId.split(",");
			for(String rid : receivers) {
				information.setReceiverId(rid);
				
				if(null!=mqPublisher && information.isPush()) {
					try {
						mqPublisher.send(information);
					} catch (Exception e) {
						e.printStackTrace();
						logger.error("failed to push message to user("+information.getReceiverId()+")");
					}
				}
				String mid = dao.replace(information);
				if(null==state.mid || "-1".equals(state.mid)) {
					state.mid = mid;
				} else {
					state.mid = state.mid+","+mid;
				}
				logger.info("replaceInfo by IPC->"+information.toString());
			}
			state.status = "success";
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult batchReplaceInfo(List<SendInfo> infos)
			throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			StringBuilder sb = new StringBuilder();
			for(SendInfo info : infos) {
				if(null == info.senderId || info.senderId.equals("")) {
					continue;
				}
				
				Information information = InformationHelper.convertToInformation(info);
				String mid = dao.replace(information);
				if(sb.length()==0) {
					sb.append(mid);
				} else {
					sb.append(",").append(mid);
				}
			}
			state.mid = sb.toString();
			state.status = "success";
			logger.info("replaceInfo by IPC->"+state.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}
	
	public StateResult batchReplaceInf(List<Info> infos)
			throws AvroRemoteException {
		StateResult state = new StateResult();
		state.status = "failed";
		try {
			StringBuilder sb = new StringBuilder();
			for(Info info : infos) {
				if(null == info.senderId || info.senderId.equals("")) {
					continue;
				}
				
				Information information = InformationHelper.convertToInformation(info);
				String mid = dao.replace(information);
				if(sb.length()==0) {
					sb.append(mid);
				} else {
					sb.append(",").append(mid);
				}
			}
			state.mid = sb.toString();
			state.status = "success";
			logger.info("replaceInfo by IPC->"+state.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new AvroRemoteException(e);
		}
		return state;
	}

	public List<Info> list(CharSequence receiverId, CharSequence status,
			long from, int size) throws AvroRemoteException {
		InformationList result = dao.list(receiverId.toString(), status.toString(), from, size);
		List<Info> infos = InformationHelper.convertToInfos(result.getInformations());
		return infos;
	}

	public List<Info> listById(CharSequence receiverId, CharSequence status,
			long mid, int count) throws AvroRemoteException {
		InformationList result = dao.listById(receiverId.toString(), status.toString(), mid, count);
		List<Info> infos = InformationHelper.convertToInfos(result.getInformations());
		return infos;
	}

	public List<Info> listByTime(CharSequence receiverId, CharSequence status,
			long from, int count) throws AvroRemoteException {
		InformationList result = null;//dao.listByTime(receiverId.toString(), status.toString(), from, count);
		List<Info> infos = InformationHelper.convertToInfos(result.getInformations());
		return infos;
	}

	public List<Info> top(CharSequence receiverId, CharSequence status, int topn)
			throws AvroRemoteException {
		InformationList result = dao.top(receiverId.toString(), status.toString(), topn);
		List<Info> infos = InformationHelper.convertToInfos(result.getInformations());
		return infos;
	}
	
	// by JSON
	public CharSequence send(CharSequence information)
			throws AvroRemoteException {
		try {
			return exeSend(information.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public CharSequence batchSend(CharSequence messages) 
			throws AvroRemoteException {
		try {
			return exeBatchSend(messages.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public CharSequence process(CharSequence mid)
			throws AvroRemoteException {
		try {
			return exeProcess(mid.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public CharSequence query(CharSequence appId, CharSequence type,
			CharSequence receiverId, CharSequence objectId) throws AvroRemoteException {
		try {
			return exeQuery(appId.toString(), type.toString(), receiverId.toString(), objectId.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public CharSequence replace(CharSequence message)
			throws AvroRemoteException {
		try {
			return exeReplace(message.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public CharSequence batchReplace(CharSequence messages)
			throws AvroRemoteException {
		try {
			return exeBatchReplace(messages.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
	
	public int count(CharSequence receiverId, CharSequence status)
			throws AvroRemoteException {
		try {
			return exeCount(receiverId.toString(), status.toString());
		} catch (Exception e) {
			e.printStackTrace();
			throw new AvroRemoteException(e);
		}
	}
}
