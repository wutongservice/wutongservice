package com.borqs.server.platform.util;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class NamedFiles extends HashMap<String, File> {
    public NamedFiles() {
    }

    public NamedFiles(Map<? extends String, ? extends File> m) {
        super(m);
    }


    public void deleteFilesByKey(String... keys) {
        // TODO: xx
    }


    public void deleteAllFiles() {
        // TODO: xx
    }
}
