package com.borqs.information.mq;

import com.borqs.information.rest.bean.Information;

public interface IMQPublisher {
	public abstract void send(final Information info);
}