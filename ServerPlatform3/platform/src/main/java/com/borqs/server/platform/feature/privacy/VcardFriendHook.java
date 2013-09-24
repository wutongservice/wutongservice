package com.borqs.server.platform.feature.privacy;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.Circle;
import com.borqs.server.platform.feature.friend.FriendsHook;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;


import java.util.ArrayList;
import java.util.List;

public class VcardFriendHook implements FriendsHook {

    public static final String CTX_TOGGLE_KEY = "vcard.friend.toggle";

    private PrivacyControlLogic privacyControl;

    public VcardFriendHook() {
    }

    public PrivacyControlLogic getPrivacyControl() {
        return privacyControl;
    }

    public void setPrivacyControl(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    @Override
    public void before(Context ctx, List<Entry> data) {
        // do nothing
    }

    @Override
    public void after(Context ctx, List<Entry> data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (!ctx.toggleEnabled(CTX_TOGGLE_KEY))
            return;


        ArrayList<PrivacyEntry> pes = new ArrayList<PrivacyEntry>();
        for (Entry e : data) {
            if (e == null)
                continue;

            if (e.friendId == null || !e.friendId.isUser())
                continue;

            boolean vcardAllow = ArrayUtils.contains(e.circleIds, Circle.CIRCLE_ADDRESS_BOOK);
            PrivacyEntry pe = new PrivacyEntry(e.userId, PrivacyResources.RES_VCARD, PrivacyTarget.user(e.friendId.getIdAsLong()), vcardAllow);
            pes.add(pe);
        }

        if (!pes.isEmpty())
            privacyControl.setPrivacy(ctx, pes.toArray(new PrivacyEntry[pes.size()]));
    }
}
