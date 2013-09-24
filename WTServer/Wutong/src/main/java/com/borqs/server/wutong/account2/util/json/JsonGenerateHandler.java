package com.borqs.server.wutong.account2.util.json;


import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public interface JsonGenerateHandler {
    void generate(JsonGenerator jg, Object arg) throws IOException;
}
