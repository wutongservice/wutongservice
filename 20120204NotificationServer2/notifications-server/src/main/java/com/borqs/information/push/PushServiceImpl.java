package com.borqs.information.push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PushServiceImpl implements IPushService {
	private static final String NOTIFICATION_SERVER_APP_ID = "101";
	
	private String appId = NOTIFICATION_SERVER_APP_ID;
	private String address = "http://192.168.5.187:9090/plugins/xDevice/send";

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean push(String from, String to, String data) {
		boolean result = false;
		
		HttpURLConnection httpConn = null;
		InputStreamReader input = null;
		OutputStream output = null;
		
		try {
			URL url = new URL(address);
			System.out.println(url);

			httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			httpConn.setRequestMethod("POST");
			httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			StringBuffer reqBuf = new StringBuffer();
			if(null!=from && !"".equals(from.trim())) {
				reqBuf.append("from_jid=").append(from);
			}
			
			if(null!=to && !"".equals(to.trim())) {
				if(reqBuf.length()>0) reqBuf.append("&");
				reqBuf.append("jid=").append(to);
			}
			
			if(null!=appId && !"".equals(appId.trim())) {
				if(reqBuf.length()>0) reqBuf.append("&");
				reqBuf.append("app_id=").append(appId);
			}
			
			if(null!=data) {
				if(reqBuf.length()>0) reqBuf.append("&");
				reqBuf.append("data=").append(URLEncoder.encode(data, "UTF-8"));
			}
			
			String req = reqBuf.toString();
			System.out.println(req);
//			httpConn.setRequestProperty( "Content-Length",String.valueOf(req.getBytes().length));
			output = httpConn.getOutputStream();
			output.write(req.getBytes());

			// read result
			input = new InputStreamReader(httpConn.getInputStream(), "utf-8");
			BufferedReader in = new BufferedReader(input);
			String inputLine;
			StringBuffer sb = new StringBuffer();
			while (null != (inputLine = in.readLine())) {
				sb.append(inputLine);
			}
			System.out.println(sb.toString());
			
			if(sb.toString().trim().equalsIgnoreCase("OK")) {
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(null!=output) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(null!=input) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(null!=httpConn) {
				httpConn.disconnect();
			}
		}
		
		return result;
	}
}
