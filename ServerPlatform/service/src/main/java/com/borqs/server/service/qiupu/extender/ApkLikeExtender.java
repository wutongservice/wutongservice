package com.borqs.server.service.qiupu.extender;


import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.extender.LikeExtender;

public class ApkLikeExtender extends LikeExtender {
    public ApkLikeExtender() {
        super(Constants.APK_OBJECT, "apk_id");
    }
}
