package com.borqs.server.platform.feature.conversation;


import com.borqs.server.platform.logic.Logic;

public abstract class ConversationBaseLogic implements Logic {
    protected ConversationLogic conversation;

    protected ConversationBaseLogic() {
    }

    protected ConversationBaseLogic(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public ConversationLogic getConversation() {
        return conversation;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }
}
