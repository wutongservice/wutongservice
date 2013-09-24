package com.borqs.server.platform.util;


import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Map;

public class XmlHelper {


    public static void setAttributes(Element elem, Map<String, Object> attrs) {
        if (MapUtils.isNotEmpty(attrs)) {
            for (Map.Entry<String, Object> e : attrs.entrySet())
                elem.setAttribute(e.getKey(), ObjectUtils.toString(e.getValue()));
        }
    }

    public static Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().newDocument();
    }

    public static Element newChildElement(Document doc, Element elem, String tagName, Map<String, Object> attrs) {
        Element child = doc.createElement(tagName);
        elem.appendChild(child);
        setAttributes(child, attrs);
        return child;
    }

    public static Element setRootElement(Document doc, String tagName, Map<String, Object> attrs) {
        Element child = doc.createElement(tagName);
        doc.appendChild(child);
        setAttributes(child, attrs);
        return child;
    }

    public static Element newTextChildElement(Document doc, Element elem, String tagName, String text, Map<String, Object> attrs) {
        Element child = newChildElement(doc, elem, tagName, attrs);
        child.appendChild(doc.createTextNode(text));
        return child;
    }

    public static void writeDocument(Document doc, PrintWriter out, String encoding, boolean human) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMSource source= new DOMSource(doc);
        transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
        transformer.setOutputProperty(OutputKeys.INDENT, human ? "yes" : "no");
        StreamResult result = new StreamResult(out);
        transformer.transform(source, result);
    }

    public static Document loadDocument(InputStream in) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(in);
    }

    public static Document loadDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
