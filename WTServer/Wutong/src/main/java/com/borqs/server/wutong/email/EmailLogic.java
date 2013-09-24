package com.borqs.server.wutong.email;


import com.borqs.server.base.context.Context;

import java.util.Map;

public interface EmailLogic  {

    boolean sendCustomEmailP(Context ctx,String title, String to, String username, String templateFile, Map<String, Object> map, String type, String lang);

    boolean sendEmailFinal(Context ctx,EmailModel email);

    boolean sendEmail(Context ctx, String title, String to, String username, String content, String type, String lang);

    boolean sendEmailDelay(Context ctx, EmailModel email);

    boolean saveFailureEmail(Context ctx,EmailModel emailModel);
}
