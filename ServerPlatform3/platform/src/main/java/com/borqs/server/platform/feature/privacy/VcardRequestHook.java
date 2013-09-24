package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.request.Request;
import com.borqs.server.platform.feature.request.RequestHook;
import com.borqs.server.platform.feature.request.RequestTypes;
import com.borqs.server.platform.feature.request.Requests;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;

public class VcardRequestHook implements RequestHook {

    private PrivacyControlLogic privacyControl;

    public VcardRequestHook() {
    }

    public VcardRequestHook(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    public PrivacyControlLogic getPrivacyControl() {
        return privacyControl;
    }

    public void setPrivacyControl(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    @Override
    public void before(Context ctx, Requests data) {
        // do nothing
    }

    @Override
    public void after(Context ctx, Requests data) {
        if (CollectionUtils.isEmpty(data))
            return;

        ArrayList<PrivacyEntry> pes = new ArrayList<PrivacyEntry>();
        for (Request req : data) {
            if (req == null || req.getType() != RequestTypes.REQ_EXCHANGE_VCARD)
                continue;

            pes.add(PrivacyEntry.of(req.getFrom(), PrivacyResources.RES_VCARD, PrivacyTarget.user(req.getTo()), true));
        }
        if (!pes.isEmpty())
            privacyControl.setPrivacy(ctx, pes.toArray(new PrivacyEntry[data.size()]));
    }
}
