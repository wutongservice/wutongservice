package com.borqs.server.pubapi;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.TargetInfo;
import com.borqs.server.platform.feature.TargetInfoFetcher;
import com.borqs.server.platform.feature.TargetInfoFormat;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.platform.web.doc.HttpExamplePackage;
import com.borqs.server.platform.web.doc.RoutePrefix;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.example.PackageClass;

@RoutePrefix("/v2")
@HttpExamplePackage(PackageClass.class)
public class TargetApi extends PublicApiSupport {

    @Route(url = "/show")
    public void showTargetInfo(Request req, Response resp) {
        Context ctx = checkContext(req, false);
        Target[] targets = Target.parseCompatibleStringToArray(req.checkString("targets"), ",");
        TargetInfo[] targetInfoArray = TargetInfoFetcher.getInstance().multiFetchTargetInfo(ctx, targets);
        resp.body(RawText.of(TargetInfo.arrayToJson(targetInfoArray, null, true)));
//        StringBuilder buff = new StringBuilder();
//        buff.append("getRequestURI " + req.httpRequest.getRequestURI() + "\n");
//        buff.append("getRequestURL " + req.httpRequest.getRequestURL() + "\n");
//        buff.append("getContextPath " + req.httpRequest.getContextPath() + "\n");
//        buff.append("getServletPath " + req.httpRequest.getServletPath() + "\n");
//        buff.append("getPathInfo " + req.httpRequest.getPathInfo() + "\n");
//        buff.append("getQueryString " + req.httpRequest.getQueryString() + "\n");
//        buff.append("getPathTranslated" + req.httpRequest.getPathTranslated() + "\n");
//        resp.body(RawText.of(buff.toString()));
    }
}
