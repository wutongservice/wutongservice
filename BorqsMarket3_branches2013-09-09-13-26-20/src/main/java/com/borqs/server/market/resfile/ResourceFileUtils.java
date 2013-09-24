package com.borqs.server.market.resfile;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipFile;

public class ResourceFileUtils {

    public static boolean isResourceFile(File file) {
        try {
            String ext = FilenameUtils.getExtension(file.getName());
            ZipFile zf = new ZipFile(file);
            if ("apk".equalsIgnoreCase(ext)) {
                return zf.getEntry("assets/ResourceManifest.xml") != null;
            } else if ("zip".equalsIgnoreCase(ext)) {
                return zf.getEntry("ResourceManifest.xml") != null;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }


    private static String getFilename(File f) {
        return FilenameUtils.getName(f.getName());
    }

    private static InputStream openImage(Object fileOrUrl) throws IOException {
        if (fileOrUrl == null)
            return null;

        String localePath = null, url = null;
        if (fileOrUrl instanceof File) {
            localePath = ((File) fileOrUrl).getAbsolutePath();
        } else if (fileOrUrl instanceof URL) {
            return ((URL) fileOrUrl).openStream();
        } else if (fileOrUrl instanceof FileItem) {
            return ((FileItem) fileOrUrl).getInputStream();
        } else {
            String s = ObjectUtils.toString(fileOrUrl).trim();
            if (s.startsWith("http://") || s.startsWith("https://")) {
                url = s;
            } else {
                localePath = s;
            }
        }

        if (localePath != null) {
            return new FileInputStream(localePath);
        } else if (url != null) {
            URL url0 = new URL(url);
            return url0.openStream();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String getImageExt(Object fileOrUrl) {
        if (fileOrUrl instanceof File) {
            return FilenameUtils.getExtension(((File) fileOrUrl).getName());
        } else if (fileOrUrl instanceof URL) {
            return FilenameUtils.getExtension(((URL) fileOrUrl).getFile());
        } else if (fileOrUrl instanceof FileItem) {
            return FilenameUtils.getExtension(((FileItem) fileOrUrl).getName());
        } else {
            String s = ObjectUtils.toString(fileOrUrl).trim();
            return FilenameUtils.getExtension(s);
        }
    }

    private static InputStream openImageSafe(Object fileOrUrl) {
        try {
            return openImage(fileOrUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private static final String IMAGES_DIR = "_images_";

    private static void addImage(ZipArchiveOutputStream out, File outResPath, Manifest manifest, Record images, String imageName) throws IOException {
        Object imageFilePath = images.get(imageName);
        InputStream image = openImageSafe(imageFilePath);
        if (image != null) {
            String name = IMAGES_DIR + "/" + StringUtils.removeEnd(imageName, "_image") + "." + getImageExt(imageFilePath);
            writeContent(out, name, image);
            if ("logo_image".equalsIgnoreCase(imageName)) {
                manifest.setLogoPath(name);
            } else if ("cover_image".equalsIgnoreCase(imageName)) {
                manifest.setCoverPath(name);
            } else if ("screenshot1_image".equalsIgnoreCase(imageName)) {
                manifest.setScreenshot1Path(name);
            } else if ("screenshot2_image".equalsIgnoreCase(imageName)) {
                manifest.setScreenshot2Path(name);
            } else if ("screenshot3_image".equalsIgnoreCase(imageName)) {
                manifest.setScreenshot3Path(name);
            } else if ("screenshot4_image".equalsIgnoreCase(imageName)) {
                manifest.setScreenshot4Path(name);
            } else if ("screenshot5_image".equalsIgnoreCase(imageName)) {
                manifest.setScreenshot5Path(name);
            }
        }
    }

    private static void addImages(ZipArchiveOutputStream out, File outResPath, Manifest manifest, Record images) throws IOException {
        addImage(out, outResPath, manifest, images, "logo_image");
        addImage(out, outResPath, manifest, images, "cover_image");
        addImage(out, outResPath, manifest, images, "screenshot1_image");
        addImage(out, outResPath, manifest, images, "screenshot2_image");
        addImage(out, outResPath, manifest, images, "screenshot3_image");
        addImage(out, outResPath, manifest, images, "screenshot4_image");
        addImage(out, outResPath, manifest, images, "screenshot5_image");
    }



    public static void packResource(File outResPath, File contentPath, String contentFilename, Manifest manifest, Record images) {
        ZipArchiveOutputStream out = null;
        try {
            String ext = FilenameUtils.getExtension(contentPath.getName());
            if (!"zip".equalsIgnoreCase(ext)) {
                // raw content
                out = new ZipArchiveOutputStream(outResPath);
                writeFile(out, contentFilename == null ? getFilename(contentPath) : contentFilename, contentPath);
            } else {
                // zip without manifest
                out = new ZipArchiveOutputStream(outResPath);
                out = createZipByCopyWithoutMeta(outResPath, contentPath);
            }
            addImages(out, outResPath, manifest, images);
            writeText(out, "ResourceManifest.xml", manifest.toXml());
        } catch (Exception e) {
            throw new ServiceException(Errors.E_ZIP, "Package resource error", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private static ZipArchiveOutputStream createZipByCopyWithoutMeta(File outFile, File inFile) throws IOException {
        ZipArchiveInputStream in = null;
        try {
            in = new ZipArchiveInputStream(new FileInputStream(inFile));
            ZipArchiveOutputStream out = new ZipArchiveOutputStream(new FileOutputStream(outFile));
            ZipArchiveEntry entry;
            while ((entry = in.getNextZipEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equalsIgnoreCase("ResourceManifest.xml")
                        || StringUtils.startsWithIgnoreCase(entryName, IMAGES_DIR))
                    continue;

                out.putArchiveEntry((ZipArchiveEntry) entry.clone());
                IOUtils.copy(in, out);
                out.closeArchiveEntry();
            }
            return out;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /*
    private static void appendToZip(File outFile, File inFile, String entryName, InputStream entryData) throws IOException {
        ZipArchiveInputStream in = null;
        ZipArchiveOutputStream out = null;
        try {
            in = new ZipArchiveInputStream(new FileInputStream(inFile));
            out = new ZipArchiveOutputStream(new FileOutputStream(outFile));
            ZipArchiveEntry entry;
            while ((entry = in.getNextZipEntry()) != null) {
                if (StringUtils.equals(entry.getName(), entryName))
                    continue;

                out.putArchiveEntry(entry);
                IOUtils.copy(in, out);
                out.closeArchiveEntry();
            }
            ZipArchiveEntry newEntry = new ZipArchiveEntry(entryName);
            byte[] bytes = IOUtils.toByteArray(entryData);
            newEntry.setSize(bytes.length);
            out.putArchiveEntry(newEntry);
            out.write(bytes);
            out.closeArchiveEntry();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(entryData);
        }
    }
    */

    private static void writeText(ZipArchiveOutputStream out, String entryName, String text) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        byte[] bytes = text.getBytes("UTF-8");
        entry.setSize(bytes.length);
        out.putArchiveEntry(entry);
        out.write(bytes);
        out.closeArchiveEntry();
    }

    private static void writeFile(ZipArchiveOutputStream out, String entryName, File f) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        entry.setSize(f.length());
        out.putArchiveEntry(entry);
        FileUtils.copyFile(f, out);
        out.closeArchiveEntry();
    }

    private static void writeContent(ZipArchiveOutputStream out, String entryName, InputStream in) throws IOException {
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        byte[] bytes = IOUtils.toByteArray(in);
        entry.setSize(bytes.length);
        try {
            out.putArchiveEntry(entry);
            out.write(bytes);
            out.closeArchiveEntry();
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
