package com.borqs.server.intrapi.v1;

import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.intrapi.InternalApiSupport;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.privacy.PrivacyControlLogic;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@RoutePrefix("/internal")
@IgnoreDocument
public class Friend1Api extends InternalApiSupport {

    private FriendLogic friend;
    private AccountLogic account;
    private PrivacyControlLogic privacyControl;

    public Friend1Api() {
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public PrivacyControlLogic getPrivacyControl() {
        return privacyControl;
    }

    public void setPrivacyControl(PrivacyControlLogic privacyControl) {
        this.privacyControl = privacyControl;
    }

    @Route(url = "/internal/no_privacy_friend_ids")
    public List<Long> getNoPrivacyFriendIds(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        String[] v1Cols = new String[]{CompatibleUser.V1COL_USER_ID, CompatibleUser.V1COL_PROFILE_PRIVACY};
        Users friendIds = friend.getBorqsFriends(ctx, ctx.getViewer(), CompatibleUser.v1ToV2Columns(v1Cols));

        ArrayList<Long> l = new ArrayList<Long>();
        for (User user : friendIds) {
            if (StringUtils.isNotEmpty((String) user.getAddon("he_allowed", ""))) {
                l.add(user.getUserId());
            }
        }
        return l;
    }

    @Route(url = "/internal/friend_ids")
    public List<Long> getFriendIds(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long user = req.checkLong("user");
        PeopleIds pi = friend.getFriends(ctx, user);
        //RecordSet recs = p.getFriends(user, user, Integer.toString(FRIENDS_CIRCLE), "user_id, contact_info", 0, 10000000);

        ArrayList<Long> l = new ArrayList<Long>();
        for (PeopleId rec : pi) {
            l.add(rec.getIdAsLong());
        }
        return l;
    }


}
