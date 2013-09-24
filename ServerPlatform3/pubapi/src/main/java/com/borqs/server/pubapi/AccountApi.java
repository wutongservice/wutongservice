package com.borqs.server.pubapi;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.feature.status.Status;
import com.borqs.server.platform.feature.status.StatusLogic;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.Copyable;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.TextEnum;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;
import org.apache.commons.collections.MapUtils;
import org.codehaus.jackson.JsonNode;

import java.util.HashMap;
import java.util.Map;


public abstract class AccountApi extends PublicApiSupport {

    protected AccountLogic account;
    protected StatusLogic status;

    public CibindLogic getCibind() {
        return cibind;
    }

    public void setCibind(CibindLogic cibind) {
        this.cibind = cibind;
    }

    protected CibindLogic cibind;

    protected AccountApi() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public StatusLogic getStatus() {
        return status;
    }

    public void setStatus(StatusLogic status) {
        this.status = status;
    }

    protected void showUser0(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        long[] userIds = req.checkLongArray("users", ",");
        String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
        cols = CollectionsHelper.removeElements(cols, new String[]{User.COL_PASSWORD, User.COL_DESTROYED_TIME});
        Users users = account.getUsers(ctx, cols, userIds);
        resp.body(RawText.of(users.toJson(cols, true)));
    }


    @RoutePrefix("/v2")
    @HttpExamplePackage(PackageClass.class)
    public static class Rpc extends AccountApi {

        /**
         * 查看指定ID的用户信息
         *
         * @remark 如果要查询的用户ID不存在，那么返回结果中将不包含此用户
         * <p>@std可以显示的列为${com.borqs.server.platform.feature.account.User.STANDARD_COLUMNS}</p>
         * <p>@full可以显示的列为${com.borqs.server.platform.feature.account.User.FULL_COLUMNS}</p>
         * <p>在tel,email,im,date,sip_address,url中，都含有列为type,info的子列，其type的可选值如下</p>
         * <p>在列tel中，tel的类型可以为${com.borqs.server.platform.feature.account.TelInfo.TYPES}</p>
         * <p>在列email中，email的类型可以为${com.borqs.server.platform.feature.account.EmailInfo.TYPES}</p>
         * <p>在列im中，im的的类型可以为${com.borqs.server.platform.feature.account.ImInfo.TYPES}</p>
         * <p>在列date中，date的类型可以为${com.borqs.server.platform.feature.account.DateInfo.TYPES}</p>
         * <p>在列sip_address中，sip_address的类型可以为${com.borqs.server.platform.feature.account.SipAddressInfo.TYPES}</p>
         * <p>在列url中，url的类型可以为${com.borqs.server.platform.feature.account.UrlInfo.TYPES}</p>
         * <p></p>
         * <p>在列organization中，其子列为${com.borqs.server.platform.feature.account.OrgInfo.COLUMNS}，type值可以为${com.borqs.server.platform.feature.account.OrgInfo.TYPES}</p>
         * <p>在列address中，其子列为${com.borqs.server.platform.feature.account.AddressInfo.COLUMNS}， type值可以为${com.borqs.server.platform.feature.account.AddressInfo.TYPES}</p>
         * <p>在列work_history中，其子列为${com.borqs.server.platform.feature.account.WorkHistory.COLUMNS}</p>
         * <p>在列education_history中，其子列为${com.borqs.server.platform.feature.account.EduHistory.COLUMNS}，其type值可能为${com.borqs.server.platform.feature.account.EduHistory.TYPES}</p>
         * <p>在列name中，其子列为${com.borqs.server.platform.feature.account.OrgInfo.COLUMNS}</p>
         *
         * pending_req_types的整数值定义如下：
         * public static final int REQ_EXCHANGE_VCARD = 1;
         * public static final int REQ_ADD_TO_FRIENDS = 2;
         * public static final int REQ_GROUP_INVITE = 3;
         * public static final int REQ_GROUP_JOIN = 4;
         * public static final int REQ_CHANGE_PROFILE = 5;
         * @group Account/User
         * @http-param users 逗号分隔的用户ID列表
         * @http-param cols:@std 逗号分隔的列名称，值为@std或者@full
         * @http-return JSON格式的用户列表，第一个示例为cols=@std返回，第二个示例为cols=@full
         * @http-example @user_std.json
         * @http-example @user_full.json
         */
        @Route(url = "/user/show")
        public void showUser(Request req, Response resp) {
            showUser0(req, resp);
        }

        /**
         * 用户更新自己的信息
         *
         * @remark 此方法的参数形式比较特殊，并不固定，分为几种情况，例如
         * <pre>
         *      {
         *          "nickname":"old_nickname",
         *          "profile": {
         *              "gender":"m"
         *          }
         *          "tel":[{
         *              "type":"home",
         *              "info":"13800138000"
         *          }]
         *      }
         * </pre>
         * <ul>
         * <li>更新nickname，使用http参数nickname=new_nickname</li>
         * <li>更新profile中的gender，使用http参数profile.gender=new_gender</li>
         * <li>更新tel，则需要在http参数tel中传入json格式的字符串</li>
         * <li>对于tel，email，work_history等复杂类型，需要进行全量更新，不支持增量更新</li>
         * </ul>
         * 此方法还有一些特点和限制，如下：
         * <ul>
         * <li>不能用此方法来更直接更新用户密码，使用/account/change_password方法更新密码</li>
         * <li>不能用此方法更新用户头像url，使用/account/upload_photo更新</li>
         * <li>可以更新虚拟字段display_name，会被自动拆分；但是在更新display_name时，不要同时更新name.first,name.middle,name.last以免冲突</li>
         * </ul>
         * @group Account/User
         * @http-return true
         * @http-example {
         * "result":"true"
         * }
         */
        @Route(url = "/account/update")
        public void updateUser(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            User org = account.getUser(ctx, User.FULL_COLUMNS, ctx.getViewer());
            if (org == null)
                throw new ServerException(E.INVALID_USER, "Invalid user id");

            User user = readUser(req, org);
            user.setUserId(ctx.getViewer());
            user.setPassword(null); // can't update password
            user.removeProperty(User.COL_PHOTO); // use /account/upload_photo
            account.update(ctx, user);
            resp.body(true);
            // for debug
            // Users r = new Users();
            // r.add(org);
            // r.add(user);
            // resp.body(RawText.of(r.toJson(User.V1_FULL_COLUMNS, true)));
        }

        private static User readUser(Request req, User org) {
            User user = new User();
            if (req.has(User.COL_DISPLAY_NAME))
                user.setName(NameInfo.split(req.checkString(User.COL_DISPLAY_NAME)));

            for (Schema.Column c : Schema.columns()) {
                String col = c.column;
                if (c.type == Schema.Column.Type.SIMPLE) {
                    if (!req.has(col))
                        continue;

                    Object value = Schema.parseSimpleValue(c.simpleType, req.checkString(col));
                    user.setProperty(col, value);
                } else if (c.type == Schema.Column.Type.OBJECT) {
                    Map<String, String> strMap = req.getMap(col + ".", true, null);
                    if (MapUtils.isEmpty(strMap))
                        continue;

                    Object oldVal = org.getProperty(col, null);
                    StringablePropertyBundle newVal = (StringablePropertyBundle) (oldVal != null ? ((Copyable) oldVal).copy() : c.newDefaultValue());
                    Map<Integer, Object> props = toProperties(strMap, newVal.subMap());
                    newVal.readProperties(props, true);
                    user.setProperty(col, newVal);
                } else if (c.type == Schema.Column.Type.OBJECT_ARRAY || c.type == Schema.Column.Type.SIMPLE_ARRAY) {
                    if (!req.has(col))
                        continue;

                    JsonNode jn = JsonHelper.parse(req.checkString(col));
                    Object v = User.propertyFromJsonNode(c, jn);
                    user.setProperty(col, v);
                }
            }
            return user;
        }

        private static Map<Integer, Object> toProperties(Map<String, String> strMap, TextEnum te) {
            HashMap<Integer, Object> props = new HashMap<Integer, Object>();
            for (Map.Entry<String, String> e : strMap.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                Integer nk = te.getValue(k);
                if (nk != null)
                    props.put(nk, v);
            }
            return props;
        }

        /**
         * 用户修改自己的密码
         *
         * @group Account/User
         * @http-param old|oldPassword 用来验证的用户的旧密码，md5大写形式
         * @http-param new|newPassword 用户的新密码，md5大写形式
         * @http-return true
         * @http-example {
         * "result":true
         * }
         */
        @Route(url = "/account/change_password")
        public void updatePassword(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            account.updatePassword(ctx, req.checkString("old"), req.checkString("new"), true);
            resp.body(true);
        }

        /**
         * 通过登录名称来查找用户ID
         *
         * @group Account/User
         * @http-param names|login_names 逗号分隔的可以用来登录的用户名称列表
         * @http-return 用户名称和用户ID的对应表，如果某个用户名称对应ID为0，则表示此用户不存在
         * @http-example @user_id.json
         */
        @Route(url = "/user/id")
        public void findUserId(Request req, Response resp) {
            Context ctx = checkContext(req, false);
            Map<String, Long> m = cibind.whoBinding(ctx, req.checkStringArray("names", "login_names", ","));
            resp.body(RawText.of(JsonHelper.toJson(m, false)));
        }


        @Route(url = "/account/upload_photo")
        public void uploadProfileImage(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            // TODO: xx
            throw new ServerException(E.UNSUPPORTED, "Unsupported");
        }


        /**
         * 获取此ticket所代表的用户信息
         *
         * @group Account/User
         * @http-param cols:@std 逗号分隔的列名称，值为@std或者@full
         * @http-return 当前登录的用户信息
         */
        @Route(url = "/whoami")
        public void who(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            String[] cols = User.expandColumns(req.getStringArray("cols", ",", User.STANDARD_COLUMNS));
            User user = account.getUser(ctx, cols, ctx.getViewer());
            if (user == null)
                throw new ServerException(E.INVALID_USER, "Invalid user");

            resp.body(RawText.of(user.toJson(cols, true)));
        }

        /**
         * 将邮件或者电话绑定到自己的帐号上
         *
         * @remark tel和email参数至少要有一个
         * @group Account/User
         * @http-param tel|phone: 要绑定的电话
         * @http-param email: 要绑定的邮件
         * @http-return true
         */
        @Route(url = "/account/bind")
        public void bind(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());

            if (req.has("phone") || req.has("tel"))
                cibind.bind(ctx, BindingInfo.MOBILE_TEL, req.checkString("tel", "phone"));

            if (req.has("email"))
                cibind.bind(ctx, BindingInfo.EMAIL, req.checkString("email"));

            resp.body(true);
        }

        /**
         * 解除一个绑定在自己帐号上的邮件或者电话
         *
         * @group Account/User
         * @http-param info 要解除绑定的邮件或者电话
         * @http-return true
         */
        @Route(url = "/account/unbind")
        public void unbind(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());
            cibind.unbind(ctx, req.checkString("info"));
            resp.body(true);
        }


        /**
         * 用户更新自己的状态信息
         *
         * @group Account/User
         * @http-param status|newStatus 要更新的状态文本
         * @http-return true
         */
        @Route(url = "/account/update_status")
        public void updateStatus(Request req, Response resp) {
            Context ctx = checkContext(req, true);
            AccountHelper.checkUser(account, ctx, ctx.getViewer());
            String s = req.checkString("status");
            status.updateStatus(ctx, new Status(s, DateHelper.nowMillis()));
            resp.body(true);
        }
    }
}
