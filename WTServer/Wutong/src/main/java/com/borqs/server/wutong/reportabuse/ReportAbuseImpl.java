package com.borqs.server.wutong.reportabuse;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.qiupu.QiupuLogic;
import com.borqs.server.qiupu.QiupuLogics;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.folder.FolderLogic;
import com.borqs.server.wutong.photo.PhotoLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.lang.ArrayUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportAbuseImpl implements ReportAbuseLogic, Initializable {
    private static final Logger L = Logger.getLogger(ReportAbuseImpl.class);
    private ConnectionFactory connectionFactory;
    private String db;
    private String reportAbuseTable = "report_abuse";
    public final Schema reportAbuseSchema = Schema.loadClassPath(ReportAbuseImpl.class, "reportabuse.schema");

    public void init() {
        Configuration conf = GlobalConfig.get();
        this.connectionFactory = ConnectionFactory.getConnectionFactory(conf.getString("account.simple.connectionFactory", "dbcp"));
        this.db = conf.getString("account.simple.db", null);
        this.reportAbuseTable = conf.getString("reportAbuse.simple.reportAbuseTable", "report_abuse");
    }

    @Override
    public void destroy() {
        connectionFactory.close();
    }

    private SQLExecutor getSqlExecutor() {
        return new SQLExecutor(connectionFactory, db);
    }

    public boolean saveReportAbuse(Context ctx, Record reportAbuse) {
        final String METHOD = "saveReportAbuse";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, reportAbuse);
        final String SQL = "INSERT INTO ${table} ${values_join(alias, reportAbuse)}";

        String sql = SQLTemplate.merge(SQL,
                "table", reportAbuseTable, "alias", reportAbuseSchema.getAllAliases(),
                "reportAbuse", reportAbuse);
        SQLExecutor se = getSqlExecutor();
        L.op(ctx, "saveReportAbuse");
        long n = se.executeUpdate(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return n > 0;
    }

    @Override
    public int getReportAbuseCount(Context ctx,int target_type,String target_id,int appid) {
        final String METHOD = "saveReportAbuse";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, target_type,target_id);
        String sql = "select count(*) from " + reportAbuseTable + " where target_type='"+target_type+"' and target_id='"+target_id+"' and appid='"+appid+"'";
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return count.intValue();
    }

    @Override
    public int iHaveReport(Context ctx, String viewerId, int target_type,String target_id,int appid) {
        final String METHOD = "iHaveReport";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, target_type,target_id,appid);
        String sql = "select count(*) from " + reportAbuseTable + " where target_type='"+target_type+"' and target_id='"+target_id+"' and appid='"+appid+"' and user_id=" + viewerId + "";
        SQLExecutor se = getSqlExecutor();
        Number count = (Number) se.executeScalar(sql);
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return count.intValue();
    }

    @Override
    public boolean reportAbuserCreate(Context ctx, String viewerId, int target_type,String target_id,String reason,int appid, String ua, String loc) {
        final String METHOD = "reportAbuserCreate";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, target_type,target_id,appid);
        Record rec = new Record();
        rec.put("target_type", target_type);
        rec.put("target_id", target_id);
        rec.put("appid", appid);
        rec.put("user_id", viewerId);
        rec.put("created_time", DateUtils.nowMillis());
        rec.put("reason", reason);
        L.op(ctx, "reportAbuserCreate");
        boolean b = saveReportAbuse(ctx, rec);
        if (b) {
            String report = Constants.getBundleString(ua, "platform.sendmail.stream.report.abuse");
            Commons commons = new Commons();
            String m = "";
            if (target_type == Constants.POST_OBJECT) {
                StreamLogic streamLogic = GlobalLogics.getStream();
                Record this_stream = streamLogic.getPostP(ctx, target_id, "post_id,message");
                m = this_stream.getString("message");
                commons.sendNotification(ctx, Constants.NTF_REPORT_ABUSE,
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(viewerId),
                        commons.createArrayNodeFromStrings(target_id, viewerId, m, String.valueOf(target_type)),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(),
                        commons.createArrayNodeFromStrings(target_id),
                        commons.createArrayNodeFromStrings(target_id, viewerId, m, String.valueOf(target_type)),
                        commons.createArrayNodeFromStrings(report),
                        commons.createArrayNodeFromStrings(report),
                        commons.createArrayNodeFromStrings(target_id),
                        commons.createArrayNodeFromStrings(target_id, viewerId, String.valueOf(target_type))
                );
            }
            //   以下因为不发通知，暂时注释掉，等发通知了再打开
            /*
            if (target_type == Constants.FILE_OBJECT) {
                FolderLogic folderLogic = GlobalLogics.getFile();
                Record this_file = folderLogic.getStaticFileByIds(ctx,target_id).getFirstRecord();
                m = this_file.getString("title");

            }
            if (target_type == Constants.PHOTO_OBJECT) {
                PhotoLogic photoLogic = GlobalLogics.getPhoto();
                Record this_photo = photoLogic.getPhotoByIds(ctx, target_id).getFirstRecord();
                m = this_photo.getString("caption");
                if (m.trim() == "")
                    m = "share photo";
            }
            */
            if (target_type == Constants.APK_OBJECT) {
                try{
                //写入APK那个表
                    String apk_id[] = StringUtils.split(target_id, "-");
                    if (apk_id.length >= 2) {
                        QiupuLogic qp = QiupuLogics.getQiubpu();
                        RecordSet recs = qp.getApkByVersionCode(apk_id[0], Integer.valueOf(apk_id[1]));
                        if (recs.size() > 0) {
                            String report_user = recs.getFirstRecord().getString("report_user");
                            List<String> l = StringUtils2.splitList(report_user, ",", true);
                            if (!l.contains(viewerId))
                                l.add(viewerId);
                            String ls = StringUtils2.joinIgnoreBlank(",", l);
                            qp.updateReportUser(apk_id[0], Integer.valueOf(apk_id[1]), ls);
                        }
                    }
                } catch (Exception e) {
                    L.debug(ctx, "updateReportUser error");
                }
            }



        }
        if (L.isTraceEnabled())
            L.traceEndCall(ctx, METHOD);
        return true;

    }

    @Override
    public Map<String, Boolean> iHaveReport(Context ctx, String viewerId, String[] postIds,int target_type,int appid) {
        final String METHOD = "iHaveReport";
        if (L.isTraceEnabled())
            L.traceStartCall(ctx, METHOD, viewerId, postIds);

        final HashMap<String, Boolean> m = new HashMap<String, Boolean>();
        if (ArrayUtils.isNotEmpty(postIds)) {
            String sql = new SQLBuilder.Select()
                    .select("post_id")
                    .from(reportAbuseTable)
                    .where("user_id=${user_id} AND post_id IN (${post_ids}) AND target_type='"+target_type+"' AND appid='"+appid+"'",
                            "user_id", StringUtils.isBlank(viewerId) ? "0" : viewerId,
                            "post_ids", StringUtils.join(postIds, ","))
                    .toString();
            SQLExecutor se = getSqlExecutor();
            se.executeRecordHandler(sql, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    m.put(rec.getString("post_id"), true);
                }
            });

            CollectionUtils2.fillMissingWithValue(m, postIds, false);
        }
        L.traceEndCall(ctx, METHOD);
        return m;
    }
}
