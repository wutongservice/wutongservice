package com.borqs.server.service.platform.extender;


import com.borqs.server.service.platform.Constants;

public class StreamCommentExtender extends CommentExtender {
    public StreamCommentExtender() {
        super(Constants.POST_OBJECT, "post_id");
    }
}
