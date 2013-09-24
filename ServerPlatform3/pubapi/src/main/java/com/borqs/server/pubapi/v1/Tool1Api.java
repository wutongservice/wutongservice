package com.borqs.server.pubapi.v1;

import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.web.doc.IgnoreDocument;
import com.borqs.server.platform.web.topaz.RawText;
import com.borqs.server.platform.web.topaz.Request;
import com.borqs.server.platform.web.topaz.Response;
import com.borqs.server.platform.web.topaz.Route;
import com.borqs.server.pubapi.PublicApiSupport;

@IgnoreDocument
public class Tool1Api extends PublicApiSupport {
    public Tool1Api() {
    }

    @Route(url = "/ping")
    public void ping(Request req, Response resp) {
        resp.type("text/plain");
        String s = String.format("HI\nIP : %s\nNow : %s", req.httpRequest.getRemoteAddr(), DateHelper.formatDateAndTime(DateHelper.now()));
        resp.body(RawText.of(s));
    }
}
