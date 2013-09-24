package com.borqs.server.wutong.conf;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.web.QueryParams;
import com.borqs.server.base.web.template.PageTemplate;
import com.borqs.server.base.web.webmethod.WebMethod;
import com.borqs.server.base.web.webmethod.WebMethodServlet;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.commons.Commons;
import com.borqs.server.wutong.commons.WutongContext;
import com.borqs.server.wutong.folder.FolderLogic;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ConfServlet extends WebMethodServlet {
    private static final PageTemplate pageTemplate = new PageTemplate(ConfServlet.class);
    private static String serverHost;

    @Override
    public void init() throws ServletException {
        super.init();
        Configuration conf = getConfiguration();
        serverHost = conf.getString("server.host", "api.borqs.com");
    }

    @WebMethod("v2/configuration/upload")
    public boolean saveConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        ConfLogic conf = GlobalLogics.getConf();
        FolderLogic folderLogic = GlobalLogics.getFile();

        FileItem fi = qp.getFile("file");
        Record configration = new Record();
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        String value = qp.getString("value", "");
        int content_type = 0;
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {

            String folder_id = folderLogic.getFolder(ctx,viewerId, Constants.FOLDER_TYPE_CONFIGURATION, "Configuration Files");
            String file_id = Long.toString(RandomUtils.generateId());
            Record rec =  Commons.formatFileBucketUrl(ctx,viewerId, Commons.uploadFile(ctx, viewerId, file_id, Long.parseLong(folder_id), fi, "", "", "", null, ""));
            content_type = 1;
            value = rec.getString("file_url");
        }
        configration.put("user_id", viewerId);
        configration.put("config_key", config_key);
        configration.put("version_code", version_code);
        configration.put("value", value);
        configration.put("content_type", content_type);
        boolean b = conf.saveConfiguration(ctx, configration);
        return b;
    }
    public void checkConfigurationId(QueryParams qp){
        String viewerId = qp.checkGetString("id");
        Configuration conf = GlobalConfig.get();
        String configId = conf.getString("configuration.internal.id", "");
        if (configId.indexOf(viewerId) == -1)
            throw new ServerException(WutongErrors.AUTH_APP_VERIFY_ERROR, "id is error!");
    }

    @WebMethod("v2/configuration/upload/internal")
    public boolean saveNoTicketConfiguration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        FolderLogic folderLogic = GlobalLogics.getFile();
        ConfLogic conf = GlobalLogics.getConf();

        String viewerId = qp.checkGetString("id");
        checkConfigurationId(qp);
        FileItem fi = qp.checkGetFile("file");
        Record configration = new Record();
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        String value = qp.getString("value", "");
        int content_type = 0;
        if (fi != null && StringUtils.isNotEmpty(fi.getName())) {
            String folder_id = folderLogic.getFolder(ctx,viewerId, Constants.FOLDER_TYPE_CONFIGURATION, "Configuration Files");
            String file_id = Long.toString(RandomUtils.generateId());
            Record rec = Commons.formatFileBucketUrl(ctx,viewerId, Commons.uploadFile(ctx, viewerId, file_id, Long.parseLong(folder_id), fi, "", "", "", null, ""));
            content_type = 1;
            value = rec.getString("file_url");
        }
        configration.put("user_id", viewerId);
        configration.put("config_key", config_key);
        configration.put("version_code", version_code);
        configration.put("value", value);
        configration.put("content_type", content_type);
        boolean b = conf.saveConfiguration(ctx, configration);
        return b;
    }

    @WebMethod("v2/configuration/get")
    public Record getConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        ConfLogic conf = GlobalLogics.getConf();

        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        RecordSet recs = conf.getConfiguration(ctx, viewerId, config_key, version_code);
        return recs.getFirstRecord();
    }



    @WebMethod("v2/configuration/get/internal")
    public Record getNoTicketConfiguration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, false);
        ConfLogic conf = GlobalLogics.getConf();

        checkConfigurationId(qp);
        String viewerId = qp.checkGetString("id");
        String config_key = qp.checkGetString("config_key");
        int version_code = (int) qp.getInt("version_code", 0);
        RecordSet recs =conf.getConfiguration(ctx,viewerId, config_key, version_code);
        return recs.getFirstRecord();
    }

    @WebMethod("v2/configuration/all")
    public RecordSet getUserConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        ConfLogic conf = GlobalLogics.getConf();

        RecordSet recs = conf.getUserConfiguration(ctx,viewerId);
        return recs;
    }

    @WebMethod("v2/configuration/delete")
    public boolean deleteConfigration(QueryParams qp, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Context ctx = WutongContext.getContext(qp, true);
        String viewerId = ctx.getViewerIdString();
        ConfLogic conf = GlobalLogics.getConf();

        String config_key = qp.getString("config_key", "");
        int version_code = (int) qp.getInt("version_code", -1);
        boolean b = conf.deleteConfiguration(ctx,viewerId, config_key, version_code);
        return b;
    }
}
