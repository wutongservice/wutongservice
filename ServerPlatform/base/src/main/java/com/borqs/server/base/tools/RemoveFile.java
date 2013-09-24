package com.borqs.server.base.tools;



import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class RemoveFile {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("RemoveFile one_file_of_dir");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists())
            return;

        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
        } else {
            file.delete();
        }
    }
}
