package com.borqs.server.qiupu;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.ConfigurableBase;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfo;
import com.borqs.server.platform.feature.TargetInfoFetcher;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.app.AppLogic;
import com.borqs.server.platform.feature.comment.CommentLogic;
import com.borqs.server.platform.feature.conversation.ConversationLogic;
import com.borqs.server.platform.feature.friend.FriendLogic;
import com.borqs.server.platform.feature.like.LikeLogic;
import com.borqs.server.platform.feature.login.LoginLogic;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.qiupu.servlet.QiupuServlet;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class QiupuFactory extends ConfigurableBase implements  Initializable, TargetInfoFetcher.Provider {
    private final SimpleQiupu qiupu = new SimpleQiupu();
    private AccountLogic account;
    private FriendLogic friend;
    private StreamLogic stream;
    private CommentLogic comment;
    private LikeLogic like;
    private ConversationLogic conversation;
    private AppLogic app;
    private LoginLogic login;

    public QiupuFactory() {
    }

    public AccountLogic getAccount() {
        return account;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public FriendLogic getFriend() {
        return friend;
    }

    public void setFriend(FriendLogic friend) {
        this.friend = friend;
    }

    public StreamLogic getStream() {
        return stream;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public CommentLogic getComment() {
        return comment;
    }

    public void setComment(CommentLogic comment) {
        this.comment = comment;
    }

    public LikeLogic getLike() {
        return like;
    }

    public void setLike(LikeLogic like) {
        this.like = like;
    }

    public ConversationLogic getConversation() {
        return conversation;
    }

    public void setConversation(ConversationLogic conversation) {
        this.conversation = conversation;
    }

    public AppLogic getApp() {
        return app;
    }

    public void setApp(AppLogic app) {
        this.app = app;
    }

    public LoginLogic getLogin() {
        return login;
    }

    public void setLogin(LoginLogic login) {
        this.login = login;
    }

    @Override
    public void setConfig(Configuration conf) {
        super.setConfig(conf);
        qiupu.setConfig(conf);
    }

    @Override
    public void init() throws Exception {
        qiupu.init();
    }

    @Override
    public void destroy() {
        qiupu.destroy();
    }

    public Qiupu createQiupu() {
        Qiupu q = new Qiupu(qiupu, account, friend, stream, comment, like, conversation);
        q.setConfig(getConfig());
        return q;
    }

    @Override
    public TargetInfo[] fetchTargetInfo(Context ctx, Target... targets) {
        String[] ids = Target.getIds(targets, Target.APK);
        Qiupu qp = createQiupu();
        String ua = "";
        try {
            RecordSet recs = qp.getApps(Long.toString(ctx.getViewer()), StringUtils.join(ids, ","), "package, version_code, architecture, app_name, icon_url", false, ua, QiupuServlet.getMinSDKFromUA(ua));
            for (Record rec : recs)
                rec.put("_apk_id_", ApkId.of(rec.checkGetString("package"), (int)rec.checkGetInt("version_code"), (int)rec.checkGetInt("architecture")));

            ArrayList<TargetInfo> tis = new ArrayList<TargetInfo>();
            for (String id : ids) {
                ApkId apkId = ApkId.parse(id);
                Record apkRec = findApkRecord(apkId, recs);
                if (apkRec != null)
                    tis.add(TargetInfo.of(Target.forApk(id), apkRec.checkGetString("app_name"), apkRec.checkGetString("icon_url")));
            }
            return tis.toArray(new TargetInfo[tis.size()]);
        } catch (AvroRemoteException e) {
            throw new ServerException(E.INVALID_APK, e);
        }
    }


    private static Record findApkRecord(ApkId apkId, RecordSet recs) {
        // match package, version_code, arch
        for (Record rec : recs) {
            ApkId apkId0 = (ApkId)rec.get("_apk_id_");
            if (apkId0.equals(apkId))
                return rec;
        }

        // match package only
        for (Record rec : recs) {
            ApkId apkId0 = (ApkId)rec.get("_apk_id_");
            if (StringUtils.equals(apkId0.package_, apkId.package_))
                return rec;
        }
        return null;
    }
}
