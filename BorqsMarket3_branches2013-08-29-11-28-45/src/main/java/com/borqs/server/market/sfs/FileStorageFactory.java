package com.borqs.server.market.sfs;


import com.borqs.server.market.deploy.DeploymentModeResolver;

public class FileStorageFactory {
    public static FileStorage create(String localRoot, String ossAccessId, String ossAccessKey, String ossEndpoint, String ossBucket, String ossFileIdPrefix) {
        String deployMode = DeploymentModeResolver.getDeploymentMode();
        if ("release".equalsIgnoreCase(deployMode)) {
            return new AliyunOssStorage(ossAccessId, ossAccessKey, ossEndpoint, ossBucket, ossFileIdPrefix);
        } else {
            return new LocalStorage(localRoot);
        }
    }
}
