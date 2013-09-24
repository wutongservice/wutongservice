package com.borqs.server.platform.web.topaz;


import java.io.IOException;

public interface TopazOutput {
    void topazOutput(Response resp, Response.OutputOptions opts) throws IOException;
}
