package com.borqs.server.platform.web.topaz;


public interface AccessHook {
    void beforeAll(Request req, Response resp);
    void exceptionCaught(Request req, Response resp, Throwable t);
    void success(Request req, Response resp);
    void beforeOutput(Request req, Response resp);
    void afterOutput(Request req, Response resp);
    void afterAll(Request req, Response resp);
}
