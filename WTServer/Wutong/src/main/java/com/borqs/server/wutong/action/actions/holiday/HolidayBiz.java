package com.borqs.server.wutong.action.actions.holiday;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.base.util.StringMap;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.account2.AccountLogic;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.email.AsyncMailTask;
import com.borqs.server.wutong.email.EmailModel;
import com.borqs.server.wutong.email.template.InnovTemplate;
import com.borqs.server.wutong.stream.StreamLogic;
import org.codehaus.jackson.JsonNode;
import org.codehaus.plexus.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class HolidayBiz implements ActionsBizLogic, Initializable {
    private static final PageTemplate pageTemplate = new PageTemplate(InnovTemplate.class);
    private static String HR_ID = "10405";//"14853";
    public static String HR_NAME = "梁丹楠";
    private String SERVER_HOST = GlobalConfig.get().getString("server.host", "api.borqs.com");

    public void init() {
        Configuration conf = GlobalConfig.get();
        HR_ID = conf.getString("HR_ID", "14853");
        HR_NAME = conf.getString("HR_NAME", "梁丹楠");
    }

    @Override
    public void destroy() {

    }

    /**
     * 处理业务流程
     * 1.发送邮件
     *
     * @param ctx
     * @param jn
     */
    @Override
    public void consumer(Context ctx, JsonNode jn) {

        long post_id = jn.path("post_id").getValueAsLong();
        JsonNode node = JsonHelper.parse(jn.get("app_data").getTextValue());
        String to = "";
        String cc = "";
        if (node.has("approved_by"))
            to = node.path("approved_by").getTextValue();
        if (node.has("mate"))
            cc = node.path("mate").getTextValue();

        if (StringUtils.isNotBlank(to))
            send(ctx, jn, post_id, node, to);
        if (StringUtils.isNotBlank(cc))
            send(ctx, jn, post_id, node, cc);

        saveHoliday(ctx, jn, post_id, node, to, cc);

    }

    private void saveHoliday(Context ctx, JsonNode jn, long post_id, JsonNode node, String to, String cc) {
        AccountLogic account = GlobalLogics.getAccount();
        long owner = jn.path("source").getValueAsLong();

        RecordSet toRs = account.getUsers(ctx, to, "display_name");
        RecordSet ccRs = account.getUsers(ctx, cc, "display_name");
        Record ownerRecord = account.getUser(ctx, String.valueOf(owner), String.valueOf(owner), "display_name");
        StringBuffer toBf = new StringBuffer();
        StringBuffer ccBf = new StringBuffer();
        for (Record r : toRs) {
            toBf.append(r.getString("display_name"));
        }
        for (Record r : ccRs) {
            ccBf.append(r.getString("display_name"));
        }

        Record r = new Record();
        r.put("approver", toBf.toString());
        r.put("approver_id", to);
        r.put("relation_id", post_id);
        r.put("mate_id", cc);
        r.put("mate", ccBf.toString());
        r.put("owner_id", owner);
        r.put("owner", ownerRecord.getString("display_name"));
        r.put("holi_type", node.path("holiday_type").getTextValue());
        r.put("apply_time", DateUtils.formatDateAndSecond(DateUtils.now()));
        r.put("holi_start_time", node.path("start_time").getTextValue());
        r.put("days", node.path("days").getTextValue());
        r.put("dep", node.path("department").getTextValue());
        GlobalLogics.getHolidayRecord().saveHolidayRecord(ctx, r);
    }


    private void send(Context ctx, JsonNode jn, long post_id, JsonNode node, String to) {
        long userId = jn.path("source").getValueAsLong();
        String userName = GlobalLogics.getAccount().getUser(ctx, String.valueOf(userId), String.valueOf(userId), "display_name").getString("display_name");
        String department = "";
        String holiType = "";
        int  days = 1;
        if(node.has("department"))
             department = node.path("department").getTextValue();
        if(node.has("holiday_type"))
            holiType = node.path("holiday_type").getTextValue();
        String nStartTimeStr = node.path("start_time").getTextValue();
        if(node.has("holiday_type"))
            days = node.path("days").getValueAsInt();
        String message = userName + " " + "请" + holiType + days + "天，请批准";

        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date dataStart = new Date();
        try {
            dataStart = formatDate.parse(nStartTimeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long nStartTime = dataStart.getTime();
        String startTime = DateUtils.formatDateLocale(nStartTime);
        String month = DateUtils.getMonth(nStartTime);
        String day = DateUtils.getDay(nStartTime);
        String weekday = DateUtils.getWeekday(nStartTime);
        String time = DateUtils.getTime(nStartTime);

        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        List<String> emailList = new ArrayList<String>();

        Record user = addEmail(ctx, to, emailList);

        String to_name = user.getString("display_name");
        map.put("displayName", to_name);

        StringMap qp2 = new StringMap();
        //得到circleIds、user_ids
        qp2.put("post_id", post_id);
        qp2.put("secretly", true);
        qp2.put("type", "accept");

        String acceptUrl = "http://" + SERVER_HOST + "/actionHoliday/holiday?" + Commons.getEmailActionUrl(new Context(), to, qp2);
        StringMap qp1 = new StringMap();
        //得到circleIds、user_ids
        qp1.put("post_id", post_id);
        qp1.put("secretly", true);
        qp1.put("type", "reject");
        String rejectUrl = "http://" + SERVER_HOST + "/actionHoliday/holiday?" + Commons.getEmailActionUrl(new Context(), to, qp1);

        long key = DateUtils.nowMillis();
        map.put("acceptUrl", GlobalLogics.getShortUrl().generalShortUrlWithExpired(acceptUrl,String.valueOf(key)));
        map.put("rejectUrl", GlobalLogics.getShortUrl().generalShortUrlWithExpired(rejectUrl,String.valueOf(key)));
        map.put("dep", department);
        map.put("startTime", startTime);
        map.put("message", message);
        map.put("month", month);
        map.put("day", day);
        map.put("weekday", weekday);
        map.put("time", time);

        String html = pageTemplate.merge("holiday.ftl", map);
        for (String e : emailList) {
            EmailModel email = EmailModel.getDefaultEmailModule(GlobalConfig.get());
            email.setContent(html);
            email.setTitle("请假单");
            email.setTo(e);
            email.setUsername(to_name);
            email.setRecord(new Record(map));

            new AsyncMailTask().sendEmailFinal(ctx, email);
        }
    }

    private Record addEmail(Context ctx, String to, List<String> emailList) {
        Record user = GlobalLogics.getAccount().getUser(ctx, to, to, "display_name,login_email1,login_email2,login_email3");

        String email1 = user.getString("login_email1");
        String email2 = user.getString("login_email2");
        String email3 = user.getString("login_email3");


        if (StringUtils.isNotBlank(email1))
            emailList.add(email1);
        if (StringUtils.isNotBlank(email2))
            emailList.add(email2);
        if (StringUtils.isNotBlank(email3))
            emailList.add(email3);
        return user;
    }


    /**
     * 回调函数
     * 1.comment
     * 2.发送邮件
     *
     * @param ctx
     * @param record
     */
    @Override
    public void callBack(Context ctx, Record record) {
        String type = record.getString("type");
        String post_id = record.getString("post_id");
        //根据post_id 查找app_data 发送回调邮件
        StreamLogic streamLogic = GlobalLogics.getStream();
        Record post = streamLogic.getPostP(ctx, post_id, "post_id,source,app_data");
        String appDataStr = post.getString("app_data");
        JsonNode appData = JsonHelper.parse(appDataStr);

        Record user = GlobalLogics.getAccount().getUser(ctx, post.getString("source"), post.getString("source"), "display_name,login_email1,login_email2,login_email3");

        List<String> emailList = new ArrayList<String>();

        // callback from http
        if (StringUtils.equals("accept", type)) {
            //send email to hr and owner and add a comment
            addEmail(ctx, post.getString("source"), emailList);
            for (String r : emailList) {
                sendEmailCallBack(ctx, appData, r, user.getString("display_name"), "您的请假已审批通过");
            }
            emailList.clear();
            addEmail(ctx, HR_ID, emailList);
            for (String r : emailList) {
                String holiType = appData.path("holiday_type").getTextValue();
                String nStartTimeStr = appData.path("start_time").getTextValue();
                int days = appData.path("days").getValueAsInt();
                String message = user.getString("display_name") + " " + "请" + holiType + days + "天";
                sendEmailCallBack(ctx, appData, r, HR_NAME, message);
            }

            GlobalLogics.getComment().createCommentP(ctx, ctx.getViewerIdString(), 2, post_id, "同意", "", true, "", "", "9", "0");
            updateHoliRecord(ctx, post_id, "1");
        } else if (StringUtils.equals("reject", type)) {
            //send email to owner and add a comment
            addEmail(ctx, post.getString("source"), emailList);
            for (String r : emailList) {
                sendEmailCallBack(ctx, appData, r, user.getString("display_name"), "您的请假未审批通过");
            }

            updateHoliRecord(ctx, post_id, "2");
            GlobalLogics.getComment().createCommentP(ctx, ctx.getViewerIdString(), 2, post_id, "不同意", "", true, "", "", "9", "0");
        }


        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void updateHoliRecord(Context ctx, String id, String status) {
        Record r = new Record();
        r.put("relation_id", id);
        r.put("finish_time", DateUtils.formatDateAndSecond(DateUtils.now()));
        r.put("holi_status", status);
        GlobalLogics.getHolidayRecord().updateHolidayRecord(ctx, r);
    }

    private void sendEmailCallBack(Context ctx, JsonNode jn, String to, String displayName, String content) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

        long nStartTime = DateUtils.nowMillis();
        String month = DateUtils.getMonth(nStartTime);
        String day = DateUtils.getDay(nStartTime);
        String weekday = DateUtils.getWeekday(nStartTime);
        String time = DateUtils.getTime(nStartTime);

        map.put("displayName", displayName);
        map.put("message", content);
        map.put("month", month);
        map.put("day", day);
        map.put("weekday", weekday);
        map.put("time", time);
        map.put("dep", jn.path("department").getTextValue());
        map.put("days", jn.path("days").getValueAsInt());
        map.put("startTime", jn.path("start_time").getTextValue());

        String html = pageTemplate.merge("holidayCallBack.ftl", map);
        EmailModel email = EmailModel.getDefaultEmailModule(GlobalConfig.get());
        email.setContent(html);
        email.setTitle("请假单");
        email.setTo(to);
        email.setUsername(displayName);
        email.setRecord(new Record(map));

        new AsyncMailTask().sendEmailFinal(ctx, email);
        //System.out.println(html);

    }
}
