package com.borqs.server.base.util.email;

import com.borqs.server.base.conf.Configuration;

public interface AsyncTaskListener
{
	void sendEmail(String title, String to, String username, String content, Configuration config, String type, String lang);
    void sendEmailHTML(String title, String to, String username, String content, Configuration config, String type, String lang);
    void sendEmailElearningHTML(String title, String to, String username, String content, Configuration config, String type, String lang);
    void sendCustomEmail(String title, String to, String username, String content, Configuration config, String type, String lang);
}