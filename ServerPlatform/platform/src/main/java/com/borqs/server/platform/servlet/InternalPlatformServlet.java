package com.borqs.server.platform.servlet;


import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.rpc.GenericTransceiverFactory;
import com.borqs.server.base.util.Encoders;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.platform.group.GroupConstants;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Platform;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.borqs.server.service.platform.Constants.*;
import static com.borqs.server.service.platform.Constants.CIRCLE_TYPE_PUBLIC;

public class InternalPlatformServlet extends WebMethodServlet {
    private static final Logger L = LoggerFactory.getLogger(InternalPlatformServlet.class);
    private final GenericTransceiverFactory transceiverFactory = new GenericTransceiverFactory();
    private Record statistics = new Record();
    private Timer timer;
    private StatisticsTask task;
    private final long interval = 60 * 1000;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();

        transceiverFactory.setConfig(conf);
        transceiverFactory.init();

        timer = new Timer();
        task = new StatisticsTask();
        timer.schedule(task, interval, interval);
    }

    private boolean saveStatistics() throws AvroRemoteException {
         Platform p = platform();
//         L.debug("Begin save statistics");
         boolean r = p.saveStatistics(statistics);
//         L.debug("End save statistics");
         statistics.clear();
         return r;
    }

    private class StatisticsTask extends TimerTask {

        @Override
        public void run() {
            try {
                saveStatistics();
            } catch (AvroRemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        transceiverFactory.destroy();
        super.destroy();
    }

    private Platform platform() {
        Platform p = new Platform(transceiverFactory);
        p.setConfig(getConfiguration());
        return p;
    }

    @Override
    protected boolean before(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String local = req.getLocalAddr();
        String remote = req.getRemoteAddr();
        if ((local.startsWith("192.168.") || local.equals("127.0.0.1"))
                && (remote.startsWith("192.168.") || remote.equals("127.0.0.1")))
            return true;

        return false;
    }

    @WebMethod("internal/no_privacy_friend_ids")
    public List<Long> getNoPrivacyFriendIds(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();

        String user = qp.checkGetString("user");
        RecordSet recs = p.getFriends(user, user, Integer.toString(FRIENDS_CIRCLE), "user_id, contact_info", 0, 10000000);

        ArrayList<Long> l = new ArrayList<Long>();
        for (Record rec : recs) {
            if (!rec.checkGetBoolean("profile_privacy"))
                l.add(rec.checkGetInt("user_id"));
        }
        return l;
    }

    @WebMethod("internal/friend_ids")
    public List<Long> getFriendIds(QueryParams qp) throws AvroRemoteException, UnsupportedEncodingException {
        Platform p = platform();

        String user = qp.checkGetString("user");
        RecordSet recs = p.getFriends(user, user, Integer.toString(FRIENDS_CIRCLE), "user_id, contact_info", 0, 10000000);

        ArrayList<Long> l = new ArrayList<Long>();
        for (Record rec : recs) {
            l.add(rec.checkGetInt("user_id"));
        }
        return l;
    }

    @WebMethod("internal/verify_password")
    public long verifyPassword(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();

        String name = qp.checkGetString("name");
        String pwd = qp.checkGetString("pwd");
        if (qp.getBoolean("encode_pwd", false))
            pwd = Encoders.md5Hex(pwd);

        if (StringUtils.isBlank(name))
            return 0;

        name = StringUtils2.splitList(name, ",", true).get(0);

        String userId = p.getUserIds(name).getFirstRecord().getString("user_id", "0");
        if (userId.equals("0"))
            return 0;

        String userPwd = p.getUser(userId, userId, "password", false).getString("password", "");
        if (StringUtils.equals(userPwd, pwd))
            return Long.parseLong(userId);

        return 0;
    }

    @WebMethod("internal/getUsers")
    public RecordSet getUsers(QueryParams qp) throws AvroRemoteException {
        // RecordSet getUsers(String viewerId, String userIds, String cols, boolean privacyEnabled) throws AvroRemoteException
        Platform p = platform();
        return p.getUsers(qp.checkGetString("viewerId"), qp.checkGetString("userIds"), qp.checkGetString("cols"), qp.checkGetBoolean("privacyEnabled"));
    }

    @WebMethod("internal/getFriends0")
    public RecordSet getFriends0(QueryParams qp) throws AvroRemoteException {
        // RecordSet getFriends0(String userId, String circleIds, int page, int count) throws AvroRemoteException;
        Platform p = platform();
        return p.getFriends0(qp.checkGetString("userId"), qp.checkGetString("circleIds"), (int)qp.checkGetInt("page"), (int)qp.checkGetInt("count"));
    }

    @WebMethod("internal/hasUser")
    public boolean hasUser(QueryParams qp) throws AvroRemoteException {
        // boolean hasUser(String userId) throws AvroRemoteException;
        Platform p = platform();
        return p.hasUser(qp.checkGetString("userId"));
    }
    
    @WebMethod("internal/statistics")
    public Record httpCallStatistics(QueryParams qp) {
        String api = qp.checkGetString("api");
//        L.debug("api: " + api);
        long increment = statistics.getInt(api, 0L);
        increment++;
        statistics.put(api, increment);
        return Record.of(api, increment);
    }

    @WebMethod("internal/getFriends")
    public RecordSet getFriends(QueryParams qp) throws AvroRemoteException {
        // RecordSet getFriends(String viewerId, String userId, String circleIds, String cols, int page, int count) throws AvroRemoteException;
        Platform p = platform();
        return p.getFriends(
                qp.checkGetString("viewerId"),
                qp.checkGetString("userId"),
                qp.checkGetString("circleIds"),
                qp.checkGetString("cols"),
                (int)qp.checkGetInt("page"),
                (int)qp.checkGetInt("count"));
    }

    @WebMethod("internal/createAccount")
    public String createAccount(QueryParams qp) throws IOException {
        // public String createAccount(String login_email1,
        //                             String login_phone1,
        //                             String pwd,
        //                             String displayName,
        //                             String gender,
        //                             String imei,
        //                             String imsi,
        //                             String device,
        //                             String location)
        Platform p = platform();
        return p.createAccount(
                qp.checkGetString("loginEmail1"),
                qp.checkGetString("loginPhone1"),
                qp.checkGetString("pwd"),
                qp.checkGetString("displayName"),
                qp.getString("nickName", ""),
                qp.checkGetString("gender"),
                qp.checkGetString("imei"),
                qp.checkGetString("imsi"),
                qp.checkGetString("device"),
                qp.checkGetString("location"));
    }

    @WebMethod("internal/findUserIdByUserName")
    public String findUserIdByUserName(QueryParams qp) throws AvroRemoteException {
        // public String findUserIdByUserName(String username)
        Platform p = platform();
        return p.findUserIdByUserName(qp.checkGetString("username"));
    }


    @WebMethod("internal/updateAccount")
    public boolean updateAccount(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        // public boolean updateAccount(String userId, Record user)
        Record userRec = Record.fromJson(qp.checkGetString("user"));
        Platform p = platform();

        String ua = getDecodeHeader(req, "User-Agent", "");
        String lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        return p.updateAccount(qp.checkGetString("userId"), userRec, lang);
    }

    @WebMethod("internal/checkTicket")
    public String checkTicket(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.checkTicket(qp);
    }

    @WebMethod("internal/checkSign")
    public boolean checkSign(QueryParams qp) {
        Platform p = platform();
        try {
            p.checkSign(qp);
            return true;
        } catch (AvroRemoteException e) {
            return false;
        }
    }

    @WebMethod("internal/checkSignAndTicket")
    public String checkSignAndTicket(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.checkSignAndTicket(qp);
    }

    @WebMethod("internal/getUser")
    public Record getUser(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.getUser(qp.checkGetString("viewerId"), qp.checkGetString("userId"), qp.checkGetString("cols"));
    }

    @WebMethod("internal/getCircles")
    public RecordSet getCircles(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        boolean withPublicCircles = qp.getBoolean("with_public_circles", false);
        if (!withPublicCircles)
            return p.getCircles(qp.checkGetString("user"), qp.checkGetString("circleIds"), qp.checkGetBoolean("withUsers"));
        else {
            String userId = qp.checkGetString("user");
            String circleIds = qp.checkGetString("circleIds");
            boolean withMembers = qp.checkGetBoolean("withUsers");
            List<String> circles = StringUtils2.splitList(circleIds, ",", true);
            List<String> groups = p.getGroupIdsFromMentions(circles);
            circles.removeAll(groups);
            RecordSet recs = p.getCircles(userId, StringUtils2.joinIgnoreBlank(",", circles), withMembers);
            RecordSet recs0 = p.getGroups(GroupConstants.PUBLIC_CIRCLE_ID_BEGIN, GroupConstants.ACTIVITY_ID_BEGIN, userId, StringUtils2.joinIgnoreBlank(",", groups), GroupConstants.GROUP_LIGHT_COLS, withMembers);
            recs0.renameColumn(GRP_COL_ID, "circle_id");
            recs0.renameColumn(GRP_COL_NAME, "circle_name");
            for (Record rec : recs)
                rec.put("type", CIRCLE_TYPE_LOCAL);
            for (Record rec : recs0)
                rec.put("type", CIRCLE_TYPE_PUBLIC);
            recs.addAll(recs0);
            return recs;
        }
    }

    @WebMethod("internal/login")
    public Record login(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.login(qp.checkGetString("name"), qp.checkGetString("password"), qp.checkGetString("appId"));
    }

    @WebMethod("internal/logout")
    public boolean logout(QueryParams qp) throws AvroRemoteException {
        Platform p = platform();
        return p.logout(qp.checkGetString("ticket"));
    }

    protected static String getDecodeHeader(HttpServletRequest req, String name, String def) throws UnsupportedEncodingException {
        String v = req.getHeader(name);
        return StringUtils.isNotEmpty(v) ? java.net.URLDecoder.decode(v, "UTF-8") : def;
    }
}
