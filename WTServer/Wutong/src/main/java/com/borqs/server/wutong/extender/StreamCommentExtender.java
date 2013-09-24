package com.borqs.server.wutong.extender;


import com.borqs.server.wutong.Constants;

public class StreamCommentExtender extends CommentExtender {
    public StreamCommentExtender() {
        super(Constants.POST_OBJECT, "post_id");
    }
}
