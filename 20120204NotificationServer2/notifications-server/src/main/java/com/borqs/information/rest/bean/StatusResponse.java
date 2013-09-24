package com.borqs.information.rest.bean;

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
