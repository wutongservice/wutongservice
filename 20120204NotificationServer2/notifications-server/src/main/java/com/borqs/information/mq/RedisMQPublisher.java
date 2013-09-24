package com.borqs.information.mq;

import org.springframework.data.redis.core.RedisTemplate;

import com.borqs.information.rest.bean.Information;

public class RedisMQPublisher implements IMQPublisher {
	private String topic = "informations";
	private RedisTemplate template;
	
	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void setTemplate(RedisTemplate template) {
		this.template = template;
	}

	@Override
	public void send(Information info) {
		this.template.convertAndSend(this.topic, info);
	}
}
