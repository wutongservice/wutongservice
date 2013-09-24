package com.borqs.server.platform.web.template;


import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.template.FreeMarker;
import org.apache.commons.lang.ObjectUtils;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class FreeMarkerLayout extends FreeMarker {

    public static final String TITLE = "_title_";
    public static final String CSS = "_css_";
    public static final String JS = "_js_";
    public static final String CONTENT = "_content_";


    public FreeMarkerLayout(File file) {
        super(file);
    }

    public FreeMarkerLayout(Class clazz) {
        super(clazz);
    }

    public FreeMarkerLayout(String... dirs) {
        super(dirs);
    }

    @SuppressWarnings("unchecked")
    public void mergeLayout(Writer out, String layoutName, Segment... segs) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        for (Segment seg : segs) {
            if (seg != null) {
                if (seg.name != null) {
                    if (seg.templateName == null) {
                        m.put(seg.name, seg.value);
                    } else {
                        if (!(seg.value instanceof Map))
                            throw new IllegalArgumentException("Value for merge is not a map");

                        m.put(seg.name, merge(seg.templateName, (Map<String, Object>) seg.value));
                    }
                } else {
                    if (!(seg.value instanceof Map))
                        throw new IllegalArgumentException("Value for merge is not a map");

                    m.putAll((Map<String, Object>) seg.value);
                }

            }
        }
        merge(out, layoutName, m);
    }

    public String mergeLayout(String layoutName, Segment... segs) {
        StringWriter out = new StringWriter();
        mergeLayout(out, layoutName, segs);
        return out.toString();
    }

    public static Segment segment(String name, Object value) {
        return new Segment(name, null, value);
    }

    public static Segment segment(String name, String templateName, Object value) {
        return new Segment(name, templateName, value);
    }

    public static Segment params(Map<String, Object> params) {
        return new Segment(null, null, params);
    }

    public static Segment params(Object[][] params) {
        return params(CollectionsHelper.arraysToMap(params));
    }

    public static Segment title(String title) {
        return segment(TITLE, null, ObjectUtils.toString(title));
    }

    public static Segment js(String js) {
        return segment(JS, null, ObjectUtils.toString(js));
    }

    public static Segment css(String css) {
        return segment(CSS, null, ObjectUtils.toString(css));
    }

    public static Segment content(String content) {
        return segment(CSS, null, ObjectUtils.toString(content));
    }

    public static Segment content(String templateName, Map<String, Object> params) {
        return segment(CONTENT, templateName, params);
    }

    public static Segment content(String templateName, Object[][] params) {
        return segment(CONTENT, templateName, CollectionsHelper.arraysToMap(params));
    }

    public static class Segment {
        public final String name;
        public final String templateName;
        public final Object value;

        private Segment(String name, String templateName, Object value) {
            this.name = name;
            this.templateName = templateName;
            this.value = value;
        }
    }
}
