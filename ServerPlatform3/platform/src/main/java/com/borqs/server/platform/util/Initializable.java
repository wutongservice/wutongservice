package com.borqs.server.platform.util;


public interface Initializable {
    void init() throws Exception;

    void destroy();
}
