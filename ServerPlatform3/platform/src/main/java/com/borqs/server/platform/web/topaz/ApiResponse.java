package com.borqs.server.platform.web.topaz;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

public class ApiResponse extends Response {

    public ApiResponse(HttpServletResponse httpResponse) {
        super(httpResponse);
    }

    @Override
    protected void outputGenericObject(final Object o, OutputOptions opts) throws IOException {
        final String RESULT_FIELD = "result";
        Object n;
        if (o == null
                || o instanceof Boolean
                || o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte
                || o instanceof Double || o instanceof Float) {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put(RESULT_FIELD, o);
            n = m;
        } else if (o instanceof Character || o instanceof CharSequence) {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put(RESULT_FIELD, o.toString());
            n = m;
        } else if (o instanceof JsonNode) {
            JsonNode jn = (JsonNode)o;
            if (jn.isNull() || jn.isNumber() || jn.isBoolean() || jn.isTextual() || jn.isBinary()) {
                HashMap<String, Object> m = new HashMap<String, Object>();
                m.put(RESULT_FIELD, jn);
                n = m;
            } else {
                n = o;
            }
        } else {
            n = o;
        }
        directWrite(JsonHelper.toJson(n, true), opts);
    }

    @Override
    protected void outputError(Throwable t, OutputOptions opts) throws IOException {
        directWrite(errToStr(t, errorDetail), opts);
    }

    private static String errToStr(final Throwable t, final boolean errorDetail) {
        if (t instanceof HaltException) {
            return ObjectUtils.toString(((HaltException) t).body, "");
        } else if (t instanceof ServerException) {
            final ServerException e = (ServerException)t;
            return JsonHelper.toJson(new JsonGenerateHandler() {
                @Override
                public void generate(JsonGenerator jg, Object arg) throws IOException {
                    jg.writeStartObject();
                    jg.writeNumberField("error_code", e.getCode());
                    jg.writeStringField("error_msg", ObjectUtils.toString(e.getMessage()));
                    if (errorDetail) {
                        StringWriter buff = new StringWriter();
                        PrintWriter w = new PrintWriter(buff);
                        w.println(t.getMessage());
                        t.printStackTrace(w);
                        w.flush();
                        jg.writeStringField("error_stack", w.toString());
                    }
                    jg.writeEndObject();
                }
            }, true);
        } else {
            return errToStr(new ServerException(E.UNKNOWN, t.getMessage()), errorDetail);
        }
    }
}
