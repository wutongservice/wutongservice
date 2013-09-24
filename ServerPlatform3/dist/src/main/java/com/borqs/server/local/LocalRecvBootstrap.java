package com.borqs.server.local;


import com.borqs.server.platform.util.MacroExpander;

public class LocalRecvBootstrap {
    public static void main(String[] args) throws Throwable {
        String conf = MacroExpander.expandSystemMacros("${BS_HOME}/etc/share/conf.recv.xml");
        LocalAppBootstrap.main(new String[]{conf, "main"});
    }
}
