package com.borqs.server.wutong.extender;


import com.borqs.server.wutong.Constants;

public class StreamLikeExtender extends LikeExtender {
    public StreamLikeExtender() {
        super(Constants.POST_OBJECT, "post_id");
    }
}
