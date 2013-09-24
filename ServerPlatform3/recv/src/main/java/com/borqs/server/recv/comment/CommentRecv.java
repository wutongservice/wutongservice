package com.borqs.server.recv.comment;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.comment.Comment;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.mq.ContextObject;
import com.borqs.server.recv.ContextMqProcessor;

public class CommentRecv extends ContextMqProcessor{
    private CommentLogic comment;

    public CommentRecv() {

    }

    public CommentLogic getComment() {
        return comment;
    }

    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }

    @Override
    protected void processContextObject(String queue, ContextObject ctxObj) {
        if (ctxObj == null)
            return;

        Context ctx =  ctxObj.context;
        int type = ctxObj.type;
        Comment comment = null;
        long[] longs = null;
        if (type == ContextObject.TYPE_CREATE || type == ContextObject.TYPE_UPDATE) {
            comment = (Comment) ctxObj.object;
        } else if (type == ContextObject.TYPE_DESTROY) {
            longs = (long[]) ctxObj.object;
        }

        switch (type) {
            case ContextObject.TYPE_CREATE:
                this.comment.createComment(ctx, comment);
                break;
            case ContextObject.TYPE_UPDATE:
                this.comment.updateComment(ctx, comment);
                break;
            case ContextObject.TYPE_DESTROY:
                for (long commentId : longs)
                    this.comment.destroyComment(ctx, commentId);
                break;
        }
    }
}