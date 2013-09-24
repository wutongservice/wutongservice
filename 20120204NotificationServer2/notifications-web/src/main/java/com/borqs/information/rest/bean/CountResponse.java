package com.borqs.information.rest.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("result")
public class CountResponse {

	public CountResponse() {
		super();
	}

	public CountResponse(int count) {
		super();
		this.count = count;
	}

	private int count = 0;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
