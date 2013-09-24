package com.borqs.server.market.utils;


import org.codehaus.jackson.JsonGenerator;

import java.io.IOException;

public interface JsonGeneratorHandler {
    void generate(JsonGenerator jgen) throws IOException;
}
