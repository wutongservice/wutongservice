package com.borqs.server.recv.stream;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.recv.ContextMqProcessor;

public class StreamRecv extends ContextMqProcessor{
    private StreamLogic stream;

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public StreamRecv() {

    }

    @Override
    protected void processContextObject(String queue, ContextObject ctxObj) {
        if (ctxObj == null)
            return;

        Context ctx =  ctxObj.context;
        int type = ctxObj.type;
        Post post = null;
        Long longs = null;
        if (type == ContextObject.TYPE_CREATE || type == ContextObject.TYPE_UPDATE) {
            post = (Post) ctxObj.object;

            // add the max size to share
            if(post.getToAndAddto().size()>400){
                throw new ServerException(E.TO_MANY_PEOPLEID,"Only can share to less than 400 guys!");
            }
        } else if (type == ContextObject.TYPE_DESTROY) {
            longs = (Long) ctxObj.object;
        }

        switch (type) {
            case ContextObject.TYPE_CREATE:
                stream.createPost(ctx, post);
                break;
            case ContextObject.TYPE_UPDATE:
                stream.updatePost(ctx, post);
                break;
            case ContextObject.TYPE_DESTROY:
                stream.destroyPosts(ctx, longs);
                break;
        }
    }
}