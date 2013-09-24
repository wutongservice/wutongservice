package com.borqs.server.base.util.email;

import com.borqs.server.base.conf.Configuration;

public class AsyncSendMailUtil {
    public static void sendEmail(String title, String to, String username, String content, Configuration config, String type, String lang) {
        ThreadPoolManager.getThreadPool().dispatch(
                new AsyncTask(new AsyncMailTask(), new Object[]{title, to, username, content, config, type, lang}) {
                    public void invoke(AsyncTaskListener listener, Object[] args) {
                        listener.sendEmail((String) args[0], (String) args[1], (String) args[2], (String) args[3], (Configuration) args[4], (String) args[5], (String) args[6]);
                    }
                });
    }

    public static void sendCustomEmail(String title, String to, String username, String content, Configuration config, String type, String lang) {
        ThreadPoolManager.getThreadPool().dispatch(
                new AsyncTask(new AsyncMailTask(), new Object[]{title, to, username, content, config, type, lang}) {
                    public void invoke(AsyncTaskListener listener, Object[] args) {
                        listener.sendCustomEmail((String) args[0], (String) args[1], (String) args[2], (String) args[3], (Configuration) args[4], (String) args[5], (String) args[6]);
                    }
                });
    }

    public static void sendEmailHTML(String title, String to, String username, String content, Configuration config, String type, String lang) {
        ThreadPoolManager.getThreadPool().dispatch(
                new AsyncTask(new AsyncMailTask(), new Object[]{title, to, username, content, config, type, lang}) {
                    public void invoke(AsyncTaskListener listener, Object[] args) {
                        listener.sendEmailHTML((String) args[0], (String) args[1], (String) args[2], (String) args[3], (Configuration) args[4], (String) args[5], (String) args[6]);
                    }
                });
    }

    public static void sendEmailElearningHTML(String title, String to, String username, String content, Configuration config, String type, String lang) {
        ThreadPoolManager.getThreadPool().dispatch(
                new AsyncTask(new AsyncMailTask(), new Object[]{title, to, username, content, config, type, lang}) {
                    public void invoke(AsyncTaskListener listener, Object[] args) {
                        listener.sendEmailElearningHTML((String) args[0], (String) args[1], (String) args[2], (String) args[3], (Configuration) args[4], (String) args[5], (String) args[6]);
                    }
                });
    }
}