package com.borqs.server.platform.web.doc.xml;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.XmlHelper;
import com.borqs.server.platform.web.doc.HttpApiDoclet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class XmlOutput implements HttpApiDoclet.Output {
    public XmlOutput() {
    }

    @Override
    public void output(HttpApiDoclet.ApiDocs docs, String outputDir, String[] args) throws Exception {
        FileUtils.forceMkdir(new File(outputDir));
        FileWriter w = null;
        try {
            w = new FileWriter(FilenameUtils.concat(outputDir, "doc.xml"));
            outputXml(docs, new PrintWriter(w));
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    public static void outputXml(HttpApiDoclet.ApiDocs docs, PrintWriter out) throws ParserConfigurationException, TransformerException, IOException {
        if (CollectionUtils.isEmpty(docs.docs))
            return;

        Document doc = XmlHelper.newDocument();
        Element root = XmlHelper.setRootElement(doc, "document", null);
        setupDocNode(doc, root, docs);
        XmlHelper.writeDocument(doc, out, Charsets.DEFAULT, true);
    }

    private static void setupDocNode(Document doc, Element node, HttpApiDoclet.ApiDocs docs) {
        Element groupsNode = XmlHelper.newChildElement(doc, node, "groups", null);
        for (String group : docs.getGroupNames()) {
            Element groupNode = XmlHelper.newChildElement(doc, groupsNode, "group", CollectionsHelper.of("name", (Object) group));
            for (String route : docs.getGroupRoutes(group))
                XmlHelper.newTextChildElement(doc, groupNode, "route", route, null);
        }

        Element apisNode = XmlHelper.newChildElement(doc, node, "apis", null);
        for (HttpApiDoclet.ApiDoc apiDoc : docs.docs) {
            Element apiNode = XmlHelper.newChildElement(doc, apisNode, "api", null);
            setupApiDocNode(doc, apiNode, apiDoc);
        }
    }

    private static void setupApiDocNode(Document doc, Element node, HttpApiDoclet.ApiDoc apiDoc) {
        XmlHelper.setAttributes(node,
                CollectionsHelper.<String, Object>of(
                        "login", HttpApiDoclet.booleanToString(apiDoc.login),
                        "deprecated", HttpApiDoclet.booleanToString(apiDoc.deprecated),
                        "http-methods", StringUtils.join(apiDoc.httpMethods, "|")));

        XmlHelper.newTextChildElement(doc, node, "group", apiDoc.group, null);

        Element routesNode = XmlHelper.newChildElement(doc, node, "routes", null);
        for (String route : apiDoc.routes)
            XmlHelper.newTextChildElement(doc, routesNode, "route", route, null);

        XmlHelper.newTextChildElement(doc, node, "description", apiDoc.description, null);
        XmlHelper.newTextChildElement(doc, node, "http-return", apiDoc.httpReturn, null);
        XmlHelper.newTextChildElement(doc, node, "remark", apiDoc.remark, null);

        Element httpExamplesNode = XmlHelper.newChildElement(doc, node, "http-examples", null);
        for (String example : apiDoc.httpExamples)
            XmlHelper.newTextChildElement(doc, httpExamplesNode, "http-example", example, null);

        Element httpParamsNode = XmlHelper.newChildElement(doc, node, "http-params", null);
        for (HttpApiDoclet.ParamDoc paramDoc : apiDoc.httpParams) {
            Element httpParamNode = XmlHelper.newChildElement(doc, httpParamsNode, "http-param", null);
            XmlHelper.newTextChildElement(doc, httpParamNode, "name", StringUtils.join(paramDoc.names, ","), null);
            XmlHelper.newTextChildElement(doc, httpParamNode, "must", HttpApiDoclet.booleanToString(paramDoc.must), null);
            XmlHelper.newTextChildElement(doc, httpParamNode, "default", paramDoc.def, null);
            XmlHelper.newTextChildElement(doc, httpParamNode, "description", paramDoc.description, null);
        }
    }
}
