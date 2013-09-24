package com.borqs.server.platform.util;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class MacroExpander {
    public static String expand(String s, KeyHandler handler) {
        if (s == null)
            return null;

        StringBuilder buff = new StringBuilder();
        int len = s.length();
        int state = 0;
        StringBuilder keyBuff = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (state) {
                case 0: {
                    if (c == '$')
                        state = 1;
                    else
                        buff.append(c);
                }
                break;

                case 1: {
                    if (c == '$') {
                        buff.append('$');
                        state = 0;
                    } else if (c == '{') {
                        state = 2;
                    } else {
                        throw new ServerException(E.MACRO_EXPAND, "expand macro error '%s'", s);
                    }
                }
                break;

                case 2: {
                    if (c == '}') {
                        String key = keyBuff.toString();
                        String val = handler != null ? handler.get(key) : "";
                        buff.append(val);
                        state = 0;
                        keyBuff.setLength(0);
                    } else {
                        keyBuff.append(c);
                    }
                }
                break;
            }
        }
        if (state != 0)
            throw new ServerException(E.MACRO_EXPAND, "expand macro error '%s'", s);
        return buff.toString();
    }

    public static String expandMacros(String s, final Map<String, ?> macros) {
        return expand(s, new KeyHandler() {
            @Override
            public String get(String key) {
                return MapUtils.isEmpty(macros) ? "" : ObjectUtils.toString(macros.get(key), "");
            }
        });
    }

    private static final KeyHandler SYSTEM_KEY_HANDLER = new KeyHandler() {
        @Override
        public String get(String key) {
            String s = System.getProperty(key);
            if (s == null)
                s = SystemHelper.getHomeDirectory();

            return s != null ? s : "";
        }
    };

    public static String expandSystemMacros(String s) {
        return expand(s, SYSTEM_KEY_HANDLER);
    }

    public static interface KeyHandler {
        String get(String key);
    }
}
