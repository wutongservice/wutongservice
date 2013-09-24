package com.borqs.server.base.tools;


import com.borqs.server.base.util.Encoders;
import com.borqs.server.base.util.ObjectHolder;
import com.borqs.server.base.web.webmethod.WebMethodClient;
import org.apache.commons.lang.ObjectUtils;
import org.apache.http.HttpResponse;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.groovy.tools.shell.AnsiDetector;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.codehaus.groovy.tools.shell.util.NoExitSecurityManager;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebTesterCli {
    static {
        AnsiConsole.systemInstall();
        Ansi.setDetector(new AnsiDetector());
    }

    public static void main(String[] args) {
        final ObjectHolder<Integer> code = new ObjectHolder<Integer>(null);
        final IO io = new IO();
        io.setVerbosity(IO.Verbosity.QUIET);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (code.value == null) {
                        io.err.println();
                        io.err.println("@|red WARNING:|@ Abnormal JVM shutdown detected");
                    }
                    io.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        SecurityManager psm = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager());

        Groovysh shell = new Groovysh(io);
        try {
            Functions.init();
            shell.execute(String.format("import static %s.Functions.*", WebTesterCli.class.getName()));
            code.value = shell.run();
        } finally {
            System.setSecurityManager(psm);
        }
        System.exit(code.value);
    }

    public static class Functions {
        private static final TesterSetting setting = new TesterSetting();

        static void init() {
            setting.load();
        }

        private static void printText(String s) {
            if (setting.print)
                System.out.println(s);
        }

        private static String getConfigPath() {
            return System.getProperty("user.home") + "/.web.properties";
        }

        public static void setting(Map<String, Object> m) {
            if (m.containsKey("uri"))
                setting.uri = ObjectUtils.toString(m.get("uri"), null);
            if (m.containsKey("ticket"))
                setting.ticket = ObjectUtils.toString(m.get("ticket"), null);
            if (m.containsKey("appid"))
                setting.appid = ObjectUtils.toString(m.get("appid"), null);
            if (m.containsKey("secret"))
                setting.secret = ObjectUtils.toString(m.get("secret"), null);
            if (m.containsKey("ua"))
                setting.ua = ObjectUtils.toString(m.get("ua"), null);
            if (m.containsKey("print"))
                setting.print = (Boolean)m.get("print");

            setting.save();
        }

        public static Map<String, Object> setting() {
            LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("uri", setting.uri);
            m.put("ticket", setting.ticket);
            m.put("appid", setting.appid);
            m.put("secret", setting.secret);
            m.put("ua", setting.ua);
            m.put("print", setting.print);
            printText("uri = " + setting.uri);
            printText("ticket = " + setting.ticket);
            printText("appid = " + setting.appid);
            printText("secret = " + setting.secret);
            printText("ua = " + setting.ua);
            printText("print = " + setting.print);

            return m;
        }

        public static String get(String method) {
            return get(new HashMap<String, Object>(), method);
        }

        public static String get(Map<String, Object> params, String method) {
            WebMethodClient wmc = createClient();
            HttpResponse resp = wmc.get(method, params);
            printText(">>>>> GET " + wmc.makeGetUrl(method, params));
            return printResponse(resp);
        }

        public static String post(String method) {
            return post(new HashMap<String, Object>(), method);
        }

        public static String post(Map<String, Object> params, String method) {
            boolean multipart = false;
            for (Object v : params.values()) {
                if (v instanceof ContentBody || v instanceof File) {
                    multipart = true;
                    break;
                }
            }
            WebMethodClient wmc = createClient();
            printText(">>>>> POST " + wmc.makeInvokeUrl(method));
            HttpResponse resp;
            if (multipart) {
                resp = wmc.multipartPost(method, params);
            } else {
                resp = wmc.formPost(method, params);
            }

            return printResponse(resp);
        }

        public static File file(String s) {
            return new File(s);
        }

        public static String base64(String s) {
            return Encoders.toBase64(s);
        }

        public static String md5hex(String s) {
            return Encoders.md5Hex(s);
        }

        public static String md5base64(String s) {
            return Encoders.md5Base64(s);
        }

        private static String printResponse(HttpResponse resp) {
            int code = WebMethodClient.getResponseCode(resp);
            String text = WebMethodClient.getResponseText(resp, null);
            printText("<<<<< STATUS CODE: " + code);
            printText(text);
            return text;
        }

        private static WebMethodClient createClient() {
            return WebMethodClient.create(setting.uri, setting.ticket, setting.appid, setting.secret);
        }
    }
}
