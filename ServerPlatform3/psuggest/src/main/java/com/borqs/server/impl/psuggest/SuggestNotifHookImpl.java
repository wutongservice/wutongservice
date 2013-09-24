package com.borqs.server.impl.psuggest;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.maker.MakerTemplates;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.SuggestHook;
import com.borqs.server.platform.util.sender.notif.NotifSenderSupport;


public class SuggestNotifHookImpl {
    public static class SuggestUserHook extends NotifSenderSupport implements SuggestHook {

        @Override
        public void before(Context ctx, PeopleSuggest... data) {

        }

        @Override
        public void after(Context ctx, PeopleSuggest... data) {
            if (data != null)
                asyncSend(ctx, MakerTemplates.NOTIF_SUGGEST_USER, new Object[][]{
                        {"suggest", data}
                });
        }
    }
}
