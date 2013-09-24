package com.borqs.server.base.tools;


import ch.qos.logback.core.helpers.ThrowableToStringArray;
import com.borqs.server.base.io.FilePair;
import com.borqs.server.base.io.VfsUtils;

import java.util.List;

public class CopyFiles {
    public static void main(String... args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String schemeFile = args[0];
        boolean override = args.length >= 2 && args[1].equalsIgnoreCase("override");
        List<FilePair> filePairs = FilePair.loadFile(schemeFile);
        copyFiles(filePairs, override, true);
    }

    private static final int OK = 1;
    private static final int IGNORED = 0;
    private static final int ERROR = 2;

    public static void copyFiles(List<FilePair> filePairs, boolean override, boolean output) {
        if (output)
            System.out.println(String.format("copy files - override:%s", override));

        int n = 0;
        for (FilePair filePair : filePairs) {
            int r;
            Throwable error = null;
            try {
                r = VfsUtils.copyFile(filePair, override) ? OK : IGNORED;
            } catch (Throwable t) {
                r = ERROR;
                error = t;
            }
            if (output) {
                String msg = String.format("%s (%s/%s): %s", makeTitle(r), n, filePairs.size(), filePair);
                System.out.println(msg);
                if (error != null)
                    error.printStackTrace(System.err);
            }
            n++;
        }
    }

    private static String makeTitle(int status) {
        switch (status) {
            case OK:
                return "OK     ";
            case IGNORED:
                return "IGNORED";
            case ERROR:
                return "ERROR  ";
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void printUsage() {
        System.out.println("CopyFiles scheme_file [override]");
    }
}
