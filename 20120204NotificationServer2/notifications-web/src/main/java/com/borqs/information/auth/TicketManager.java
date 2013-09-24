package com.borqs.information.auth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.json.JSONObject;

public class TicketManager {
	private HashMap<String, String> mTicketTable = new HashMap<String, String>();
	
	private String authUrl = "http://api.borqs.com/account/who?ticket=%s";

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String ticket2ID(String ticket) {
		if (mTicketTable.containsKey(ticket)) {
			return mTicketTable.get(ticket);
		}
		String result = null;
		try {
			
			String formatedUrl = String.format(authUrl, ticket);
			URL url = new URL(formatedUrl);
			HttpURLConnection urlConn = (HttpURLConnection) url
					.openConnection();
			urlConn.setDoOutput(true);
			urlConn.setRequestMethod("GET");
			urlConn.connect();
			if (urlConn.getResponseCode() == 200) {
				InputStream in = urlConn.getInputStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new GZIPInputStream(in)));
				String temp = "";
				String jstr = "";
				while ((temp = reader.readLine()) != null) {
					jstr += temp;
				}
				reader.close();
				in.close();
				JSONObject obj = new JSONObject(jstr);
				result = obj.getString("result");
				if ("0".equals(result)) {
					result = null;
				} else {
					mTicketTable.put(ticket, result);
					if (mTicketTable.size() > 10000) {
						mTicketTable.clear();
					}
				}
			}
			urlConn.disconnect();
		} catch (Exception e) {
			return null;
		}
		return result;
	}
}
