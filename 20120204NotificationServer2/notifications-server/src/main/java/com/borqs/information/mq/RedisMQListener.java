package com.borqs.information.mq;

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.borqs.information.push.IPushService;
import com.borqs.information.rest.bean.Information;

public class RedisMQListener implements MessageDelegate {
	private static Logger logger = LoggerFactory.getLogger(RedisMQListener.class);
	private IPushService pushService;
	
	public void setPushService(IPushService pushService) {
		this.pushService = pushService;
	}
	
	@Override
	public void handleMessage(String message) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleMessage(Map message) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleMessage(byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleMessage(Serializable message) {
		try {
			if (message instanceof Information) {
				if (null != pushService) {
					Information msg = (Information) message;
					
					String from = msg.getSenderId();
					String to = msg.getReceiverId();
					String title = msg.getTitle();
					
					pushService.push(from, to, (null == title) ? "untitled" : title);
					logger.info("push message: from="+from+", to="+to+", title="+title);
				}
			} else {
				throw new IllegalArgumentException(
						"Message must be of type Information");
			}
		} catch (Exception ex) {
			logger.error("failed handle message:\n"+((Information)message).toString());
		}
	}
}
