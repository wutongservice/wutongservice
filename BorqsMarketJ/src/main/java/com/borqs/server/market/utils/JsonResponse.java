package com.borqs.server.market.utils;


import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class JsonResponse extends ModelAndView {

    private static final String MODEL_NAME = "data";

    protected JsonResponse(Object data) {
        super();
        setView(new JsonView());
        addObject(MODEL_NAME, data);
    }

    protected Object wrapData(Object data) {
        return data;
    }

    private class JsonView implements View {
        @Override
        public String getContentType() {
            return null;
        }

        private void outputJson(Writer out, Object data) throws IOException {
            if (data instanceof RawJson) {
                out.write(((RawJson) data).json);
            } else {
                JsonUtils.toJson(out, data, false);
            }
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
            Object data = model.get(MODEL_NAME);
            if (!(data instanceof RawJson)) {
                data = wrapData(data);
            }
            PrintWriter out;
            String callback = request.getParameter("callback");
            if (StringUtils.isNotEmpty(callback)) {
                // xhr
                response.setHeader("Content-Type", "text/javascript; charset=UTF-8;");
                out = response.getWriter();
                out.print(callback + "(");
                outputJson(out, data);
                out.print(");");
            } else {
                response.setHeader("Content-Type", "application/json; charset=UTF-8;");
                out = response.getWriter();
                outputJson(out, data);
            }
            out.flush();
        }
    }

    public static JsonResponse of(Object data) {
        return new JsonResponse(data);
    }

    public static JsonResponse raw(String json) {
        return new JsonResponse(new RawJson(json));
    }

    protected static class RawJson {
        final String json;

        public RawJson(String json) {
            this.json = json;
        }
    }
}
