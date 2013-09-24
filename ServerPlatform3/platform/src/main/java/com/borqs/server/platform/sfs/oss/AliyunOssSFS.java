package com.borqs.server.platform.sfs.oss;


import com.aliyun.openservices.ClientException;
import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.OSSException;
import com.aliyun.openservices.oss.model.OSSObject;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.AbstractSFS;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.web.WebHelper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AliyunOssSFS extends AbstractSFS implements Initializable {
    private static final Logger L = Logger.get(AliyunOssSFS.class);

    private static final String DEFAULT_ACCESS_ID = "42azpskan63xg69r03qx7x9x";
    private static final String DEFAULT_ACCESS_KEY = "Jhas2oPaqmsHytGy/bNMzRPHHGo=";
    private static final String DEFAULT_OSS_ENDPOINT = "http://storage.aliyun.com/";

    private OSSClient client;

    private String accessId = DEFAULT_ACCESS_ID;
    private String accessKey = DEFAULT_ACCESS_KEY;
    private String ossEndpoint = DEFAULT_OSS_ENDPOINT;
    private String bucketName = "";

    public AliyunOssSFS() {
    }

    public String getAccessId() {
        return accessId;
    }

    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getOssEndpoint() {
        return ossEndpoint;
    }

    public void setOssEndpoint(String ossEndpoint) {
        this.ossEndpoint = ossEndpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public void init() throws Exception {
        if (client != null)
            throw new IllegalStateException();
        client = new OSSClient(ossEndpoint, accessId, accessKey);
    }

    @Override
    public void destroy() {
        client = null;
    }

    @Override
    public boolean exists(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream read(String name) throws IOException {
        try {
            L.debug(null, "AliyunOssSFS: read %s/%s", bucketName, name);
            OSSObject obj = client.getObject(bucketName, name);
            return obj.getObjectContent();
        } catch (Exception e) {
            throw new IOException("OSS read error", e);
        }
    }

    @Override
    public void write(String name, InputStream in) throws IOException {
        final String CONTENT_TYPE = "binary/octet-stream";
        L.debug(null, "AliyunOssSFS: write %s/%s, len=%s, type=%s", bucketName, name, in.available(), CONTENT_TYPE);
        writeHelper(name, in, CONTENT_TYPE);
    }

    private void writeHelper(String name, InputStream in, String contentType) throws IOException {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(in.available());
        meta.setContentType(contentType);
        try {
            client.putObject(bucketName, name, in, meta);
        } catch (Exception e) {
            throw new IOException("OSS write error", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public void writeFile(String name, String localFile) throws IOException {
        FileInputStream in = new FileInputStream(localFile);
        String contentType = WebHelper.getMimeTypeByFileName(FilenameUtils.getName(localFile));
        L.debug(null, "AliyunOssSFS: write %s => %s/%s, len=%s, type=%s", localFile, bucketName, name, in.available(), contentType);
        writeHelper(name, in, contentType);
    }

    @Override
    public void delete(String name) throws IOException {
        try {
            L.debug(null, "AliyunOssSFS: delete %s/%s", bucketName, name);
            client.deleteObject(bucketName, name);
        } catch (Exception e) {
            throw new IOException("OSS delete error", e);
        }
    }
}
