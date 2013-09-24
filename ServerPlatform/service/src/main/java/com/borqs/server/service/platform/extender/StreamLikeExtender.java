package com.borqs.server.service.platform.extender;


import com.borqs.server.service.platform.Constants;

public class StreamLikeExtender extends LikeExtender {
    public StreamLikeExtender() {
        super(Constants.POST_OBJECT, "post_id");
    }
}
