package com.borqs.server.test.platform.util;


import com.borqs.server.platform.util.MacroExpander;
import junit.framework.TestCase;


public class ExpandMacrosTest extends TestCase {
    public void testExpand() {
        System.setProperty("BS_HOME", "/bs_home2");
        String ss = MacroExpander.expandSystemMacros("${BS_HOME}/eee");
        assertEquals("/bs_home2/eee", ss);
    }
}
