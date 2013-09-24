package com.borqs.server.platform.util.json;


import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public interface JsonGenerateHandler {
    void generate(JsonGenerator jg, Object arg) throws IOException;
}
