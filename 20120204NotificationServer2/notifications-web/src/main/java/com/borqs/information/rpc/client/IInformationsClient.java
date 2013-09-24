package com.borqs.information.rpc.client;

import com.borqs.information.rest.bean.CountResponse;
import com.borqs.information.rest.bean.InformationList;
import com.borqs.information.rest.bean.SendStatusResponse;
import com.borqs.information.rest.bean.StatusResponse;
import com.borqs.information.rpc.service.Info;

public interface IInformationsClient {
	public InformationList list(String id, String status, Long from, Integer size);
	public InformationList listById(String id, String status, Long mid, Integer count);
	public InformationList top(String id, String status, Integer topn);
	public CountResponse count(String ticket, String status);
	public StatusResponse markProcessed(String ticket, String mid);
	public SendStatusResponse send(String ticket, Info msg);
	public StatusResponse markRead(String ticket, String mid);
	public InformationList listbytime(String ticket, String status, Long from, Integer count);
	
}
