package com.borqs.server.migrate;

import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.OSSObject;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.List;

public class FilesToOSS {
    static OSSClient client ;
    static String accessId = "42azpskan63xg69r03qx7x9x";
    static String accessKey = "Jhas2oPaqmsHytGy/bNMzRPHHGo=";
    static String ossEndpoint = "http://storage.aliyun.com/";

    static LinkedHashSet<String> all;
    static LinkedHashSet<String> success;
    static LinkedHashSet<String> failed;
    static File af;
    static File sf;
    static File ff;
    
    public static void writeLine(File f, String content) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(f, true));
            bw.write(content);
            bw.newLine();
        } catch (IOException e) {

        } finally {
            try {
                bw.close();
            } catch (IOException e) {

            }
        }
    }

    public static void uploadFailedFiles(String bucketName) {
        String[] fl = failed.toArray(new String[failed.size()]);
        for (String path : fl) {
            String fn = StringUtils.substringAfterLast(path, "/");
            String suffix = StringUtils.substringAfterLast(fn, ".");
            File f = new File(path);
            if (f.isFile()) {
                ObjectMetadata objectMeta = new ObjectMetadata();
                objectMeta.setContentLength(f.length());
                if (StringUtils.equalsIgnoreCase(suffix, "png"))
                    objectMeta.setContentType("image/png");
                else if (StringUtils.equalsIgnoreCase(suffix, "jpg")
                        || StringUtils.equalsIgnoreCase(suffix, "jpeg"))
                    objectMeta.setContentType("image/jpeg");
                else if (StringUtils.equalsIgnoreCase(suffix, "gif"))
                    objectMeta.setContentType("image/gif");
                else if (StringUtils.equalsIgnoreCase(suffix, "tif")
                        || StringUtils.equalsIgnoreCase(suffix, "tiff"))
                    objectMeta.setContentType("image/tiff");
                try {
                    InputStream input = new FileInputStream(f);
                    client.putObject(bucketName, fn, input, objectMeta);
                    input.close();
                    success.add(path);
                    writeLine(sf, path);
                    failed.remove(path);
                } catch (Exception e) {
                    failed.add(path);
                    continue;
                }
            }
        }
        try {
            FileUtils.writeLines(ff, failed);
        } catch (IOException e) {

        }
    }

    public static void uploadFilesToOSS(String bucketName) {
        String[] fl = all.toArray(new String[all.size()]);
        for (String path : fl) {
            String fn = StringUtils.substringAfterLast(path, "/");
            String suffix = StringUtils.substringAfterLast(fn, ".");
            File f = new File(path);
            if (f.isFile() && !success.contains(path)) {
                ObjectMetadata objectMeta = new ObjectMetadata();
                objectMeta.setContentLength(f.length());
                if (StringUtils.equalsIgnoreCase(suffix, "png"))
                    objectMeta.setContentType("image/png");
                else if (StringUtils.equalsIgnoreCase(suffix, "jpg")
                        || StringUtils.equalsIgnoreCase(suffix, "jpeg"))
                    objectMeta.setContentType("image/jpeg");
                else if (StringUtils.equalsIgnoreCase(suffix, "gif"))
                    objectMeta.setContentType("image/gif");
                else if (StringUtils.equalsIgnoreCase(suffix, "tif")
                        || StringUtils.equalsIgnoreCase(suffix, "tiff"))
                    objectMeta.setContentType("image/tiff");
                try {
                    InputStream input = new FileInputStream(f);
                    client.putObject(bucketName, fn, input, objectMeta);
                    input.close();
                    success.add(path);
                    writeLine(sf, path);
                } catch (Exception e) {
                    failed.add(path);
                    writeLine(ff, path);
                    continue;
                }
            }
        }
    }
    
    public static void modifyContentTypeOnOSS(String bucketName) {
        String[] fl = all.toArray(new String[all.size()]);
        for (String path : fl) {
            String fn = StringUtils.substringAfterLast(path, "/");
            String suffix = StringUtils.substringAfterLast(fn, ".");
            if (!success.contains(path) && !StringUtils.equalsIgnoreCase(suffix, "apk")) {
                try {
                    OSSObject ossObj = client.getObject(bucketName, fn);
                    ObjectMetadata objectMeta = ossObj.getObjectMetadata();

                    if (StringUtils.equalsIgnoreCase(suffix, "png"))
                        objectMeta.setContentType("image/png");
                    else if (StringUtils.equalsIgnoreCase(suffix, "jpg")
                            || StringUtils.equalsIgnoreCase(suffix, "jpeg"))
                        objectMeta.setContentType("image/jpeg");
                    else if (StringUtils.equalsIgnoreCase(suffix, "gif"))
                        objectMeta.setContentType("image/gif");
                    else if (StringUtils.equalsIgnoreCase(suffix, "tif")
                            || StringUtils.equalsIgnoreCase(suffix, "tiff"))
                        objectMeta.setContentType("image/tiff");

                    client.putObject(bucketName, fn, ossObj.getObjectContent(), objectMeta);
                    success.add(path);
                    writeLine(sf, path);
                } catch (Exception e) {
                    failed.add(path);
                    writeLine(ff, path);
                    continue;
                }
            }
        }
    }
    
    public static void generateFileList(String uploadDir) throws Exception {
        File ud = new File(uploadDir);
        String[] fl = ud.list();
        for (String fn : fl) {
            String path = uploadDir + File.separator + fn;

            File f = new File(path);
            if (f.isFile())
                writeLine(af, path);
            else {
                generateFileList(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        client = new OSSClient(ossEndpoint, accessId, accessKey);

        if (ArrayUtils.isEmpty(args)) {
            System.out.println("Usage: FilesToOSS [function] [arg] ...");
            System.out.println("      function=1: [function] [uploadDir] [listFilePath]");
            System.out.println("      function=2: [function] [listFilePath] [bucketName] [successFilePath] [failedFilePath]");
            System.out.println("      function=3: [function] [listFilePath] [bucketName] [successFilePath] [failedFilePath]");
            return;
        }
        String function = args[0];
        if (StringUtils.equals(function, "1")) {
            String uploadDir = args[1];
            String listFilePath = args[2];

            af = new File(listFilePath);
            if (af.exists())
                af.delete();
            af.createNewFile();

            generateFileList(uploadDir);
        } else if (StringUtils.equals(function, "2")) {
            String listFilePath = args[1];
            String bucketName = args[2];
            String successFilePath = args[3];
            String failedFilePath = args[4];

            af = new File(listFilePath);
            sf = new File(successFilePath);
            ff = new File(failedFilePath);
            if (!af.exists()) {
                System.out.println("The list file is not exist!");
                return;
            }
            if (!sf.exists())
                sf.createNewFile();
            if (!ff.exists())
                ff.createNewFile();

            List<String> al = FileUtils.readLines(af);
            all = new LinkedHashSet<String>(al);
            
            List<String> sl = FileUtils.readLines(sf);
            success = new LinkedHashSet<String>(sl);

            List<String> fl = FileUtils.readLines(ff);
            failed = new LinkedHashSet<String>(fl);

            uploadFilesToOSS(bucketName);

            while (failed.size() > 0) {
                uploadFailedFiles(bucketName);
            }
        } else if(StringUtils.equals(function, "3")) {
            String listFilePath = args[1];
            String bucketName = args[2];
            String successFilePath = args[3];
            String failedFilePath = args[4];

            af = new File(listFilePath);
            sf = new File(successFilePath);
            ff = new File(failedFilePath);
            if (!af.exists()) {
                System.out.println("The list file is not exist!");
                return;
            }
            if (!sf.exists())
                sf.createNewFile();
            if (!ff.exists())
                ff.createNewFile();

            List<String> al = FileUtils.readLines(af);
            all = new LinkedHashSet<String>(al);

            List<String> sl = FileUtils.readLines(sf);
            success = new LinkedHashSet<String>(sl);

            List<String> fl = FileUtils.readLines(ff);
            failed = new LinkedHashSet<String>(fl);

            modifyContentTypeOnOSS(bucketName);
        }
    }
}
