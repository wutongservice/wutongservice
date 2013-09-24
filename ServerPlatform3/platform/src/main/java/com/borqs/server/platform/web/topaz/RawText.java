package com.borqs.server.platform.web.topaz;


import org.apache.commons.lang.ObjectUtils;

import java.io.IOException;

public class RawText implements TopazOutput {
    public final String text;

    public RawText(String text) {
        this.text = text;
    }

    @Override
    public void topazOutput(Response resp, Response.OutputOptions opts) throws IOException {
        resp.directWrite(ObjectUtils.toString(text), opts);
    }

    @Override
    public String toString() {
        return text;
    }

    public static RawText of(String text) {
        return new RawText(text);
    }
}
