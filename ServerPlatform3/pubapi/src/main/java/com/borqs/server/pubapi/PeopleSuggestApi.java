package com.borqs.server.pubapi;

import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.psuggest.PeopleSuggest;
import com.borqs.server.platform.feature.psuggest.PeopleSuggestLogic;
import com.borqs.server.platform.feature.psuggest.PeopleSuggests;
import com.borqs.server.platform.feature.psuggest.SuggestionReasons;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class PeopleSuggestApi extends PublicApiSupport {
    private AccountLogic account;
    private PeopleSuggestLogic psuggest;

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public PeopleSuggestLogic getPsuggest() {
        return psuggest;
    }

    public void setPsuggest(PeopleSuggestLogic psuggest) {
        this.psuggest = psuggest;
    }

    public PeopleSuggestApi() {
    }

    /**
     * 登录用户给别人推荐一批用户
     *
     * @remark peopleId的格式为：播思ID不用加前缀，联系人需要加前缀“c:”，即c:[id]，其它类型的用u:[id]
     * @group PeopleSuggest
     * @http-param to 推荐给谁，播思ID
     * @http-param suggested|suggestedusers 推荐哪些人，多个peopleId用逗号分隔
     * @http-return true
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/suggest/recommend")
    public void recommendPeoples(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long toUserId = req.checkLong("to");
        PeopleIds suggested = checkPeopleIds(req, "suggested");
        ArrayList<PeopleSuggest> suggests = new ArrayList<PeopleSuggest>();
        for (PeopleId p : suggested) {
            PeopleSuggest ps = PeopleSuggest.of(toUserId, p, SuggestionReasons.RECOMMENDER_USER, Long.toString(ctx.getViewer()));
            suggests.add(ps);
        }
        psuggest.create(ctx, suggests.toArray(new PeopleSuggest[suggests.size()]));
        resp.body(true);

    }


    /**
     * 拒绝推荐的用户
     *
     * @remark peopleId的格式为：播思ID不用加前缀，联系人需要加前缀“c:”，即c:[id]，其它类型的用u:[id]
     * @group PeopleSuggest
     * @http-param suggested 被拒绝的用户,peopleId，多个逗号隔开
     * @http-return true
     * @http-example {
     * "result":"true"
     * }
     */
    @Route(url = "/suggest/reject")
    public void rejectPeoples(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        PeopleIds suggested = checkPeopleIds(req, "suggested");
        psuggest.reject(ctx, suggested.toIdArray());
        resp.body(true);
    }


    private static final String[] SUGGESTED_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
            PeopleSuggests.COL_SUGGESTED_REASON,
            PeopleSuggests.COL_RECOMMEND_BY,
    };

    private static final String[] SOURCE_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };


    /**
     * 获取我可能认识的人的信息列表
     *
     * @group PeopleSuggest
     * @http-param count:40 需要返回记录的条数，默认为40条
     * @http-return JSON格式的可能认识的人列表
     * @http-example @suggested_user.json
     */
    @Route(url = "/suggest/get")
    public void getSuggested(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        int count = req.getInt("count", 40);
        if (count <= 0)
            count = 40;
        PeopleSuggests pss = psuggest.getSuggested(ctx, ctx.getViewer(), count);
        Users suggestUsers = pss.getSuggestedUsers(ctx, account, SUGGESTED_USER_COLUMNS, SOURCE_USER_COLUMNS);
        resp.body(RawText.of(suggestUsers.toJson(SUGGESTED_USER_COLUMNS, true)));
    }

}
