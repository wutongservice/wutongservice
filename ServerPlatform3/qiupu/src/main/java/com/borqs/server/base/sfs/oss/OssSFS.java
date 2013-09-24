package com.borqs.server.base.sfs.oss;

import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.OSSObject;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sfs.AbstractSFS;

import java.io.InputStream;
import java.io.OutputStream;

public class OssSFS extends AbstractSFS {
    private final String accessId = "42azpskan63xg69r03qx7x9x";
    private final String accessKey = "Jhas2oPaqmsHytGy/bNMzRPHHGo=";
    private final String ossEndpoint = "http://storage.aliyun.com/";

    private OSSClient client;
    private String bucketName;

    public OssSFS(Configuration conf) {
        this(conf.checkGetString("bucket"));
    }
    
    public OssSFS(String bucketName) {
        this.bucketName = bucketName;
        client = new OSSClient(ossEndpoint, accessId, accessKey);
    }

    public OSSClient getOSSClient() {
        if (client == null)
            client = new OSSClient(ossEndpoint, accessId, accessKey);

        return client;
    }

    public String getBucketName() {
        return bucketName;
    }
    
    @Override
    public OutputStream create(String file) {
        return null;
    }

    @Override
    public InputStream read(String file) {
        try {
            OSSObject ossObject = client.getObject(bucketName, file);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean delete(String file) {
        return false;
    }

    @Override
    public boolean exists(String file) {
        return false;
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }
}
