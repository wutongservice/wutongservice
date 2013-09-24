package com.borqs.server.service.qiupu.extender;


import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.extender.CommentExtender;

public class ApkCommentExtender extends CommentExtender {
    public ApkCommentExtender() {
        super(Constants.APK_OBJECT, "apk_id");
    }
}
