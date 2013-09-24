package com.borqs.server.pubapi.v1;


import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatiblePeopleSuggest;
import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

import java.util.ArrayList;

@IgnoreDocument
public class PeopleSuggest1Api extends PublicApiSupport {
    private PeopleSuggestLogic psuggest;
    private AccountLogic account;

    public PeopleSuggest1Api() {
    }

    public PeopleSuggestLogic getPsuggest() {
        return psuggest;
    }

    public void setPsuggest(PeopleSuggestLogic psuggest) {
        this.psuggest = psuggest;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    @Route(url = "/suggest/recommend")
    public void recommend(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long toUserId = req.checkLong("touser");
        PeopleIds suggested = checkPeopleIds(req, "suggestedusers");
        ArrayList<PeopleSuggest> suggests = new ArrayList<PeopleSuggest>();
        for (PeopleId p : suggested) {
            PeopleSuggest ps = PeopleSuggest.of(toUserId, p, SuggestionReasons.RECOMMENDER_USER, Long.toString(ctx.getViewer()));
            suggests.add(ps);
        }
        psuggest.create(ctx, suggests.toArray(new PeopleSuggest[suggests.size()]));
        resp.body(true);
    }

    private static final String[] V1_SUGGESTED_USER_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_MIDDLE_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
            CompatiblePeopleSuggest.V1COL_SUGGEST_TYPE,
            CompatiblePeopleSuggest.V1COL_SUGGEST_REASON,
    };

    private static final String[] V1_SOURCE_USER_COLUMNS = {
            CompatibleUser.V1COL_USER_ID,
            CompatibleUser.V1COL_DISPLAY_NAME,
            CompatibleUser.V1COL_MIDDLE_NAME,
            CompatibleUser.V1COL_IMAGE_URL,
    };

    @Route(url = "/suggest/get")
    public void getSuggested(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int count = req.getInt("count", 100);
        if (count <= 0)
            count = 100;
        // TODO: getback
        PeopleSuggests pss = psuggest.getSuggested(ctx, ctx.getViewer(), count);
        Users suggestUsers = CompatiblePeopleSuggest.getSuggestUsers(pss, ctx, account, V1_SUGGESTED_USER_COLUMNS, V1_SOURCE_USER_COLUMNS);
        resp.body(RawText.of(CompatibleUser.usersToJson(suggestUsers, V1_SUGGESTED_USER_COLUMNS, true)));
    }

    @Route(url = "/suggest/refuse")
    public void refuse(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        PeopleIds suggested = checkPeopleIds(req, "suggested");
        psuggest.reject(ctx, suggested.toIdArray());
        resp.body(true);
    }
}
