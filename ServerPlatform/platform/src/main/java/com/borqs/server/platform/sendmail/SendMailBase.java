package com.borqs.server.platform.sendmail;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.service.SendMail;
import org.apache.avro.AvroRemoteException;
import com.borqs.server.base.util.email.AsyncSendMailUtil;

public class SendMailBase extends RPCService implements SendMail {

	@Override
	public boolean sendEmail(CharSequence title, CharSequence to,
			CharSequence username, CharSequence content, CharSequence type, CharSequence lang)
			throws AvroRemoteException, ResponseError {
		String title0 = toStr(title);
		String to0 = toStr(to);
		String username0 = toStr(username);
		String content0 = toStr(content);
        String type0 = toStr(type);
        String lang0 = toStr(lang);

		AsyncSendMailUtil.sendEmail(title0, to0, username0, content0, getConfig(), type0, lang0);
		
		return true;
	}

    @Override
    public boolean sendCustomEmail(CharSequence title, CharSequence to,
                             CharSequence username, CharSequence content, CharSequence type, CharSequence lang)
            throws AvroRemoteException, ResponseError {
        String title0 = toStr(title);
        String to0 = toStr(to);
        String username0 = toStr(username);
        String content0 = toStr(content);
        String type0 = toStr(type);
        String lang0 = toStr(lang);

        AsyncSendMailUtil.sendCustomEmail(title0, to0, username0, content0, getConfig(), type0, lang0);

        return true;
    }

    @Override
   	public boolean sendEmailHTML(CharSequence title, CharSequence to,
   			CharSequence username, CharSequence content, CharSequence type, CharSequence lang)
   			throws AvroRemoteException, ResponseError {
   		String title0 = toStr(title);
   		String to0 = toStr(to);
   		String username0 = toStr(username);
   		String content0 = toStr(content);
           String type0 = toStr(type);
           String lang0 = toStr(lang);

   		AsyncSendMailUtil.sendEmailHTML(title0, to0, username0, content0, getConfig(), type0, lang0);

   		return true;
   	}

    @Override
   	public boolean sendEmailElearningHTML(CharSequence title, CharSequence to,
   			CharSequence username, CharSequence content, CharSequence type, CharSequence lang)
   			throws AvroRemoteException, ResponseError {
   		String title0 = toStr(title);
   		String to0 = toStr(to);
   		String username0 = toStr(username);
   		String content0 = toStr(content);
           String type0 = toStr(type);
           String lang0 = toStr(lang);

   		AsyncSendMailUtil.sendEmailElearningHTML(title0, to0, username0, content0, getConfig(), type0, lang0);

   		return true;
   	}
	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class getInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getImplement() {
		// TODO Auto-generated method stub
		return null;
	}

}
