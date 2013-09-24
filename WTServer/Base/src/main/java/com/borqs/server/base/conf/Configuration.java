package com.borqs.server.base.conf;

import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.util.StringMap;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Configuration extends StringMap {

    public Configuration expandMacros() {
        Pattern p = Pattern.compile("\\$\\{(\\w+(\\.\\w+)*)\\}");
        HashMap<String, Object> nm = new HashMap<String, Object>(this);
        for (Map.Entry<String, Object> e : nm.entrySet()) {
            String k = e.getKey();
            String v = ObjectUtils.toString(e.getValue(), "");
            Matcher m = p.matcher(v);
            if (!m.find())
                continue;

            String macro = m.group(1);
            if (containsKey(macro))
                put(k, get(macro));
        }
        return this;
    }

    public void loadIn(String path) {
        loadIn(path, false);
    }

    public void loadIn(String path, boolean append) {
        String s = TextLoader.load(path);
        try {
            Properties props = new Properties();
            props.load(new StringReader(s));
            if (!append)
                clear();

            for (Map.Entry<Object, Object> e : props.entrySet()) {
                put(e.getKey().toString(), e.getValue().toString());
            }
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IO_ERROR, e);
        }
    }

    public static Configuration loadFiles(String... paths) {
        Configuration conf = new Configuration();
        for (String path : paths) {
            conf.loadIn(path, true);
        }
        return conf;
    }

    public static Configuration loadArgs(String... args) {
        ArrayList<String> l = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (StringUtils.equals(args[i], "-c")) {
                l.add("-c" + args[i + 1]);
                i++;
            } else {
                l.add(args[i]);
            }
        }


        Configuration conf = new Configuration();
        for (String s : l) {
            if (s.startsWith("-c"))
                conf.loadIn(StringUtils.removeStart(s, "-c"), true);
        }

        for (String s : l) {
            if (s.startsWith("-c"))
                continue;

            String k = StringUtils.substringBefore(s, "=");
            String v = StringUtils.substringAfter(s, "=");
            conf.put(k, v);
        }
        return conf;
    }

    public Map<String, String> toStrStr() {
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Object> e : entrySet())
            m.put(e.getKey(), ObjectUtils.toString(e.getValue(), ""));
        return m;
    }

    public void loadJsonIn(JsonNode jn, boolean append) {
        Validate.notNull(jn);
        Validate.isTrue(jn.isObject());
        Iterator<String> fields = jn.getFieldNames();
        if (!append)
            clear();

        while (fields.hasNext()) {
            String fieldName = fields.next();
            JsonNode vjn = jn.path(fieldName);
            if (vjn.isBoolean()) {
                put(fieldName, vjn.getBooleanValue());
            } else if (vjn.isIntegralNumber()) {
                put(fieldName, vjn.getLongValue());
            } else if (vjn.isFloatingPointNumber()) {
                put(fieldName, vjn.getDoubleValue());
            } else if (vjn.isTextual()) {
                put(fieldName, vjn.getTextValue());
            } else {
                throw new IllegalArgumentException("Load configuration from json error");
            }
        }
    }

    public static Configuration loadJson(JsonNode jn) {
        Configuration conf = new Configuration();
        conf.loadJsonIn(jn, true);
        return conf;
    }

    public static Configuration loadJson(String json) {
        return loadJson(JsonUtils.parse(json));
    }
}
