package com.borqs.server.qiupu;


import com.borqs.server.ServerException;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostExpansion;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.qiupu.servlet.QiupuServlet;
import com.borqs.server.service.qiupu.Qiupu;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashSet;

public class ApkPostExpansion implements PostExpansion {
    private QiupuFactory qiupuFactory;


    public ApkPostExpansion() {
    }

    public QiupuFactory getQiupuFactory() {
        return qiupuFactory;
    }

    public void setQiupuFactory(QiupuFactory qiupuFactory) {
        this.qiupuFactory = qiupuFactory;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, Post.COL_ATTACHMENTS))
            expandApkAttachments(ctx, data);
    }

    private void expandApkAttachments(Context ctx, Posts posts) {
        LinkedHashSet<String> apkIds = new LinkedHashSet<String>();
        for (Post post : posts) {
            if (post == null || post.getType() != Post.POST_APK)
                continue;

            Target[] ts = Target.fromCompatibleStringArray(post.getAttachmentIds());
            for (Target t : ts) {
                if (t.type == Target.APK)
                    apkIds.add(t.id);
            }
        }

        Qiupu q = qiupuFactory.createQiupu();

        String viewerId = Long.toString(ctx.getViewer());
        String ua = ctx.getRawUserAgent();
        int minSDK = QiupuServlet.getMinSDKFromUA(ua);
        RecordSet apkRecs;
        try {
            apkRecs = q.getAppsFull(viewerId, StringUtils.join(apkIds, ","), false, ua, minSDK);
        } catch (AvroRemoteException e) {
            throw new ServerException(E.INVALID_APK, e, "Invalid apk");
        }

        for (Post post : posts) {
            if (post == null || post.getType() != Post.POST_APK)
                continue;

            Target[] apkTargets = Target.fromCompatibleStringArray(post.getAttachmentIds());
            if (ArrayUtils.isNotEmpty(apkTargets)) {
                RecordSet apkRec = getApks(apkRecs, Target.getIds(apkTargets, Target.APK));
                String json = apkRec.toString(false, true);
                post.setAttachments(json);
            } else {
                post.setAttachments("[]");
            }
        }
    }

    private static Record getApk(RecordSet apkRecs, String apkId) {
        for (Record rec : apkRecs) {
            if (rec != null && StringUtils.equals(rec.getString("apk_id"), apkId))
                return rec;
        }

        String packageName = ApkId.parse(apkId).package_;
        for (Record rec : apkRecs) {
            if (rec == null)
                continue;

            if (StringUtils.equals(rec.getString("package"), packageName))
                return rec;
        }
        return null;
    }

    private static RecordSet getApks(RecordSet apkRecs, String[] apkIds) {
        RecordSet recs = new RecordSet();
        for (String apkId : apkIds) {
            Record rec = getApk(apkRecs, apkId);
            if (rec != null)
                recs.add(rec);
        }
        return recs;
    }
}
