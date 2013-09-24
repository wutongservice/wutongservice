package com.borqs.server.platform.web.topaz;


import javax.servlet.http.HttpServletRequest;

public class ApiRequest extends Request {
    public ApiRequest(HttpServletRequest httpRequest) {
        super(httpRequest);
    }
}
