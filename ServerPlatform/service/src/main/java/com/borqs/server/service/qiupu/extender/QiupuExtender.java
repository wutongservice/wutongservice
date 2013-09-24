package com.borqs.server.service.qiupu.extender;


import com.borqs.server.service.platform.Platform;
import com.borqs.server.service.platform.extender.PlatformExtender;
import com.borqs.server.service.qiupu.Qiupu;

public abstract class QiupuExtender extends PlatformExtender {
    protected Qiupu qiupu;

    public QiupuExtender() {
    }

    public Qiupu getQiupu() {
        return qiupu;
    }

    @Override
    public void setPlatform(Platform platform) {
        super.setPlatform(platform);
        this.qiupu = new Qiupu(platform.getTransceiverFactory());
        this.qiupu.setConfig(platform.getConfig());
    }
}
