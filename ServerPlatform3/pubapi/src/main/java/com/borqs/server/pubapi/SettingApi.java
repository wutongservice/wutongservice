package com.borqs.server.pubapi;


import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.pubapi.example.PackageClass;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class SettingApi extends PublicApiSupport {
    public SettingApi() {
    }


}
