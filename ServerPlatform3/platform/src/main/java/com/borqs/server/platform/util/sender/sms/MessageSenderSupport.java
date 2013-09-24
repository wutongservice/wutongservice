package com.borqs.server.platform.util.sender.sms;


import com.borqs.server.platform.util.sender.AsyncSender;


public class MessageSenderSupport {
    protected AsyncSender<Message> sender;

    public MessageSenderSupport() {
    }

    public AsyncSender<Message> getSender() {
        return sender;
    }

    public void setSender(AsyncSender<Message> sender) {
        this.sender = sender;
    }

    public void asyncMessage(Message msg) {
        sender.asyncSend(msg);
    }
}
