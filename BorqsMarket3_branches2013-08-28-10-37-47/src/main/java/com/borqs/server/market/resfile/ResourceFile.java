package com.borqs.server.market.resfile;


import com.borqs.server.market.models.VersionedProductId;
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
    private Manifest manifest;

    public ResourceFile(File file) throws IOException, DocumentException {
        this(file.getAbsolutePath());
    }

    public ResourceFile(String path) throws IOException, DocumentException {
        this.zip = new ZipFile(path);
        this.manifest = Manifest.load(readFile("ResourceManifest.xml", "assets/ResourceManifest.xml"));
    }

    public static ResourceFile createQuietly(File file) {
        return file != null ? createQuietly(file.getAbsolutePath()) : null;
    }

    public static ResourceFile createQuietly(String path) {
        try {
            return new ResourceFile(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        this.zip.close();
    }

    public String getPath() {
        return zip.getName();
    }

    public File getFile() {
        return new File(getPath());
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

    public String getAuthorName() {
        return manifest.getAuthorName();
    }

    public String getAuthorPhone() {
        return manifest.getAuthorPhone();
    }

    public String getAuthorEmail() {
        return manifest.getAuthorEmail();
    }

    public String getAuthorWebsite() {
        return manifest.getAuthorWebsite();
    }

    public String getTags() {
        return manifest.getTags();
    }

    public List<VersionedProductId> getDependencies() {
        return manifest.getDependencies();
    }

    public String getDefaultName(String def) {
        return manifest.getDefaultName(def);
    }

    public String getDefaultDescription(String def) {
        return manifest.getDefaultDescription(def);
    }

    private InputStream readFile(String name) throws IOException {
        return zip.getInputStream(new ZipEntry(name));
    }

    private boolean fileExists(String name) {
        if (StringUtils.isBlank(name))
            return false;

        ZipEntry ze = zip.getEntry(name);
        return ze != null && !ze.isDirectory();
    }

    private boolean fileExists(String name, String altName) {
        return fileExists(name) || fileExists(altName);
    }

    private InputStream readFile(String name, String altName) throws IOException {
        InputStream in;
        try {
            in = readFile(name);
        } catch (IOException e) {
            in = null;
        }

        if (in != null) {
            return in;
        } else {
            return readFile(altName);
        }
    }

    private static String trimPath(String p) {
        return StringUtils.removeStart(p, "/");
    }

    public boolean logoExists() {
        String p = trimPath(manifest.getLogoPath());
        return fileExists(p, "assets/" + p);
    }

    public boolean coverExists() {
        String p = trimPath(manifest.getCoverPath());
        return fileExists(p, "assets/" + p);
    }

    public boolean screenshot1Exists() {
        String p = trimPath(manifest.getScreenshot1Path());
        return fileExists(p, "assets/" + p);
    }

    public boolean screenshot2Exists() {
        String p = trimPath(manifest.getScreenshot2Path());
        return fileExists(p, "assets/" + p);
    }

    public boolean screenshot3Exists() {
        String p = trimPath(manifest.getScreenshot3Path());
        return fileExists(p, "assets/" + p);
    }

    public boolean screenshot4Exists() {
        String p = trimPath(manifest.getScreenshot4Path());
        return fileExists(p, "assets/" + p);
    }

    public boolean screenshot5Exists() {
        String p = trimPath(manifest.getScreenshot5Path());
        return fileExists(p, "assets/" + p);
    }

    public String getImagePath(String imageField) {
        if ("logo_image".equals(imageField)) {
            return getLogoPath();
        } else if ("cover_image".equals(imageField)) {
            return getCoverPath();
        } else if ("screenshot1_image".equals(imageField)) {
            return getScreenshot1Path();
        } else if ("screenshot2_image".equals(imageField)) {
            return getScreenshot2Path();
        } else if ("screenshot3_image".equals(imageField)) {
            return getScreenshot3Path();
        } else if ("screenshot4_image".equals(imageField)) {
            return getScreenshot4Path();
        } else if ("screenshot5_image".equals(imageField)) {
            return getScreenshot5Path();
        } else {
            return null;
        }
    }

    public InputStream readImage(String imageField) throws IOException {
        if ("logo_image".equals(imageField)) {
            return readLogo();
        } else if ("cover_image".equals(imageField)) {
            return readCover();
        } else if ("screenshot1_image".equals(imageField)) {
            return readScreenshot1();
        } else if ("screenshot2_image".equals(imageField)) {
            return readScreenshot2();
        } else if ("screenshot3_image".equals(imageField)) {
            return readScreenshot3();
        } else if ("screenshot4_image".equals(imageField)) {
            return readScreenshot4();
        } else if ("screenshot5_image".equals(imageField)) {
            return readScreenshot5();
        } else {
            return null;
        }
    }

    public InputStream readLogo() throws IOException {
        String p = trimPath(manifest.getLogoPath());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readCover() throws IOException {
        String p = trimPath(manifest.getCoverPath());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readScreenshot1() throws IOException {
        String p = trimPath(manifest.getScreenshot1Path());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readScreenshot2() throws IOException {
        String p = trimPath(manifest.getScreenshot2Path());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readScreenshot3() throws IOException {
        String p = trimPath(manifest.getScreenshot3Path());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readScreenshot4() throws IOException {
        String p = trimPath(manifest.getScreenshot4Path());
        return p != null ? readFile(p, "assets/" + p) : null;
    }

    public InputStream readScreenshot5() throws IOException {
        String p = trimPath(manifest.getScreenshot5Path());
        return p != null ? readFile(p, "assets/" + p) : null;
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
