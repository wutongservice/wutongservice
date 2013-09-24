package com.borqs.server;


import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.json.JsonCompare;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.base.web.webmethod.WebMethodClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.codehaus.jackson.JsonNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class DiffTest {
    private static Options makeOpts() {
        Options opts = new Options();
        opts.addOption("s1", true, "Server1 Address");
        opts.addOption("s2", true, "Server2 Address");
        opts.addOption("ticket", true, "Ticket");
        opts.addOption("app", true, "App ID");
        opts.addOption("secret", true, "App Secret");
        opts.addOption("h", "help", false, "Print this message");
        return opts;
    }

    private static void printUsage(Options opts) {
        new HelpFormatter().printHelp(DiffTest.class.getName(), opts);
    }

    public static void main(String[] args) throws Exception {
        Options opts = makeOpts();
        CommandLine cl = new GnuParser().parse(opts, args);
        if (cl.hasOption("h")) {
            printUsage(opts);
            return;
        }

        String server1 = cl.getOptionValue("s1", null);
        String server2 = cl.getOptionValue("s2", null);
        if (server1 == null || server2 == null) {
            printUsage(opts);
            return;
        }

        String ticket = cl.getOptionValue("ticket", null);
        String appId = cl.getOptionValue("app", null);
        String appSecret = cl.getOptionValue("secret", null);

        // load tests
        String[] scriptPaths = cl.getArgs();
        ArrayList<Test> tests = new ArrayList<Test>();
        for (String scriptPath : scriptPaths) {
            loadTests(scriptPath, tests);
        }

        // String apiUri, String ticket, String appId, String appSecret
        WebMethodClient c1 = WebMethodClient.create(server1, ticket, appId, appSecret);
        WebMethodClient c2 = WebMethodClient.create(server2, ticket, appId, appSecret);

        for (Test test : tests) {
            try {
                runTest(test, c1, c2);
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    private static void printResponse(int code, String content) {
        System.out.println("CODE   :" + code);
        System.out.println("CONTENT:");
        System.out.println(content);
    }

    private static void runTest(Test test, WebMethodClient c1, WebMethodClient c2) throws Exception {
        System.out.println(String.format("===== TEST %s@%s =====", test.api, FilenameUtils.getName(test.scriptPath)));
        if (StringUtils.isNotBlank(test.comment))
            System.out.println("COMMENT: " + test.comment);

        HttpResponse resp1 = c1.get(test.api, test.params);
        HttpResponse resp2 = c2.get(test.api, test.params);

        int code1 = WebMethodClient.getResponseCode(resp1);
        int code2 = WebMethodClient.getResponseCode(resp2);
        String content1 = WebMethodClient.getResponseText(resp1, null);
        String content2 = WebMethodClient.getResponseText(resp2, null);

        System.out.println(">>> Response1:");
        printResponse(code1, content1);
        System.out.println(">>> Response2:");
        printResponse(code2, content2);
        System.out.println("------------");


        if (code1 != code2) {
            System.out.println(String.format("[ER] Status code: %d - %d", code1, code2));
            return;
        }

        JsonNode jn1 = null;
        JsonNode jn2 = null;
        try {
            jn1 = JsonUtils.parse(content1);
            jn2 = JsonUtils.parse(content2);
        } catch (Exception ignored) {
        }
        if (jn1 == null || jn2 == null) {
            if (!StringUtils.equals(content1, content2)) {
                System.out.println(String.format("[ER] Different content"));
                return;
            }
        }

        JsonCompare.Result r = JsonCompare.compareType(jn1, jn2);
        if (!r.isEquals()) {
            System.out.println("[ER] Different PLATFORM_JSON_ERROR type");
            return;
        }
        if (jn1.isObject()) {
            r = JsonCompare.compareObject(jn1, jn2, test.excludedFields);
            if (!r.isEquals()) {
                System.out.println(r.toString());
                return;
            }
        }
        if (jn1.isArray()) {
            boolean isObjectArray = true;
            for (int i = 0; i < jn1.size(); i++) {
                if (!jn1.path(i).isObject()) {
                    isObjectArray = false;
                    break;
                }
            }
            for (int i = 0; i < jn2.size(); i++) {
                if (!jn1.path(i).isObject()) {
                    isObjectArray = false;
                    break;
                }
            }
            if (isObjectArray) {
                r = JsonCompare.compareObjectArray(jn1, jn2, test.excludedFields);
            } else {
                r = JsonCompare.compareArray(jn1, jn2);
            }
            if (!r.isEquals()) {
                System.out.println(r.toString());
                return;
            }
        }

        System.out.println("[OK]");
    }

    private static class Test {
        String scriptPath;
        String comment;
        String api;
        Map<String, Object> params;
        String[] excludedFields;
    }

    private static void loadTests(String scriptPath, List<Test> result) throws Exception {
        String xml = TextLoader.load(scriptPath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        NodeList testNodes = root.getChildNodes();
        for (int i = 0; i < testNodes.getLength(); i++) {
            Node childNode = testNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE && "test".equalsIgnoreCase(((Element) childNode).getTagName())) {
                readTest((Element) childNode, scriptPath, result);
            }
        }

    }

    private static void readTest(Element node, String scriptPath, List<Test> result) throws IOException {
        Test test = new Test();
        test.scriptPath = scriptPath;
        test.api = readChildNodeText(node, "api", "");
        test.comment = readChildNodeText(node, "text", "");
        test.params = parseParams(readChildNodeText(node, "params", ""));
        test.excludedFields = parseExcludedFields(readChildNodeText(node, "excludedFields", ""));
        result.add(test);
    }

    private static String readChildNodeText(Element node, String childTag, String def) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) childNode;
                if (StringUtils.equals(childElem.getTagName(), childTag)) {
                    return childElem.getTextContent();
                }
            }
        }
        return def;
    }

    private static Map<String, Object> parseParams(String s) throws IOException {
        LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();

        Properties props = new Properties();
        props.load(new StringReader(s));
        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            m.put(key, val);
        }

        return m;
    }

    private static String[] parseExcludedFields(String s) {
        return StringUtils.isBlank(s) ? null : StringUtils2.splitArray(s, ",", true);
    }

}
