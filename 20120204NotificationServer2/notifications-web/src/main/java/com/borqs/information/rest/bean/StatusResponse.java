package com.borqs.information.rest.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("status")
public class StatusResponse {
	private String status = "failed";
	
	public StatusResponse() {
		super();
	}

	public StatusResponse(String status) {
		super();
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
