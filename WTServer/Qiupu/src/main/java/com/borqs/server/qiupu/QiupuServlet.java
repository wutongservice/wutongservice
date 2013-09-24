package com.borqs.server.qiupu;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.local.LocalSFS;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.ElapsedCounter;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.email.ThreadPoolManager;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.DirectResponse;
import com.borqs.server.base.web.webmethod.NoResponse;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.qiupu.util.apkinfo.ApkInfo;
import com.borqs.server.qiupu.util.apkinfo.ApkInfoReader;
import com.borqs.server.qiupu.util.market.AppMarketPageReader;
import com.borqs.server.qiupu.util.market.AsyncTask;
import com.borqs.server.qiupu.util.market.AsyncTaskListener;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.commons.WutongContext;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class QiupuServlet extends WebMethodServlet {
    private static final Logger L = LoggerFactory.getLogger(QiupuServlet.class);

    public static final int DEFAULT_APP_COUNT_IN_PAGE = 20;
    private StaticFileStorage apkStorage;
    private StaticFileStorage apkSubImgStorage;
    private static final PageTemplate pageTemplate = new PageTemplate(QiupuServlet.class);
    private Set<String> borqsApps = new HashSet<String>();
    private String qiupuParentPath;
    
    public QiupuServlet() {
    }

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = GlobalConfig.get();


        apkStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.apkStorage", LocalSFS.class.getName() + "|~/.apk"));
        apkStorage.init();
        
        apkSubImgStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.apkSubImgStorage", ""));
        apkSubImgStorage.init();
        
        String temp = conf.getString("qiupu.borqsApps", "com.msocial.freefb,com.tormas.home,com.android.omshome,oms.sns.main,com.borqs.qiupu,com.tormas.litesina,sys.info.jtbuaa");
        borqsApps = StringUtils2.splitSet(temp, ",", true);

        qiupuParentPath = conf.getString("qiupu.parent", "/home/zhengwei/data/apk/com/borqs/qiupu/");
    }

    @Override
    public void destroy() {
        apkStorage.destroy();
        super.destroy();
    }

    private Qiupu qiupu() {
        return new Qiupu();
    }

    @Override
    protected String getXmlDocumentPath() {
        return "document/qiupu";
    }

    @Override
    protected String getXmlDocument() {
        return getConfiguration().getBoolean("qiupu.servlet.document", false)
                ? TextLoader.loadClassPath(QiupuServlet.class, "qiupu_servlet_document.xml")
                : null;
    }

    protected static String getDecodeHeader(HttpServletRequest req, String name, String def) throws UnsupportedEncodingException {
        String v = req.getHeader(name);
        return StringUtils.isNotEmpty(v) ? java.net.URLDecoder.decode(v, "UTF-8") : def;
    }

    protected static int getMinSDKFromUA(String ua) {
        int minSDK = 1000;
        if (ua.length() > 0) {
            String[] s = StringUtils2.splitArray(ua, ";", true);
            if (s.length == 5) {
                String[] d = StringUtils2.splitArray(s[0], "-", true);
                if (d.length >= 3)
                    minSDK = Integer.parseInt(d[1]);
            }
        }
//        minSDK=1000;
        return minSDK;
    }

    @WebMethod("qiupu/app/all")
    public RecordSet getAllApps(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        ElapsedCounter ec = ctx.getElapsedCounter();
        ec.record("QiupuServlet.getAllApps begin");
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        RecordSet recs;
        if (!qp.getString("cols", "").equals("")) {
            recs = q.getAllApps(ctx, viewerId, qp.getString("category", "0"), qp.getBoolean("paid", false), qp.getString("sort", "download"), qp.getString("cols", ""), (int) qp.getInt("page", 0), (int) qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),minSDK);
        } else {
            recs = q.getAllAppsFull(ctx, viewerId, qp.getString("category", "0"), qp.getBoolean("paid", false), qp.getString("sort", "download"), (int) qp.getInt("page", 0), (int) qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),minSDK);
        }
        ec.record("QiupuServlet.getAllApps end");
        return recs;
    }

    @WebMethod("qiupu/app/last_installed")
    public RecordSet getLastInstalledApp(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK = getMinSDKFromUA(ua);
        return q.getLastInstalledApp(ctx, ctx.getViewerIdString(), qp.getBoolean("history_version", false), minSDK);
    }

    @WebMethod("qiupu/app/for")
    public RecordSet getUserApps(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String viewerId = "";
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String r = qp.getString("reason", "");
        String reason = r.equals("")?"":Integer.toString(Qiupu.REASONS.getValue(r));
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        if (!qp.getString("cols", "").equals("")) {
            return q.getUserApps(ctx, viewerId, qp.checkGetString("user"), reason, qp.getString("cols", ""), (int)qp.getInt("page", 0), (int)qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),qp.getString("apps",""),minSDK);
        } else {
            return q.getUserAppsFull(ctx, viewerId, qp.checkGetString("user"), reason, (int)qp.getInt("page", 0), (int)qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),qp.getString("apps",""),minSDK);
        }
    }

    @WebMethod("qiupu/category/for")
    public RecordSet getCategoryApps(QueryParams qp, HttpServletRequest req) throws Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String ua = getDecodeHeader(req, "User-Agent", "");
        return q.getTop1ApkByCategory(ctx, qp.getString("category",""));
    }

    @WebMethod("qiupu/search")
    public RecordSet search(QueryParams qp, HttpServletRequest req) throws Exception {
        Qiupu q = qiupu();
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        int minSDK =  getMinSDKFromUA(ctx.getUa());
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        if (!qp.getString("cols", "").equals("")) {
            return q.searchApps(ctx, viewerId,qp.getString("value", ""), qp.getString("cols", ""), (int)qp.getInt("page", 0),
                    (int)qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),minSDK);
        } else {
            return q.searchAppsFull(ctx, viewerId,qp.getString("value", ""), (int)qp.getInt("page", 0),
                    (int)qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),minSDK);
        }
    }

    @WebMethod("qiupu/active_down")
    public NoResponse activeDown(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String oldFileName = "com.borqs.qiupu-"
                + q.getMaxVersionCode(ctx, "com.borqs.qiupu",1000) + "-arm.apk";
        String filepath = qiupuParentPath + oldFileName;

        //check if file exists
        if (!(apkStorage instanceof OssSFS)) {
            File obj = new File(filepath);
            if (!obj.exists()) {
                throw new ServerException(BaseErrors.PLATFORM_GENERAL_ERROR, "BPC apk file is not exist");
            }
        }
        String login_name = qp.checkGetString("bind");
        String pwd = qp.checkGetString("password");
        //default filename
        String data = login_name + "abz_seperate_998" + pwd;
        data = URLEncoder.encode(data, "utf-8");
        String fileName = data + "beijj2012.com.borqs.qiupu.apk";

        //write stream
        ServletOutputStream out = resp.getOutputStream();
        resp.setHeader("Content-type", "application/vnd.android.package-archive");
        resp.setHeader("Content-disposition", "attachment; filename=" + fileName);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try
        {
            if (apkStorage instanceof OssSFS)
                bis = new BufferedInputStream(apkStorage.read(oldFileName, resp));
            else
                bis = new BufferedInputStream(new FileInputStream(filepath));
            bos = new BufferedOutputStream(out);
            byte[] buff = new byte[2048];
            int bytesRead;
            while(-1 != (bytesRead = bis.read(buff, 0, buff.length)))
            {
                bos.write(buff, 0, bytesRead);
            }
        }
        catch(IOException ioe)
        {
            System.out.println("Download BPC apk file failed");
        }
        finally
        {
            if(bis != null)
                bis.close();
            if(bos != null)
                bos.close();
        }

        return NoResponse.get();
    }

    @WebMethod("qiupu/scheme_image")
    public NoResponse getScheme_image(QueryParams qp, HttpServletResponse resp) {
        SFSUtils.writeResponse(resp, apkSubImgStorage, qp.checkGetString("file"));
        return NoResponse.get();
    }
    
    @WebMethod("qiupu/remove_app")
    public boolean removeuserApp(QueryParams qp,HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        return q.removeUserLinkApp(ctx, qp.getString("userId", ctx.getViewerIdString()), qp.getString("apps", ""),minSDK);
    }

    @WebMethod("qiupu/user/setting")
    public boolean userSetting(QueryParams qp) throws AvroRemoteException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        return q.userSettingAll(ctx, qp.getString("userId", ctx.getViewerIdString()), qp.getString("value", ""));
    }

    @WebMethod("qiupu/user/getsetting")
    public String getUserSettingAll(QueryParams qp) throws AvroRemoteException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        return q.getUserSettingAll(ctx, qp.getString("userId", ctx.getViewerIdString()));
    }
    
    @WebMethod("qiupu/app/setting")
    public boolean setAppPrivancy(QueryParams qp) throws AvroRemoteException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        return q.setUserSingleAppPrivancy(ctx, qp.getString("userId", ctx.getViewerIdString()),qp.checkGetString("app"), qp.getString("value","1"));
    }
    
    @WebMethod("qiupu/app/getsetting")
    public String getUserSingleAppPrivancy(QueryParams qp) throws AvroRemoteException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        return q.getUserSingleAppPrivancy(ctx, qp.getString("userId", ctx.getViewerIdString()),qp.checkGetString("app"));
    }

    @WebMethod("qiupu/favorite")
    public boolean setFavorite(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        int minSDK =  getMinSDKFromUA(ctx.getUa());
        String deviceid = Constants.parseUserAgent(java.net.URLDecoder.decode(req.getHeader("User-Agent"), "UTF-8"), "device");
        return q.setAppfavorite(ctx, qp.getString("userId", ctx.getViewerIdString()), qp.checkGetString("app"),deviceid,minSDK);
    }

    @WebMethod("qiupu/app/get")
    public RecordSet getApps(QueryParams qp,HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        if (!qp.getString("ticket", "").equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        if (!qp.getString("cols", "").equals("")) {
            return q.getApps(ctx, viewerId,qp.checkGetString("apps"), qp.getString("cols", ""),qp.getBoolean("history_version", false),ua,minSDK);
        } else {
            return q.getAppsFull(ctx, viewerId,qp.checkGetString("apps"),qp.getBoolean("history_version", false),ua,minSDK);
        }
    }

    @WebMethod("qiupu/app/shareto")
    public RecordSet getAppShareToMe(QueryParams qp,HttpServletRequest req) throws Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, true);
        int minSDK =  getMinSDKFromUA(ctx.getUa());
        return q.getAppsSharedToMe(ctx, ctx.getViewerIdString(), qp.getString("cols", ""),qp.getString("userIds", ""),qp.getBoolean("tome", false),qp.getString("getType", "stream"),qp.checkGetBoolean("friend"),  (int)qp.getInt("page", 0),
                    (int)qp.getInt("count", DEFAULT_APP_COUNT_IN_PAGE),qp.getBoolean("history_version", false),minSDK);
    }

    @WebMethod("qiupu/download")
    public NoResponse download(QueryParams qp, HttpServletResponse resp, HttpServletRequest req) throws AvroRemoteException, IOException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String file = qp.checkGetString("file");
        String ext = FilenameUtils.getExtension(file);
        int minSDK =  getMinSDKFromUA(ctx.getUa());
        if (StringUtils.equalsIgnoreCase(ext, "apk")) {
            String deviceid = Constants.parseUserAgent(java.net.URLDecoder.decode(req.getHeader("User-Agent"),"UTF-8"),"device");
            q.download(ctx, qp.getString("userId", ""), FilenameUtils.getBaseName(file),deviceid,minSDK);
        }
        
        SFSUtils.writeResponse(resp, apkStorage, file);
        return NoResponse.get();
    }
    
    @WebMethod("qiupu/nginxdownload")
    public boolean nginxDownload(QueryParams qp, HttpServletResponse resp, HttpServletRequest req) throws AvroRemoteException, IOException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String file = qp.checkGetString("file");
        String ext = FilenameUtils.getExtension(file);

        int minSDK =  getMinSDKFromUA(ctx.getViewerIdString());
        if (StringUtils.equalsIgnoreCase(ext, "apk")) {
            String deviceid = Constants.parseUserAgent(java.net.URLDecoder.decode(req.getHeader("User-Agent"),"UTF-8"),"device");
            q.download(ctx, qp.getString("userId", ""), FilenameUtils.getBaseName(file),deviceid,minSDK);
        }
        return true;
    }

    @WebMethod("qiupu/user")
    public RecordSet usedAppUsers(QueryParams qp) throws AvroRemoteException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        return q.getUsedAppUsers(ctx, viewerId, qp.getString("app", ""), qp.getString("reason", String.valueOf("installed")), qp.getString("cols", "user_id,display_name,image_url"), (int)qp.getInt("page", 0), (int)qp.getInt("count", 20));
    }


    @WebMethod("qiupu/suggest")
    public RecordSet suggestApps(QueryParams qp,HttpServletRequest req) throws ResponseError, Exception {
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        ElapsedCounter ec = ctx.getElapsedCounter();
        ec.record("QiupuServlet.suggestApps begin");
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String cols = qp.getString("columns", Qiupu.QAPK_FULL_COLUMNS);
        if(StringUtils.isBlank(cols))
        {
        	cols = Qiupu.QAPK_FULL_COLUMNS;
        }
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String category = qp.getString("category", "0");
        Qiupu qiupu = qiupu();
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        RecordSet recs = qiupu.suggestApks(ctx, viewerId, category, cols, page, count,ua,minSDK);
        ec.record("QiupuServlet.suggestApps end");
        return recs;
    }
    
    @WebMethod("qiupu/hot")
    public RecordSet hotApps(QueryParams qp,HttpServletRequest req) throws ResponseError, Exception {
        String cols = qp.getString("columns", Qiupu.QAPK_FULL_COLUMNS);
        if(StringUtils.isBlank(cols))
        {
        	cols = Qiupu.QAPK_FULL_COLUMNS;
        }
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String category = qp.getString("category", "0");
        Qiupu qiupu = qiupu();
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        return qiupu.getHotOrSelectedApps(ctx, viewerId,Qiupu.MANUAL_HOT, category, cols, page, count,minSDK);
    }
    
    @WebMethod("qiupu/selected")
    public RecordSet selectedApps(QueryParams qp,HttpServletRequest req) throws ResponseError, Exception {
        String cols = qp.getString("columns", Qiupu.QAPK_FULL_COLUMNS);
        if(StringUtils.isBlank(cols))
        {
        	cols = Qiupu.QAPK_FULL_COLUMNS;
        }
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String category = qp.getString("category", "0");
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        Qiupu qiupu = qiupu();
        return qiupu.getHotOrSelectedApps(ctx, viewerId,Qiupu.MANUAL_SELECTED, category, cols, page, count,minSDK);
    }

    @WebMethod("qiupu/sync")
    public RecordSet syncApks(QueryParams qp, HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        String apkIds = qp.checkGetString("apps");
        boolean all = qp.checkGetBoolean("all");

        Qiupu qiupu = qiupu();
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        String deviceid = Constants.parseUserAgent(java.net.URLDecoder.decode(req.getHeader("User-Agent"),"UTF-8"),"device");
        return qiupu.syncApks(ctx, viewerId, apkIds, all,deviceid,minSDK);
    }


    @WebMethod("qiupu/upload")
    public Record uploadApk(QueryParams qp, HttpServletRequest req) throws AvroRemoteException {
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        final Context ctx = WutongContext.getContext(qp, false);
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }

        FileItem fiApk = qp.checkGetFile("apk");
        ApkInfo apkInfo = ApkInfoReader.getApkInfoByAapt(fiApk);
        if (apkInfo == null)
            apkInfo = new ApkInfo();


        if (qp.containsKey("package"))
            apkInfo.setPackage(qp.getString("package", ""));

        
        Configuration conf = getConfiguration();
        if (StringUtils.startsWith(apkInfo.getPackage(), "com.borqs.")) {
//        	boolean enabled = qp.getBoolean("enabled", false);
//            if(enabled)
//            {
            	boolean auth = false;
            	String uploaders = conf.getString("borqs.app.uploader", "*").trim();
        		if (uploaders.equals("*")) {
        			auth = true;
        		}
        		else if (!viewerId.isEmpty()) {
            		 
            			String[] borqsAppUploader = StringUtils2.splitArray(uploaders, ",", true);
            			if (ArrayUtils.contains(borqsAppUploader, viewerId))
            				auth = true;
            		
            	}
            	if (!auth)
            		throw new ServerException(QiupuErrors.APK_INFO_ERROR, "Can't upload borqs app");
//            }
//            else
//            {
//            	String serverHost = conf.getString("server.host", "api.borqs.com");
//            	if(StringUtils.equals(serverHost, "api.borqs.com"))
//            		throw new QiupuException(ErrorCode.APK_INFO_ERROR, "Can't upload borqs app");
//            }
        }

        if (qp.containsKey("app_name"))
            apkInfo.setAppName(qp.getString("app_name", ""));

        if (qp.containsKey("version_code"))
            apkInfo.setVersionCode((int)qp.getInt("version_code", 0));

        if (qp.containsKey("version_name"))
            apkInfo.setVersionName(qp.getString("version_name", ""));

        if (qp.containsKey("min_sdk"))
            apkInfo.setMinSdkVersion((int)qp.getInt("min_sdk", 7)); // TODO: 0 is ok?


        if (qp.containsKey("target_sdk"))
            apkInfo.setTargetSdkVersion((int)qp.getInt("target_sdk", 7));

        if (qp.containsKey("max_sdk"))
            apkInfo.setMaxSdkVersion((int)qp.getInt("max_sdk", 10000));

        if (qp.containsKey("arch")) {
            String s = qp.getString("arch", "");
            apkInfo.setArchitecture(Qiupu.ARCHS.getValue(s));
        }

        if (qp.containsKey("icon")) {
            FileItem fiIcon = qp.checkGetFile("icon");
            apkInfo.setIcon(fiIcon.get());
        }

        String apkIdName = apkInfo.getApkId().toString();
        String apkFile = apkIdName + ".apk";
        String iconFile = apkInfo.hasIcon() ? apkIdName + ".icon.png" : "";


        if (apkStorage.exists(apkFile))
            apkStorage.delete(apkFile);

        SFSUtils.saveUpload(fiApk, apkStorage, apkFile);

        if (apkInfo.hasIcon()) {
            if (apkStorage.exists(iconFile))
                apkStorage.delete(iconFile);

            SFSUtils.saveBytes(apkInfo.getIcon(), apkStorage, iconFile);
        }

        final Qiupu qiupu = qiupu();
        Record rec = new Record();
        rec.put("package", apkInfo.getPackage());
        if (apkInfo.getPackage().equals("com.borqs.qiupu"))
            rec.put("sub_category","276");
        rec.put("app_name", apkInfo.getAppName());
        rec.put("version_code", apkInfo.getVersionCode());
        rec.put("version_name", apkInfo.getVersionName());
        rec.put("min_sdk_version", apkInfo.getMinSdkVersion());
        rec.put("target_sdk_version", apkInfo.getTargetSdkVersion());
        rec.put("max_sdk_version", apkInfo.getMaxSdkVersion());
        rec.put("architecture", apkInfo.getArchitecture());
        rec.put("icon_url", iconFile);
        rec.put("file_url", apkFile);
        rec.put("file_size", apkInfo.getFileSize());
        rec.put("upload_user", NumberUtils.toLong(viewerId, 0));
        rec.put("rating", "3");
        int borqs = borqsApps.contains(apkInfo.getPackage()) ? 1 : 0; 
        rec.put("borqs", borqs);

        try {
            String deviceid = Constants.parseUserAgent(java.net.URLDecoder.decode(req.getHeader("User-Agent"),"UTF-8"),"device");
            qiupu.uploadApk(ctx, viewerId, rec,deviceid);
        } catch (Throwable t) {
            L.error("uploadApk error", t);
        }

        //Record r = qiupu.getAppFull(apkInfo.getApkId().toString()).get(0);


        //get extended infomation from google market
        ThreadPoolManager.getThreadPool().dispatch(
				new AsyncTask(new AppMarketPageReader(conf), new Object[]{rec})
		        {
		            public void invoke(AsyncTaskListener listener,Object[] args)
		            {
		                listener.asyncRead((Record)args[0], new RecordHandler() {

							@Override
							public void handle(Record rec) {
								try {
									qiupu.updateApk(ctx, rec);
								} catch (Exception e) {
									
								}
							}		                	
		                });
		            }
		        });		

        //TODO: generate a stream

        return Record.of(
                "apk_id", apkInfo.getApkId().toString(),
                "file_url", "",
                "file_size", apkInfo.getFileSize());
    }
    
    @WebMethod("qiupu/fill_info")
    public DirectResponse fillApkInfo(QueryParams qp) throws Exception {
        Context ctx = WutongContext.getContext(qp, false);

        int appCount = 0;
        int todayCount = 0;
        int apkNeedInfoCount = 0;
        int appNeedInfoCount = 0;
    	String qapp = ""; 
        String subcategory = "";
        
        boolean isAll = qp.getBoolean("all", false);
        
        Qiupu qiupu = qiupu();
        appCount = qiupu.getApplicationCount(ctx);
        todayCount = qiupu.getTodayAppCount(ctx);
        appNeedInfoCount = qiupu.getNeedExinfoAppCount(ctx);
        RecordSet apks = qiupu.loadNeedExinfoApks(ctx, isAll);
        apkNeedInfoCount = apks.size();
        
        StringBuilder sb = new StringBuilder();
        for(Record apk : apks)
        {
        	String apkName = apk.getString("app_name");
        	String packageName = apk.getString("package");
        	long versionCode = apk.getInt("version_code");        	
        	long arch = apk.getInt("architecture");
        	
        	sb.append("<option value=\"" + packageName + "#" + versionCode + "#" + arch 
        			+ "\">" + apkName + "-" + packageName + "</option>");
        }
        qapp = sb.toString();
        
        sb = new StringBuilder();
        Iterator iter = ApkCategory.appmap.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			Long value = (Long)entry.getValue();
			
			sb.append("<option value=\"" + value + "\">" + key + "</option>");
		}
		iter = ApkCategory.gamemap.entrySet().iterator();
	    while(iter.hasNext())
	    {
		   Map.Entry entry = (Map.Entry)iter.next();
		   String key = (String)entry.getKey();
		   Long value = (Long)entry.getValue();
		   
		   sb.append("<option value=\"" + value + "\">" + key + "</option>");
		}
	    subcategory = sb.toString();
        
    	String html = pageTemplate.merge("fill_apk_info.freemarker", new Object[][]{
    			 {"appCount", appCount},
    			 {"todayCount", todayCount},
    			 {"apkNeedInfoCount", apkNeedInfoCount},
    			 {"appNeedInfoCount", appNeedInfoCount},
    			 {"qapp", qapp},
                 {"application", ApkCategory.APPTYPE_APPLICATION},
                 {"game", ApkCategory.APPTYPE_GAME},
                 {"subcategory", subcategory}
                 });

         return DirectResponse.of("text/html", html);
    }

    @WebMethod("qiupu/borqs_page")
    public DirectResponse borqsAppsPage(QueryParams qp) throws Exception {
        String html = pageTemplate.merge("borqs_app.ftl", new Object[][]{});
        return DirectResponse.of("text/html", html);
    }
    
    @WebMethod("qiupu/borqs")
    public boolean manageBorqsApps(QueryParams qp) throws Exception {
        Qiupu qiupu = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        Configuration conf = getConfiguration();
        String package_ = qp.checkGetString("package");
        int opt = (int) qp.checkGetInt("opt");

        if (StringUtils.isNotBlank(package_)) {
            if (opt == 1) {
                borqsApps.add(package_);
            }
            else if (opt == 0) {
                borqsApps.remove(package_);
            }
            conf.setString("qiupu.borqsApps", StringUtils2.joinIgnoreBlank(",", borqsApps));

            return qiupu.updateBorqsByPackage(ctx, package_, opt);
        }
        else
            return false;
    }

    @WebMethod("qiupu/update")
    public boolean updateApk(QueryParams qp) throws Exception {
        Context ctx = WutongContext.getContext(qp, false);

        Qiupu qiupu = qiupu();
        String packageName = "";
        int versionCode = 0;
        int arch = 1;
        String qapp = "";
        
        String inputPackage = qp.getString("input_pkg", "");
        if (StringUtils.isBlank(inputPackage)) {
            qapp = qp.checkGetString("packagename");
            String[] arr = StringUtils2.splitArray(qapp, "#", true);
            packageName = arr[0];
            versionCode = Integer.parseInt(arr[1]);
            arch = Integer.parseInt(arr[2]);            
        } else {
            packageName = inputPackage;
            versionCode = qiupu.getMaxVersionCode(ctx, inputPackage, 1000);
            arch = 1;
            qapp = inputPackage + "#" + versionCode + "#" + arch;
        }

        String apkId = ApkId.of(packageName, versionCode, arch).toString();
        String apkDesc = qp.getString("apkdesc", "");
        String apkEnDesc = qp.getString("apk_en_desc", "");
        String recentChange = qp.getString("recentChange", "");
        String recentChangeEn = qp.getString("recentChange_en", "");
        String rating = qp.getString("rating", "3");
        long category = qp.getInt("category", ApkCategory.APPTYPE_APPLICATION);
        long subCategory = qp.getInt("subcategory", ApkCategory.CATEGORY_DEFAULT);
        String appName = qp.getString("appname", "");
        String appEnName = qp.getString("app_en_name", "");
        String rawUrl = qp.getString("raw_url", ""); 
        long source = qp.getInt("source", Qiupu.SOURCE_APP_CHINA);
        
        try {
            if (apkDesc.length() < 10) {
                qiupu.createUpdateApkLessDesc(ctx, qapp, versionCode, appName, "");
            }
        } catch (Throwable t) {
        }
        
        
        List<String> screenshots = new ArrayList<String>();
        for(int i = 1; i <= 8; i++)
        {
        	String fieldName = "screenshot" + i;
        	FileItem fi = qp.getFile(fieldName);
        	if((fi != null) && fi.getSize() > 0)
        	{
        		String temp = apkId + "." + fieldName;
                long now = DateUtils.nowMillis();
                String screenshot = temp + ".jpg";
        		if (apkStorage instanceof OssSFS)
                    screenshot = temp + "." + now + ".jpg";

                if (apkStorage.exists(screenshot))
        		{
        			apkStorage.delete(screenshot);
        		}
        		SFSUtils.saveUpload(fi, apkStorage, screenshot);
        		screenshots.add(temp + "." + now + ".jpg");
        	}
        }
        
        Record info = new Record();
        info.put("package", packageName);
        info.put("version_code", versionCode);
        info.put("architecture", arch);
        if(StringUtils.isNotBlank(apkDesc))
        {
        	info.put("description", apkDesc);
        }
        if(StringUtils.isNotBlank(apkEnDesc))
        {
        	info.put("description_en", apkEnDesc);
        }
        if(StringUtils.isNotBlank(recentChange))
        {
            info.put("recent_change", recentChange);
        }
        if(StringUtils.isNotBlank(recentChangeEn))
        {
            info.put("recent_change_en", recentChangeEn);
        }
        if(StringUtils.isNotBlank(rating))
        {
        	info.put("rating", rating);
        }
        info.put("category", category);
        info.put("sub_category", subCategory);
        if(StringUtils.isNotBlank(appName))
        {
        	info.put("app_name", appName);
        }
        if(StringUtils.isNotBlank(appEnName))
        {
        	info.put("app_name_en", appEnName);
        }
        if(screenshots.size() > 0)
        {
        	info.put("screenshots_urls", JsonUtils.toJson(screenshots, false));
        }
        if(StringUtils.isNotBlank(rawUrl))
        {
        	List<String> otherUrls = new ArrayList<String>();
        	otherUrls.add(rawUrl);       	
        	info.put("other_urls", JsonUtils.toJson(otherUrls, false));
        }
        info.put("source", source);
        
        return qiupu.updateApk(ctx, info);
    }

    @WebMethod("search")
    public NoResponse downloadLastApk(QueryParams qp, HttpServletResponse resp,HttpServletRequest req) throws AvroRemoteException, UnsupportedEncodingException {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        String package_ = qp.checkGetString("q");
        String ua = ctx.getUa();
        int minSDK =  getMinSDKFromUA(ua);
        int versionCode = q.getMaxVersionCode(ctx, package_,minSDK);
//        RecordSet rs = q.getApps("", package_ + "-" + versionCode + "-arm", "apk_id", false);
//        if (rs.size() <= 0) {
//            throw Errors.createResponseError(ErrorCode.PLATFORM_GENERAL_ERROR, "Not found app %s", package_);
//        }
//        if (versionCode == 0) {
//            throw Errors.createResponseError(ErrorCode.PLATFORM_GENERAL_ERROR, "Not found app %s", package_);
//        } else {
//            String apkFile = ApkId.of(package_, versionCode, Qiupu.ARCH_ARM).toString() + ".apk";
//            SFSUtils.writeResponse(resp, apkStorage, apkFile);
//        }
        String apkFile = ApkId.of(package_, versionCode, Qiupu.ARCH_ARM).toString() + ".apk";
        try
        {
        	SFSUtils.writeResponse(resp, apkStorage, apkFile);
        }
        catch(Exception e)
        {
        	resp.setStatus(404);        	
        }
        
        return NoResponse.get();
    }
    
    @WebMethod("qiupu/app/manual/create")
    public boolean createManualApks(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.manualApks(ctx, qp.checkGetString("apkIds"), qp.checkGetString("type"));
    }
    
    @WebMethod("qiupu/app/prefectur/get")
    public RecordSet getPrefecturApks(QueryParams qp,HttpServletRequest req) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        String ticket = qp.getString("ticket", null);
        String viewerId = "";
        Context ctx = WutongContext.getContext(qp, false);
        if (ticket != null && !ticket.equals("")) {
            viewerId = ctx.getViewerIdString();
        }
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        String ua = getDecodeHeader(req, "User-Agent", "");
        int minSDK =  getMinSDKFromUA(ua);
        return q.getPrefecturApps(ctx, viewerId,qp.checkGetString("type"),qp.getString("cols", ""),page,count,qp.getBoolean("history_version", false),ua,minSDK);
    } 
    
    @WebMethod("qiupu/app/policy/set")
    public boolean qiupuAppPolicySet(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.saveQapkSuggest(ctx, qp.checkGetString("type"),
                qp.checkGetString("sub_name"),
                (int)qp.getInt("manual_count",10),
                (int)qp.getInt("week_download_count",0),
                (int)qp.getInt("month_download_count",0),
                (int)qp.getInt("year_download_count",20),
                (int)qp.getInt("sum_download_count",40),
                (int)qp.getInt("rating_count",30),
                (int)qp.getInt("borqs_count",5),
                (int)qp.getInt("random_count",5),
                qp.getString("hdpi_img_url", "sub_img_hdpi_none.gif"),
                qp.getString("mdpi_img_url", "sub_img_mdpi_none.gif"));
    }
    
    @WebMethod("qiupu/app/policy/delete")
    public boolean qiupuAppPolicyDelete(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.deleteQapkSuggest(ctx, qp.checkGetString("type"));
    }
    
    @WebMethod("qiupu/app/policy/get")
    public RecordSet qiupuAppPolicyGet(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.getAllQapkSuggestType(ctx, (int)qp.getInt("type",0),qp.getBoolean("ifsuggest", false));
    } 
    
    @WebMethod("qiupu/app/policy/suggest")
    public boolean qiupuAppPolicySuggest(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.updateQapkIfSuggest(ctx, qp.checkGetString("type"));
    }
    
    @WebMethod("qiupu/strongmen/get")
    public RecordSet getStrongMan(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        int page = (int) qp.getInt("page", 0);
        int count = (int) qp.getInt("count", 20);
        return q.getStrongMen(ctx, qp.getString("sub_category", ""), page, count);
    }
    
    @WebMethod("qiupu/wantdesc")
    public RecordSet getUpdateApkLessDesc(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.getUpdateApkLessDesc(ctx, (int)qp.getInt("page", 0),(int)qp.getInt("count", 1000),qp.getString("package", ""),qp.getString("app_name", ""));
    }

    @WebMethod("qiupu/updatecategory")
    public boolean updateApkCategory(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        return q.updateApkCategory(ctx);
    }

    @WebMethod("qiupu/formatdata")
    public boolean qiupuFormatdata(QueryParams qp) throws AvroRemoteException, Exception {
        Qiupu q = qiupu();
        Context ctx = WutongContext.getContext(qp, false);
        q.qiupuFormatData(ctx, "10015");
        return true;
    }
}
