package com.borqs.server.market.sfs;


import com.aliyun.openservices.ClientException;
import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.OSSException;
import com.aliyun.openservices.oss.model.OSSObject;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.aliyun.openservices.oss.model.PutObjectResult;
import com.borqs.server.market.utils.MimeTypeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class AliyunOssStorage implements FileStorage {

    private String accessId;
    private String accessKey;
    private String endpoint;
    private String bucket;
    private String prefix;
    private OSSClient client;

    public AliyunOssStorage() {
    }

    public AliyunOssStorage(String accessId, String accessKey, String endpoint, String bucket, String prefix) {
        this.accessId = accessId;
        this.accessKey = accessKey;
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.prefix = prefix;
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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void init() {
        client = new OSSClient(endpoint, accessId, accessKey);
    }

    @Override
    public String write(String fileId, FileContent content) throws IOException {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(content.size);
        String contentType = content.contentType != null ? content.contentType : MimeTypeUtils.getMimeTypeByFilename(fileId);
        meta.setContentType(contentType);
        if (!StringUtils.startsWith(contentType, "image/") && !StringUtils.startsWith(contentType, "text/")) {
            meta.setContentDisposition("attachment; filename=\"" + fileId + "\"");
        }
        try {
            client.putObject(bucket, ObjectUtils.toString(prefix) + fileId, content.stream, meta);
            return fileId;
        } catch (Exception e) {
            throw new IOException("Save to OSS error " + fileId, e);
        } finally {
            IOUtils.closeQuietly(content.stream);
        }
    }

    @Override
    public FileContent read(String fileId) throws IOException {
        OSSObject ossObj;
        try {
            ossObj = client.getObject(bucket, ObjectUtils.toString(prefix) + fileId);
        } catch (Exception e) {
            ossObj = null;
        }

        if (ossObj == null)
            throw new IOException("Not found file " + fileId);

        ObjectMetadata meta = ossObj.getObjectMetadata();
        String contentType = meta.getContentType();
        return FileContent.create(ossObj.getObjectContent(), contentType, meta.getContentLength());
    }
}
