package com.borqs.information;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.borqs.information.push.IPushService;
import com.borqs.information.rest.bean.Information;

public class JmsMessageListener implements MessageListener {
	private IPushService pushService;
	
	public void setPushService(IPushService pushService) {
		this.pushService = pushService;
	}

	public void onMessage(Message msg) {
		if (msg instanceof TextMessage) {
			try {
//				long mid = information.getLongProperty(Information.INFO_ID);
				if(null!=pushService) {
					String from = msg.getStringProperty(Information.INFO_SENDER_ID);
					String to = msg.getStringProperty(Information.INFO_RECEIVER_ID);
					String title = msg.getStringProperty(Information.INFO_TITLE);
					pushService.push(from, to, (null==title)?"untitled":title);
				}
			} catch (JMSException ex) {
				throw new RuntimeException(ex);
			}
		} else {
			throw new IllegalArgumentException(
					"Message must be of type TextMessage");
		}
	}

}
