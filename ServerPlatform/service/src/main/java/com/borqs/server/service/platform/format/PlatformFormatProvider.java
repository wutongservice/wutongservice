package com.borqs.server.service.platform.format;


import com.borqs.server.base.util.TextFormatter;
import com.borqs.server.service.platform.Platform;

public abstract class PlatformFormatProvider implements TextFormatter.Provider {
    protected final Platform platform;

    protected PlatformFormatProvider(Platform platform) {
        this.platform = platform;
    }

    public Platform getPlatform() {
        return platform;
    }
}
