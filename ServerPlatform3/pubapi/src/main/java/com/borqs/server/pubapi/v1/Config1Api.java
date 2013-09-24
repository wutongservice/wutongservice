package com.borqs.server.pubapi.v1;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.feature.account.AccountHelper;
import com.borqs.server.platform.feature.account.AccountLogic;
import com.borqs.server.platform.feature.configuration.Config;
import com.borqs.server.platform.feature.configuration.ConfigLogic;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreDocument
public class Config1Api extends PublicApiSupport {
    private ConfigLogic config;
    private AccountLogic account;
    private SFS sfsConfigFile;

    public static String bucketName = "wutong-data";
    public static String bucketName_video_key = "media/video/";
    public static String bucketName_audio_key = "media/audio/";
    public static String bucketName_static_file_key = "files/";


    public ConfigLogic getConfig() {
        return config;
    }

    public void setAccount(AccountLogic account) {
        this.account = account;
    }

    public void setConfig(ConfigLogic config) {
        this.config = config;
    }

    public void setSfsConfigFile(SFS sfsConfigFile) {
        this.sfsConfigFile = sfsConfigFile;
    }

    public Config1Api() {
    }

    @Route(url = "/configuration/upload")
    public void fileUpload(Request req, Response resp) {
        Context ctx = checkContext(req, true);
        FileItem fileItem = req.checkFile("file");
        String json = uploadFile(req, ctx, fileItem);
        resp.body(RawText.of(json));
    }

    @Route(url = "/configuration/get")
    public void getConfigration(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String key = req.getString("key", "");
        int version_code = req.getInt("version_code", 0);
        List<Config> list = config.getConfig(ctx, ctx.getViewer(), key, version_code);
        Config config;
        if (list.size() > 0)
            config = list.get(0);
        else
            config = new Config();

        resp.body(RawText.of(JsonHelper.toJson(config, false)));
    }

    @Route(url = "/configuration/all")
    public void getUserConfigration(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        List<Config> list = config.getConfigsById(ctx, ctx.getViewer());
        RecordSet rs = getVersions(list);
        resp.body(RawText.of(rs.toJson(false, false)));
    }

    @Route(url = "/configuration/delete")
    public void deleteConfigration(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        AccountHelper.checkUser(account, ctx, ctx.getViewer());

        String key = req.getString("key", "");
        int version_code = req.getInt("version_code", -1);
        boolean b = config.deleteConfig(ctx, ctx.getViewer(), key, version_code);
        resp.body(b);
    }


    // group the result the style is ：k1:v1 k1:v2 k2:v1 k2:v2
    private RecordSet getVersions(List<Config> configs) {
        RecordSet rs = new RecordSet();
        if (configs.size() < 1)
            return null;

        Map<String, RecordSet> map = new HashMap<String, RecordSet>();
        for (Config config1 : configs) {
            String key = config1.getConfigKey();
            if (map.containsKey(key)) {
                Record r = new Record();
                r.put("config_key", key);
                r.put("versions", config1);
                map.get(key).add(r);
            } else {
                map.put(key, new RecordSet());
                Record r = new Record();
                r.put("config_key", key);
                r.put("versions", config1);
                map.get(key).add(r);
            }
        }
        for (Map.Entry<String, RecordSet> entry : map.entrySet()) {
            Record record = new Record();
            record.put(entry.getKey(), entry.getValue());
            rs.add(record);
        }
        return rs;
    }

    private String uploadFile(Request req, Context ctx, FileItem fileItem) {

        if (fileItem.getSize() > 50 * 1024 * 1024)
            throw new ServerException(E.INVALID_VERIFICATION_CODE, "file size is too large");

        String content_type = req.getString("content_type", "");
        String fileName ="";

        if (StringUtils.isEmpty(fileName))
            fileName = fileItem.getName().substring(fileItem.getName().lastIndexOf("\\") + 1, fileItem.getName().length());
        String expName = "";
        if (fileName.contains(".")) {
            expName = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        }

        String json = saveConfig(ctx, req, fileItem, fileName, expName, content_type);

        return json;
    }


    private String saveConfig(Context ctx, Request req, FileItem fileItem, String fileName, String expName, String content_type) {
        String newFileName = "";
        String video_id = Long.toString(RandomHelper.generateId());
        if (expName.equals("")) {
            newFileName = ctx.getViewer() + "_" + video_id;
        } else {
            newFileName = ctx.getViewer() + "_" + video_id + "." + expName;
        }
        long time = DateHelper.nowMillis();


        this.config.uploadConfig(ctx, sfsConfigFile, fileItem, newFileName);

        Config config1 = new Config();

        String path = bucketName + "/" + "config/" + config1.getUserId() + "/" + newFileName;
        config1.setConfigKey(req.checkString("config_key"));
        config1.setUserId(ctx.getViewer());
        config1.setVersionCode(req.getInt("version_code", 0));
        String value = req.getString("value", "");
        if (StringUtils.isEmpty(value)) {
            config1.setContentType(1);
            value = path;
        }
        config1.setValue(value);
        this.config.saveConfig(ctx, config1);

        config1.setAddon("file_url", path);

        return config1.toJson(Config.STANDARD_COLUMNS, false);
    }


}
