package com.borqs.server.compatible;


import com.borqs.server.platform.feature.account.MiscInfo;
import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public class CompatibleMiscInfo {
    public static void serializeMisc(JsonGenerator jg, MiscInfo misc) throws IOException {
        misc.serialize(jg);
    }
}
