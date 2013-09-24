package com.borqs.information;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.borqs.information.mq.IMQPublisher;
import com.borqs.information.rest.bean.Information;

public class JmsQueueSender implements IMQPublisher {		
	private JmsTemplate jmsTemplate;
	private Queue queue;

	public void setConnectionFactory(ConnectionFactory cf) {
		this.jmsTemplate = new JmsTemplate(cf);
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public void send(final Information info) {
		this.jmsTemplate.send(this.queue, new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				TextMessage m = session.createTextMessage(info.getTitle());
				
				if(info.getId()>0) {
					m.setLongProperty(Information.INFO_ID, info.getId());
				}
				if(null != info.getTitle()) {
					m.setStringProperty(Information.INFO_TITLE, info.getTitle());
				}
				if(null != info.getType()) {
					m.setStringProperty(Information.INFO_TYPE, info.getType());
				}
				if(null != info.getAppId()) {
					m.setStringProperty(Information.INFO_APP_ID, info.getAppId());
				}
				if(null != info.getSenderId()) {
					m.setStringProperty(Information.INFO_SENDER_ID, info.getSenderId());
				}
				if(null != info.getReceiverId()) {
					m.setStringProperty(Information.INFO_RECEIVER_ID, info.getReceiverId());
				}
				
				m.setLongProperty(Information.INFO_DATE, info.getDate());
				
				if(null != info.getUri()) {
					m.setStringProperty(Information.INFO_URI, info.getUri());
				}
				
				m.setBooleanProperty(Information.INFO_PROCESSED, info.isProcessed());
				
				if(null != info.getAction()) {
					m.setStringProperty(Information.INFO_ACTION, info.getAction());
				}
				if(null != info.getData()) {
					m.setStringProperty(Information.INFO_DATA, info.getData());
				}
				
				m.setIntProperty(Information.INFO_PROCESS_METHOD, info.getProcessMethod());
				m.setIntProperty(Information.INFO_IMPORTANCE, info.getImportance());
				
				if(null != info.getGuid()) {
					m.setStringProperty(Information.INFO_GUID, info.getGuid());
				}
//				if(null != information.getTitleHtml()) {
//					m.setStringProperty(Information.INFO_TITLE_HTML, information.getTitleHtml());
//				}
				
				if(null != info.getBody()) {
					m.setStringProperty(Information.INFO_BODY, info.getBody());
				}
				if(null != info.getBodyHtml()) {
//					m.setStringProperty(Information.INFO_BODY_HTML, information.getBodyHtml());
				}
				
				if(null != info.getObjectId()) {
					m.setStringProperty(Information.INFO_OBJECT_ID, info.getObjectId());
				}
				
				m.setLongProperty(Information.INFO_LAST_MODIFIED, info.getLastModified());
				
				return m;
			}
		});
	}
}