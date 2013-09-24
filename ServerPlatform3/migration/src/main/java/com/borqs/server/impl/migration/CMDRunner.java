package com.borqs.server.impl.migration;


import java.util.List;
import java.util.Properties;

public interface CMDRunner {
    List<String> getDependencies();
    void run(String cmd,Properties config);
}
