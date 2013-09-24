/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.borqs.server.base.util.email;

import com.borqs.server.base.conf.Configuration;

/**
 * @author Administrator
 */
public class AsyncMailTask implements AsyncTaskListener {
    public void sendEmail(String title, String to, String username,
                          String content, Configuration config, String type, String lang) {
        // TODO Auto-generated method stub
        AsyncSendMail sendMail = AsyncSendMail.getInstance(config);
        sendMail.asyncSendMail(title, to, username, content, type, lang);
    }

    @Override
    public void sendCustomEmail(String title, String to, String username, String content, Configuration config, String type, String lang) {
        AsyncSendMail sendMail = AsyncSendMail.getInstance(config);
        sendMail.asyncSendMail(title, to, username, content, false, type, lang);
    }

    public void sendEmailHTML(String title, String to, String username,
                              String content, Configuration config, String type, String lang) {
        AsyncSendMailINNOV sendMail = AsyncSendMailINNOV.getInstance(config);
        sendMail.asyncSendMailHTML(title, to, username, content, type, lang);
    }

    public void sendEmailElearningHTML(String title, String to, String username,
                              String content, Configuration config, String type, String lang) {
        AsyncSendMailElearning sendMail = AsyncSendMailElearning.getInstance(config);
        sendMail.asyncSendMailELearning(title, to, username, content, type, lang);
    }
}
