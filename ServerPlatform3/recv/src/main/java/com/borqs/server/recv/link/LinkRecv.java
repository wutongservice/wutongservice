package com.borqs.server.recv.link;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.link.LinkLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.recv.ContextMqProcessor;

public class LinkRecv extends ContextMqProcessor{
    private StreamLogic stream;
    private LinkLogic linkLogic;

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public LinkLogic getLinkLogic() {
        return linkLogic;
    }

    public void setLinkLogic(LinkLogic linkLogic) {
        this.linkLogic = linkLogic;
    }

    public LinkRecv() {

    }

    @Override
    protected void processContextObject(String queue, ContextObject ctxObj) {
        if (ctxObj == null)
            return;

        Context ctx =  ctxObj.context;
        Post post = (Post) ctxObj.object;;

        if(post == null)
            return;
        
        linkLogic.get(ctx,post.getMessage());

    }
}