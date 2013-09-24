package com.borqs.server.market.utils;


import org.dom4j.Element;
import org.dom4j.Node;

public class Dom4jUtils {
    public static String attributeValue(Element elem, String attrName, String def) {
        String val = elem.attributeValue(attrName);
        return val != null ? val : def;
    }

    public static String selectTextValue(Element elem, String xpath, String def, boolean trim) {
        Node node = elem.selectSingleNode(xpath);
        if (node == null) {
            return def;
        } else {
            String val = node.getText();
            if (trim) {
                val = val.trim();
            }
            return val;
        }
    }

    public static int selectIntValue(Element elem, String xpath, int def) {
        String val = selectTextValue(elem, xpath, Integer.toString(def), true);
        return Integer.parseInt(val);
    }
}
