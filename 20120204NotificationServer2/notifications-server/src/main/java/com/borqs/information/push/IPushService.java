package com.borqs.information.push;

public interface IPushService {

	public abstract boolean push(String from, String to, String data);

}