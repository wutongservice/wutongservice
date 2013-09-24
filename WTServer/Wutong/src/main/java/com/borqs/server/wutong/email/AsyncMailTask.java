/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.borqs.server.wutong.email;

import com.borqs.server.base.context.Context;

public class AsyncMailTask  {
    public void sendEmailFinal(Context ctx,EmailModel email) {
        AsyncSendMailFinal sendMail = AsyncSendMailFinal.getInstance(email);
        sendMail.asyncSendMailFinal(ctx,email);
    }
}
