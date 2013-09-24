package com.borqs.server.base.io;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class FilePair {
    public final String source;
    public final String destination;

    public FilePair(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    public static FilePair parse(String s) {
        String src = StringUtils.substringBefore(s, "|").trim();
        String dst = StringUtils.substringAfter(s, "|").trim();
        return new FilePair(src, dst);
    }

    @Override
    public String toString() {
        return String.format("%s|%s", source, destination);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FilePair filePair = (FilePair) o;

        return StringUtils.equals(source, filePair.source)
                && StringUtils.equals(destination, filePair.destination);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.hashCode(source);
        result = 31 * result + ObjectUtils.hashCode(destination);
        return result;
    }

    public static List<FilePair> loadFile(String path) {
        InputStream in = null;
        try {
            in = new VfsInputStream(path);
            List<String> lines = IOUtils.readLines(in);
            ArrayList<FilePair> filePairs = new ArrayList<FilePair>();
            for (String line : lines)
                filePairs.add(parse(line));
            return filePairs;
        } catch (IOException e) {
            throw new IOException2(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static void saveFile(List<FilePair> filePairs, String path) {
        PrintStream out = null;
        try {
            out = new PrintStream(new VfsOutputStream(path), true);
            for (FilePair filePath : filePairs)
                out.println(filePath.toString());
        } catch (IOException e) {
            throw new IOException2(e);
        }finally {
            IOUtils.closeQuietly(out);
        }
    }
}
