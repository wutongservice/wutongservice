package com.borqs.server.service.platform.format;


import com.borqs.server.ServerException;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.TextFormatException;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlainUserFormatProvider extends PlatformFormatProvider {
    public PlainUserFormatProvider(Platform platform) {
        super(platform);
    }

    @Override
    public String getProvider() {
        return "plain_user";
    }

    @Override
    public Map<String, String> format(String viewer, Set<String> names, String display) {
        try {
            RecordSet recs = platform.getUsers(viewer, StringUtils.join(names, ","), "user_id, display_name, remark", false);
            HashMap<String, String> r = new HashMap<String, String>();
            for (Record rec : recs) {
                r.put(rec.checkGetString("user_id"), rec.checkGetString("display_name"));
            }
            return r;
        } catch (AvroRemoteException e) {
            throw new TextFormatException("Get users error", e);
        }
    }
}
