package com.borqs.server.base.util.email;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.I18nUtils;
import org.apache.commons.lang.StringUtils;


public class AsyncSendMailElearning {
    private final static Logger log = Logger.getLogger(AsyncSendMailElearning.class);
    private static AsyncSendMailElearning _instance;
	private String smtpServer;
	private String smtpPort;
	private String smtpFromUser;
	private String smtpFromUserPassword;
	private Configuration config;
	private String serverHost;
	boolean isNeedAuthLogin = false;


    private AsyncSendMailElearning(Configuration config){
//    	smtpServer = "smtp.gmail.com";
    	smtpServer = "smtp.bizmail.yahoo.com";
    	smtpPort = "465";
//    	smtpFromUser = "qiupu2011@gmail.com";
//    	smtpFromUserPassword = "borqsoms";

        //-------------2012-10-11-------
        smtpFromUser = "e_learning@borqs.com";
    	smtpFromUserPassword = "Borqs.com";
        //------------end--------------------
        //smtpFromUser = "innovation@borqs.com";
        //smtpFromUserPassword = "innovation";

    	this.config = config;
    	serverHost = config.getString("server.host", "api.borqs.com");
    }
    
    synchronized public static AsyncSendMailElearning getInstance(Configuration config){
    	if(_instance == null){
    		_instance = new AsyncSendMailElearning(config);
    	}
    	return _instance;
    }
    
    private static transient Dispatcher dispatcher;
    private boolean shutdown = false;
   
    public void asyncSendMailELearning(final String title, final String to,final String username, final String content, String type, String lang){
    	asyncSendMail(title, to, username, content, false, type, lang);
    }

    private String mailHead(String lang) {
		String content="";
		content = "<html>";
		content = content + "<head>";
		content = content + "<meta http-equiv=Content-Type content=text/html; charset=utf-8>";
		content = content + "<title>send mail</title>";		
		content = content + "</head>";
		content = content + "<body style:background-image:url(http://www.borqs.com/img/logo.jpg)>";
		content = content + "<table width=600 align=left cellpadding=3 cellspacing=3 style='border-top:#cfd8c5 1px solid;border-left:#cfd8c5 1px solid;border-right:#cfd8c5 1px solid;border-bottom:#cfd8c5 1px solid;' border=0>";
		content = content + "<tr valign=middle>";
		content = content + "<td valign=middle colspan=2 height=22 style='border-top:#c5f6c4 1px solid;border-left:#c5f6c4 1px solid;border-right:#c5f6c4 1px solid;border-bottom:#c5f6c4 1px solid;background-color:#b1cf92;;text-align:left;color:#024079;padding:2px 2px 0px 2px'><img width=40 border=0 src=http://" + serverHost + "/sys/icon/bpc.png><b>&nbsp;&nbsp;<font size:3>" + I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailhead.borqsaccount") + "</font></b></td>";
		content = content + "</tr>";
		content = content + "<tr>";
		content = content + "<td colspan=2 height=10></td>";
		content = content + "</tr>";
		content = content + "<tr>";
		content = content + "<td style='border-bottom:#999999 0px solid;color:#333333;padding:2px 2px 0px 2px;word-wrap:break-word; word-break:break-all;'>";
		
		return content;
	}
    
	private String mailBottom(String to, String type, String lang) {
		String content = "<br><br><br>";
		content = content + "</td></tr>";
		content = content + "<tr>";
		content = content + "<td colspan=2 style='border-bottom:#999999 0px solid;color:#333333;padding:2px 2px 0px 2px;word-wrap:break-word; word-break:break-all;'>";
			content = content + "<table width=100% align=center cellpadding=2 cellspacing=2 style='border-top:cef9d1 1px solid;border-left:cef9d1 1px solid;border-right:cef9d1 1px solid;border-bottom:cef9d1 1px solid;background-color:#edf9e0' border=0>";
			content = content + "<tr>";
			content = content + "<td>";
			
			String template = "";
            if(!StringUtils.equals(type, "email.essential") && !StringUtils.equals(type, "email.share_to"))
            {
//            content = content + "		这封邮件是发送给<a href=mailto:" + to + ">" + to + "</a>的，<br>"
//                + "     如果您不想再接收到此种类型的邮件，请点击<a href=http://" + serverHost + "/preferences/subscribe?user=" + to + "&type=" + type + "&value=1 target=_blank>退订</a>。<br>";
                template = I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailbottom.subscribe");
                String subscribe = SQLTemplate.merge(template, new Object[][]{
                        {"to", to},
                        {"serverHost", serverHost},
                        {"type", type}
                });
                content += subscribe;
            }
            else if(StringUtils.equals(type, "email.share_to"))
            {
                template = I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailbottom.shareto");
                String shareTo = SQLTemplate.merge(template, new Object[][]{
                        {"to", to},
                        {"serverHost", serverHost},
                        {"type", type}
                });
                content += shareTo;
            }
//            content = content + "		北京播思软件技术有限公司<br>"
//	      	  	+ "		<a href=http://" + serverHost + "/search?q=com.borqs.qiupu target=_blank>http://" + serverHost + "/search?q=com.borqs.qiupu</a>";
        template = I18nUtils.getBundleStringByLang(lang, "asyncsendmail.mailbottom.bottom");
        String bottom = SQLTemplate.merge(template, new Object[][]{
                {"serverHost", serverHost}
        });
        content += bottom;
        
			content = content + "</td>";
			content = content + "</tr>";
			content = content + "</table>";
		content = content + "</tr>";
		content = content + "</table>";
		content = content + "</body></html>";
		return content;
	}
	
	public void asyncSendMail(final String title, final String to,final String username, final String content, final boolean useTemplate, final String type, final String lang){
    	log.debug(null, "entering asyncSendMessage");
    	getDispatcher().invokeLater(new AsyncTask() {
            public void invoke() throws Exception {
            	log.debug(null, "entering asyncSendMessage invoke method");
            	SendMailElearning sendMail = new SendMailElearning();
            	sendMail.setHTML(true);
            	sendMail.setToField(to);
            	sendMail.setSubjectField(title);
            	//sendMail.setCustomerName(" ME");           	
            	
            	String mailContent = content;
            	if(useTemplate)
            	{
            		mailContent = mailHead(lang) + content + mailBottom(to, type, lang);
            	}
            	sendMail.setMessageText(mailContent);
            	sendMail.setMyAddress(smtpFromUser);
            	sendMail.sendMessage(title,smtpServer, smtpPort, smtpFromUser,smtpFromUserPassword);
            	//sendMail.sendMessage(content, smtpServer, smtpPort,smtpFromUser,smtpFromUserPassword); 
            	
            	//log
                log.debug(null, "Send email to: " + to);
            }
        });
    }

    public void shutdown(){
        synchronized (AsyncSendMailElearning.class) {
            if (shutdown) {
                throw new IllegalStateException("Already shut down");
            }
            getDispatcher().shutdown();
            dispatcher = null;
            shutdown = true;
        }
    }
    
    private Dispatcher getDispatcher(){
        if(shutdown){
            throw new IllegalStateException("Already shut down");
        }
        if (null == dispatcher) {
            dispatcher = new DispatcherFactory().getInstance();
        }
        return dispatcher;
    }

    abstract class AsyncTask implements Runnable {
       
        abstract void invoke() throws Exception;

        public void run() {
            try {
                   invoke();
            } catch (Exception te) {
               
            }
        }
    }
}
