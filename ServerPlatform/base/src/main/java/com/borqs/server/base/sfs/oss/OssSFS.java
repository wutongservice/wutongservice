package com.borqs.server.base.sfs.oss;

import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.OSSObject;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.sfs.AbstractSFS;
import org.apache.commons.lang.Validate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

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
    public InputStream read(String file, HttpServletResponse resp) {
        try {
            if (resp != null) {
                ObjectMetadata metaData = client.getObjectMetadata(bucketName, file);
                resp.setContentLength((int) metaData.getContentLength());
                resp.addHeader("Etag", metaData.getETag());
            }
            
            return read(file);
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
