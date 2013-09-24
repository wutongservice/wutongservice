package com.borqs.server.market.resfile;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.dom4j.DocumentException;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceFile implements Closeable {;
    private final ZipFile zip;
    private final Manifest manifest;

    public ResourceFile(String path) throws IOException, DocumentException {
        this.zip = new ZipFile(path);
        this.manifest = Manifest.load(readFile("ResourceManifest.xml"));
    }

    @Override
    public void close() throws IOException {
        this.zip.close();
    }

    public String getPath() {
        return zip.getName();
    }

    public Manifest getManifest() {
        return manifest;
    }

    public String getId() {
        return manifest.getId();
    }

    public int getVersion() {
        return manifest.getVersion();
    }

    public String getDefaultLanguage() {
        return manifest.getDefaultLanguage();
    }

    public JsonNode getName() {
        return manifest.getName();
    }

    public JsonNode getVersionName() {
        return manifest.getVersionName();
    }

    public JsonNode getRecentChange() {
        return manifest.getRecentChange();
    }

    public JsonNode getDescription() {
        return manifest.getDescription();
    }

    public String getAppId() {
        return manifest.getAppId();
    }

    public String getCategory() {
        return manifest.getCategory();
    }

    public int getMinAppVersion() {
        return manifest.getMinAppVersion();
    }

    public int getMaxAppVersion() {
        return manifest.getMaxAppVersion();
    }

    public String[] getSupportedMod() {
        return manifest.getSupportedMod();
    }

    public String getLogoPath() {
        return manifest.getLogoPath();
    }

    public String getCoverPath() {
        return manifest.getCoverPath();
    }

    public String getScreenshot1Path() {
        return manifest.getScreenshot1Path();
    }

    public String getScreenshot2Path() {
        return manifest.getScreenshot2Path();
    }

    public String getScreenshot3Path() {
        return manifest.getScreenshot3Path();
    }

    public String getScreenshot4Path() {
        return manifest.getScreenshot4Path();
    }

    public String getScreenshot5Path() {
        return manifest.getScreenshot5Path();
    }

    private InputStream readFile(String name) throws IOException {
        return zip.getInputStream(new ZipEntry(name));
    }

    private static String trimPath(String p) {
        return StringUtils.removeStart(p, "/");
    }

    public InputStream readLogo() throws IOException {
        String p = trimPath(manifest.getLogoPath());
        return p != null ? readFile(p) : null;
    }

    public InputStream readCover() throws IOException {
        String p = trimPath(manifest.getCoverPath());
        return p != null ? readFile(p) : null;
    }

    public InputStream readScreenshot1() throws IOException {
        String p = trimPath(manifest.getScreenshot1Path());
        return p != null ? readFile(p) : null;
    }

    public InputStream readScreenshot2() throws IOException {
        String p = trimPath(manifest.getScreenshot2Path());
        return p != null ? readFile(p) : null;
    }

    public InputStream readScreenshot3() throws IOException {
        String p = trimPath(manifest.getScreenshot3Path());
        return p != null ? readFile(p) : null;
    }

    public InputStream readScreenshot4() throws IOException {
        String p = trimPath(manifest.getScreenshot4Path());
        return p != null ? readFile(p) : null;
    }

    public InputStream readScreenshot5() throws IOException {
        String p = trimPath(manifest.getScreenshot5Path());
        return p != null ? readFile(p) : null;
    }

    private static void saveStream(InputStream in, String path) throws IOException {
        if (in != null && path != null)
            FileUtils.copyInputStreamToFile(in, new File(path));
    }

    public void saveLogo(String path) throws IOException {
        saveStream(readLogo(), path);
    }

    public void saveCover(String path) throws IOException {
        saveStream(readCover(), path);
    }

    public void saveScreenshot1(String path) throws IOException {
        saveStream(readScreenshot1(), path);
    }

    public void saveScreenshot2(String path) throws IOException {
        saveStream(readScreenshot2(), path);
    }

    public void saveScreenshot3(String path) throws IOException {
        saveStream(readScreenshot3(), path);
    }

    public void saveScreenshot4(String path) throws IOException {
        saveStream(readScreenshot4(), path);
    }

    public void saveScreenshot5(String path) throws IOException {
        saveStream(readScreenshot5(), path);
    }

    public static void use(String resourcePath, UseHandler handler) throws IOException, DocumentException {
        ResourceFile res = new ResourceFile(resourcePath);
        try {
            handler.handle(res);
        } finally {
            IOUtils.closeQuietly(res);
        }
    }

    public static interface UseHandler {
        void handle(ResourceFile res) throws IOException, DocumentException;
    }
}
