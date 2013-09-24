package com.borqs.server.pubapi;

import com.borqs.server.ServerException;
import com.borqs.server.compatible.CompatibleContactInfo;
import com.borqs.server.compatible.CompatibleRequest;
import com.borqs.server.compatible.CompatibleUser;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.request.RequestLogic;
import com.borqs.server.platform.feature.request.RequestTypes;
import com.borqs.server.platform.feature.request.Requests;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class RequestApi extends PublicApiSupport {

    private AccountLogic account;
    private RequestLogic request;

    public RequestApi() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public RequestLogic getRequest() {
        return request;
    }

    public void setRequest(RequestLogic request) {
        this.request = request;
    }



//    /**
//     * 发送或处理请求
//     *
//     * @remark 调用的url格式为/request/[请求类型]，例如：/request/exchange_vcard，发送交换名片请求。
//     *         请求类型包括exchange_vcard，add_friend，change_tel，change_email，group_invite和group_join几种类型。
//     *         /request/done处理一个或者多个请求，此时请求参数为requests。
//     * @group Request
//     * @http-param to 向这个userid的用户发起请求
//     * @http-param app:0 来自哪个应用
//     * @http-param message:空串 请求附加信息
//     * @http-param data:空串 请求附加数据
//     * @http-param requests 当url为/request/done时使用，要处理的request id，逗号分隔
//     * @http-return true
//     * @http-example {
//     * "result":"true"
//     * }
//     */
//    @Route(url = "/request/:req_type")
//    public void createRequest(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long viewerId = ctx.getViewer();
//        String s = req.checkString("req_type");
//        if (StringUtils.equals(s, "get")) {
//            getRequest0(req, resp);
//        } else if (StringUtils.equals(s, "count")) {
//            getPendingCount0(req, resp);
//        } else if (StringUtils.equals(s, "done")) {
//            doneRequest(req, resp);
//        } else {
//            int type = typeEnum.checkStrictParse(s);
//            long to = req.checkLong("to");
//            int app = req.getInt("app", 0);
//            String message = req.getString("message", "");
//            String data = req.getString("data", "");
//
//            com.borqs.server.platform.feature.request.Request request
//                    = new com.borqs.server.platform.feature.request.Request(viewerId, to, app, type, message, data);
//            request.create(ctx, request);
//            resp.body(true);
//        }
//    }
//
//    private void getPendingCount0(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long viewerId = ctx.getViewer();
//        int app = req.getInt("app", 0);
//        int type = typeEnum.checkStrictParse(req.getString("type", "all"));
//
//        resp.body(request.getPendingCount(ctx, viewerId, app, type));
//    }
//
//    /**
//     * 获取未处理的请求数量
//     *
//     * @remark 调用的url格式为/request/count/[用户id]，例如：/request/count/10001，获取id为10001的用户的未处理请求数量。
//     *         /request/count获取当前登录用户的未处理请求数量。
//     * @group Request
//     * @http-param app:0 来自哪个应用
//     * @http-param type:all 请求类型，包括exchange_vcard，add_friend，change_tel，change_email，group_invite和group_join几种类型，默认为all，取所有类型。
//     * @http-return integer
//     * @http-example {
//     * "result":3
//     * }
//     */
//    @Route(url = "/request/count/:to")
//    public void getPendingCount(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long to = req.checkLong("to");
//        int app = req.getInt("app", 0);
//        int type = typeEnum.checkStrictParse(req.getString("type", "all"));
//
//        resp.body(request.getPendingCount(ctx, to, app, type));
//    }
//
//    private void getRequest0(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long viewerId = ctx.getViewer();
//        String status = req.getString("status", "pending");
//        int app = req.getInt("app", 0);
//        int type = typeEnum.checkStrictParse(req.getString("type", "all"));
//        int limit = req.getInt("limit", 0);
//
//        Requests requests = new Requests();
//        if (StringUtils.equals(status, "pending"))
//            requests.addAll(request.getPendingRequests(ctx, viewerId, app, type));
//        else if (StringUtils.equals(status, "done"))
//            requests.addAll(request.getDoneRequests(ctx, viewerId, app, type, limit));
//        else
//            requests.addAll(request.getAllRequests(ctx, viewerId, app, type, limit));
//
//        resp.body(RawText.of(JsonHelper.toJson(requests, false)));
//    }
//
//    /**
//     * 获取请求列表
//     *
//     * @remark 调用的url格式为/request/get/[用户id]，例如：/request/get/10001，获取id为10001的用户的请求列表。
//     *         /request/get获取当前登录用户的请求列表。
//     * @group Request
//     * @http-param status:pending 指定获取请求列表的状态，默认是pending-未处理，done-已处理，其它值获取所有状态
//     * @http-param app:0 来自哪个应用
//     * @http-param type:all 请求类型，包括exchange_vcard，add_friend，change_tel，change_email，group_invite和group_join几种类型，默认为all，取所有类型。
//     * @http-param limit:20 需要返回记录的条数，默认为20条
//     * @http-return JSON格式的请求列表
//     * @http-example @request.json
//     */
//    @Route(url = "/request/get/:to")
//    public void getRequest(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long to = req.checkLong("to");
//        String status = req.getString("status", "pending");
//        int app = req.getInt("app", 0);
//        int type = typeEnum.checkStrictParse(req.getString("type", "all"));
//        int limit = req.getInt("limit", 20);
//
//        Requests requests = new Requests();
//        if (StringUtils.equals(status, "pending"))
//            requests.addAll(request.getPendingRequests(ctx, to, app, type));
//        else if (StringUtils.equals(status, "done"))
//            requests.addAll(request.getDoneRequests(ctx, to, app, type, limit));
//        else
//            requests.addAll(request.getAllRequests(ctx, to, app, type, limit));
//
//        resp.body(RawText.of(JsonHelper.toJson(requests, false)));
//    }
//
//    private void doneRequest(Request req, Response resp) {
//        Context ctx = checkContext(req, true);
//        long viewerId = ctx.getViewer();
//        long[] requestIds = req.checkLongArray("requests", ",");
//
//        request.done(ctx, requestIds);
//        resp.body(true);
//    }


    private static final String[] REQUEST_COLUMNS = {
            com.borqs.server.platform.feature.request.Request.COL_REQUEST_ID,
            com.borqs.server.platform.feature.request.Request.COL_SOURCE,
            com.borqs.server.platform.feature.request.Request.COL_APP,
            com.borqs.server.platform.feature.request.Request.COL_TYPE,
            com.borqs.server.platform.feature.request.Request.COL_CREATED_TIME,
            com.borqs.server.platform.feature.request.Request.COL_MESSAGE,
            com.borqs.server.platform.feature.request.Request.COL_DATA,
    };

    private static final String[] FROM_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_DISPLAY_NAME,
            User.COL_NICKNAME,
            User.COL_PHOTO,
    };

    /**
     * 获取此用户未处理的request列表
     *
     * @remark request的类型为
     *         <ul>
     *             <li>1 - 交换名片</li>
     *             <li>2 - 请求修改电话或者邮件</li>
     *         </ul>
     * @group Request
     * @http-param type:0 获取指定类型的request，默认为0，表示不限制特定类型的request
     * @http-return JSON格式的请求列表
     * @http-example @request.json
     */
    @Route(url = "/request/get")
    public void getRequests(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        Requests requests = request.getPendingRequests(ctx, ctx.getViewer(), ctx.getApp(),
                req.getInt("type", com.borqs.server.platform.feature.request.Request.TYPE_ANY));

        resp.body(RawText.of(requests.toJson(REQUEST_COLUMNS, true)));
    }


    /**
     * 获取未处理的请求数量
     *
     * @group Request
     * @http-param type:0 获取指定类型的request，默认为0，表示不限制特定类型的request
     * @http-return 未处理的request数量
     * @http-example {
     * "result":3
     * }
     */
    @Route(url = "/request/count")
    public void getRequestCount(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long count = request.getPendingCount(ctx, ctx.getViewer(), ctx.getApp(),
                req.getInt("type", com.borqs.server.platform.feature.request.Request.TYPE_ANY));
        resp.body(count);
    }

    /**
     * 向服务器反馈已经处理完指定request_id的request，处理完成后/request/get将不会再返回此条request
     *
     * @group Request
     * @http-param request 要处理的request_id
     * @http-return true
     */
    @Route(url = "/request/done")
    public void doneRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long reqId = req.checkLong("request");
        request.done(ctx, reqId);
        resp.body(true);
    }


    /**
     * 向目标用户发送交换名片的request
     *
     * @remark 此request有两个发送途径
     *          <ul>
     *              <li>用户主动调用此方法发起</li>
     *              <li>如果用户设置自己的名片向对方可见，那么系统会自动以用户的名义向对方发起这个request</li>
     *          </ul>
     * @group Request
     * @http-param to 向这个id指定的用户发送request
     * @http-param message: 在发送时附加的一个给对方用户查看的消息文本
     * @http-return true
     */
    @Route(url = "/request/send_exchange_vcard")
    public void sendExchangeVcardRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        long to = AccountHelper.checkUser(account, ctx, req.checkLong("to"));
        String message = req.getString("message", "");

        com.borqs.server.platform.feature.request.Request newReq =
                com.borqs.server.platform.feature.request.Request.newRandom(ctx.getViewer(), to, ctx.getApp(),
                        RequestTypes.REQ_EXCHANGE_VCARD, message, "");
        request.create(ctx, newReq);
        resp.body(true);
    }


    private static final String[] TO_USER_COLUMNS = {
            User.COL_USER_ID,
            User.COL_TEL,
            User.COL_EMAIL,
            User.COL_IM,
            User.COL_SIP_ADDRESS,
    };
    /**
     * 向对方用户发起修改对方邮件或者电话的request
     *
     * @remark 对方收取到的request中，data值为"data":{"type":"email", "old":"old_email", "new":"new_email", "v1Type":"4"}
     * @group Request
     * @http-param to 向这个id指定的用户发送request
     * @http-param message: 在发送时附加的一个给对方用户查看的消息文本
     * @http-param type 如果更改对方的邮件，此值为email；如果更改对方的电话，此值为tel
     * @http-param old: 需要被更改的邮件或者电话，如果此值为空串则表示为对方补齐新的邮件或者电话
     * @http-param new 为对方修改的新的邮件或者电话
     * @http-return true
     */
    @Route(url = "/request/send_change_profile")
    public void sendChangeTelRequest(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        long to = req.checkLong("to");

        User toUser = account.getUser(ctx, TO_USER_COLUMNS, to);
        if (toUser == null)
            throw new ServerException(E.INVALID_USER, "Invalid 'to'");

        String message = req.getString("message", "");

        String type = req.checkString("type");
        if (!ArrayHelper.inArrayIgnoreCase(type, "tel", "email"))
            throw new ServerException(E.PARAM, "Illegal type " + type);

        String old = req.getString("old", "");
        Record data = Record.of("type", type, "old", old, "new", req.checkString("new"));
        String v1Type = getV1Type(type, old, toUser);
        data.put("v1Type", v1Type);
        com.borqs.server.platform.feature.request.Request newReq =
                com.borqs.server.platform.feature.request.Request.newRandom(ctx.getViewer(), to, ctx.getApp(),
                        RequestTypes.REQ_CHANGE_PROFILE, message, data.toJson());

        request.create(ctx, newReq);
        resp.body(true);
    }

    // compatible v1 API
    private String getV1Type(String type, String old, User toUser) {
        Map<String, String> oldCI = CompatibleContactInfo.toContactInfo(toUser.getTel(), toUser.getEmail(), toUser.getIm(), toUser.getSipAddress());
        if (old.isEmpty()) {
            if (type.equals("tel")) {
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_MOBILE_TELEPHONE_NUMBER, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_MOBILE_TELEPHONE_NUMBER;
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_MOBILE_2_TELEPHONE_NUMBER, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_MOBILE_2_TELEPHONE_NUMBER;
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_MOBILE_3_TELEPHONE_NUMBER, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_MOBILE_3_TELEPHONE_NUMBER;
            } else if (type.equals("email")) {
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_EMAIL_ADDRESS, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_EMAIL_ADDRESS;
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_EMAIL_2_ADDRESS, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_EMAIL_2_ADDRESS;
                if (MapUtils.getString(oldCI, CompatibleRequest.CI_EMAIL_3_ADDRESS, "").isEmpty())
                    return CompatibleRequest.V1TYPE_CHANGE_EMAIL_3_ADDRESS;
            }
        } else {
            for (Map.Entry<String, String> e : oldCI.entrySet()) {
                if (StringUtils.equals(e.getValue(), old))
                    return CompatibleRequest.getChangeProfileV1Type(e.getKey());
            }
        }
        return "";
    }
}
