/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.borqs.server.base.util.email;

import com.borqs.server.base.conf.Configuration;

/**
 *
 * @author Administrator
 */
public class AsyncMailTask implements AsyncTaskListener {
    public void sendEmail(String title, String to, String username,
			String content, Configuration config, String type, String lang) {
		// TODO Auto-generated method stub
		AsyncSendMail sendMail = AsyncSendMail.getInstance(config);
		sendMail.asyncSendMail(title, to, username, content, type, lang);
	}
}
