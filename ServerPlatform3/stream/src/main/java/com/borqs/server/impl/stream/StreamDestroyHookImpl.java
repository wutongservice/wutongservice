package com.borqs.server.impl.stream;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostHook;


public class StreamDestroyHookImpl implements PostHook{
    private ConversationLogic conversationLogic;

    public ConversationLogic getConversationLogic() {
        return conversationLogic;
    }

    public void setConversationLogic(ConversationLogic conversationLogic) {
        this.conversationLogic = conversationLogic;
    }

    @Override
    public void before(Context ctx, Post data) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void after(Context ctx, Post post) {
        //delete all conversation of the Target is Target(POST ,postId)
        Target target = new Target(Target.POST,post.getPostId()+"");
        Target target2 = new Target(Target.PHOTO,post.getPostId()+"");
        Target [] targets = {target,target2};
        conversationLogic.delete(ctx,targets);
        
    }
}
