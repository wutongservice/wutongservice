package com.borqs.server.service.platform.extender;


import com.borqs.server.base.data.DataException;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.RecordsExtender;
import com.borqs.server.base.data.RecordsExtenders;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;

import java.util.Set;

public abstract class PlatformExtender extends RecordsExtender {
    protected String viewerId = Constants.NULL_USER_ID;
    protected Platform platform;

    public PlatformExtender() {
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    protected abstract void extend0(RecordSet recs, Set<String> cols) throws AvroRemoteException;

    @Override
    public final void extend(RecordSet recs, Set<String> cols) {
        try {
            extend0(recs, cols);
        } catch (AvroRemoteException e) {
            throw new DataException(e);
        }
    }

    public static <E extends PlatformExtender> E setViewerId(E extender, String viewerId) {
        extender.setViewerId(viewerId);
        return extender;
    }

    public static void configPlatformExtenders(RecordsExtenders res, Platform platform, String viewerId) {
        for (RecordsExtender re : res) {
            if (re instanceof PlatformExtender) {
                ((PlatformExtender) re).setPlatform(platform);
                ((PlatformExtender) re).setViewerId(viewerId);
            }
        }
    }
}
