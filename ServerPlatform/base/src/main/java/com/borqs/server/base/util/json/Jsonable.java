package com.borqs.server.base.util.json;


import org.codehaus.jackson.JsonGenerator;

public interface Jsonable {
    void generateJson(JsonGenerator jg, String[] cols);
}
