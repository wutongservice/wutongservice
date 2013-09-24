package com.borqs.server.migrate;

import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

public class MoveFilesOnOSS {
    static OSSClient client ;
    static String accessId = "42azpskan63xg69r03qx7x9x";
    static String accessKey = "Jhas2oPaqmsHytGy/bNMzRPHHGo=";
//    static String ossEndpoint = "http://storage.aliyun.com/";
    static String ossEndpoint = "http://storage-vm.aliyun-inc.com/";

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

    public static void writeSegment(ObjectListing listing) {
        List<OSSObjectSummary> objSummaries = listing.getObjectSummaries();
        for (OSSObjectSummary objSummary : objSummaries) {
            String key = objSummary.getKey();
            writeLine(af, key);
        }
    }

    public static void deleteSegment(String bucketName, ObjectListing listing) throws Exception {
        List<OSSObjectSummary> objSummaries = listing.getObjectSummaries();
        for (OSSObjectSummary objSummary : objSummaries) {
            String key = objSummary.getKey();
            client.deleteObject(bucketName, key);
        }
    }

    public static void generateFileList(String bucketName) throws Exception {
        ObjectListing listing = client.listObjects(bucketName);
        writeSegment(listing);

        ListObjectsRequest request = new ListObjectsRequest(bucketName);
        request.setMaxKeys(1000);
        while (listing.isTruncated()) {
            request.setMarker(listing.getNextMarker());
            listing = client.listObjects(request);
            writeSegment(listing);
        }
    }
    
    public static void deleteBucket(String bucketName) throws Exception {
        ObjectListing listing = client.listObjects(bucketName);
        deleteSegment(bucketName, listing);

        ListObjectsRequest request = new ListObjectsRequest(bucketName);
        request.setMaxKeys(1000);
        while (listing.isTruncated()) {
            request.setMarker(listing.getNextMarker());
            listing = client.listObjects(request);
            deleteSegment(bucketName, listing);
        }

        client.deleteBucket(bucketName);
    }
    
    public static void moveFiles(String srcBucket, String destBucket, String destPrefix) throws Exception {
        String[] keys = all.toArray(new String[all.size()]);
        for (String key : keys) {
            if (!success.contains(key)) {
                try {
                    OSSObject ossObj = client.getObject(srcBucket, key);
                    ObjectMetadata objectMeta = ossObj.getObjectMetadata();
                    client.putObject(destBucket, destPrefix + key, ossObj.getObjectContent(), objectMeta);
                    success.add(key);
                    writeLine(sf, key);
                } catch (Exception e) {
                    failed.add(key);
                    writeLine(ff, key);
                    continue;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        client = new OSSClient(ossEndpoint, accessId, accessKey);

        if (ArrayUtils.isEmpty(args)) {
            System.out.println("Usage: MoveFilesOnOSS [function] [arg] ...");
            System.out.println("      function=1: [function] [bucketName] [listFilePath]");
            System.out.println("      function=2: [function] [listFilePath] [srcBucket] [destBucket] [destPrefix] [successFilePath] [failedFilePath]");
            System.out.println("      function=3: [function] [bucketName]");
            return;
        }
        String function = args[0];
        if (StringUtils.equals(function, "1")) {
            String bucketName = args[1];
            String listFilePath = args[2];

            af = new File(listFilePath);
            if (af.exists())
                af.delete();
            af.createNewFile();

            generateFileList(bucketName);
        } else if (StringUtils.equals(function, "2")) {
            String listFilePath = args[1];
            String srcBucket = args[2];
            String destBucket = args[3];
            String destPrefix = args[4];
            String successFilePath = args[5];
            String failedFilePath = args[6];

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

            moveFiles(srcBucket, destBucket, destPrefix);
        } else if (StringUtils.equals(function, "3")) {
            String bucketName = args[1];
            deleteBucket(bucketName);
        }
    }
}
