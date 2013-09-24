package com.borqs.information.rest.bean;

public class SendStatusResponse {
	private String status = "failed";
	private String mid = "-1";

	public SendStatusResponse() {
		super();
	}

	public SendStatusResponse(String status) {
		super();
		this.status = status;
	}
	
	public SendStatusResponse(String status, String mid) {
		super();
		this.status = status;
		this.mid = mid;
	}

	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
