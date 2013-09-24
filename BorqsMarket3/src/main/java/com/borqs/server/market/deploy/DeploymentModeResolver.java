package com.borqs.server.market.deploy;


public class DeploymentModeResolver {
    public static String getDeploymentMode() {
        final String envKey = "BorqsMarketDeploymentMode";
        String deploymentMode = System.getenv(envKey);
        if (deploymentMode == null)
            deploymentMode = System.getProperty(envKey);

        if (deploymentMode == null)
            deploymentMode = "release";

        return deploymentMode;
    }

    public static String getConfigPropertiesPath() {
        String deploymentMode = DeploymentModeResolver.getDeploymentMode();
        return String.format("/WEB-INF/properties/%s.properties", deploymentMode);
    }
}
