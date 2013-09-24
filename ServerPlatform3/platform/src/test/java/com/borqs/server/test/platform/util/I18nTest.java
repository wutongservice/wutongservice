package com.borqs.server.test.platform.util;


import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.I18nHelper;
import com.borqs.server.platform.util.template.FreeMarker;
import com.borqs.server.test.platform.util.i18n.PackageClass;
import junit.framework.TestCase;

import java.util.Map;
import java.util.ResourceBundle;

public class I18nTest extends TestCase {
    public void testI18n() {
        ResourceBundle bundle = I18nHelper.getBundle("com.borqs.server.test.platform.util.i18n.Test", "");
        assertEquals(bundle.getString("test.key1"), "Value1");
        assertEquals(bundle.getString("test.key2"), "Value2");

        bundle = I18nHelper.getBundle("com.borqs.server.test.platform.util.i18n.Test", "en");
        assertEquals(bundle.getString("test.key1"), "Value1");
        assertEquals(bundle.getString("test.key2"), "Value2");

        bundle = I18nHelper.getBundle("com.borqs.server.test.platform.util.i18n.Test", "zh");
        assertEquals(bundle.getString("test.key1"), "值1");
        assertEquals(bundle.getString("test.key2"), "值2");
    }

    public void testFreeMarker() {
        FreeMarker fm = new FreeMarker(PackageClass.class);

        Map<String, Object> params = CollectionsHelper.arraysToMap(new Object[][] {
                {"msg", "AAA"},
        });

        assertEquals("Hello_AAA", fm.merge("F1.ftl", "", params).trim());
        assertEquals("Hello_AAA", fm.merge("F1.ftl", "en", params).trim());
        assertEquals("Hello_AAA", fm.merge("F1.ftl", "en_US", params).trim());
        assertEquals("你好_AAA", fm.merge("F1.ftl", "zh", params).trim());
        assertEquals("你好_AAA", fm.merge("F1.ftl", "zh_CN", params).trim());
    }
}
