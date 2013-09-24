package com.borqs.server.impl.link;


import com.borqs.server.impl.link.linkutils.CharsetDetector;
import com.borqs.server.impl.link.linkutils.JpgScaleZoom;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.data.RecordSet;
import com.borqs.server.platform.feature.link.LinkEntity;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.sfs.SFS;
import com.borqs.server.platform.util.RandomHelper;
import com.borqs.server.platform.util.image.ImageMagickCommand;
import net.sf.json.JSONArray;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Parser;
import org.htmlparser.beans.StringBean;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.HtmlPage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sharelink {

    private static final Logger L = Logger.get(Sharelink.class);
    private static final int IMGCOUNT = 5;


    private SFS linkSFS;
    private long linkImageScale = 210;
    private String linkImgUrlPattern = "http://apitest.borqs.com/links/%s";

    public Sharelink() {
    }

    public SFS getLinkSFS() {
        return linkSFS;
    }

    public void setLinkSFS(SFS linkSFS) {
        this.linkSFS = linkSFS;
    }

    public long getLinkImageScale() {
        return linkImageScale;
    }

    public void setLinkImageScale(long linkImageScale) {
        this.linkImageScale = linkImageScale;
    }

    public String getLinkImgUrlPattern() {
        return linkImgUrlPattern;
    }

    public void setLinkImgUrlPattern(String linkImgUrlPattern) {
        this.linkImgUrlPattern = linkImgUrlPattern;
    }



    public RecordSet shareImgLink(String url) throws Exception {
        RecordSet out_recs = new RecordSet();
        Record out_rec = new Record();
        out_rec.put("url", url);
        URL ur = null;

        try {
            ur = new URL(url);
        } catch (MalformedURLException e) {
            L.error(null, "new URL error");
        }

        String host = ur.getHost();
        out_rec.put("host", host);
        out_rec.put("title", "");
        out_rec.put("description", "share image");

        JSONArray ja = new JSONArray();
        ja.clear();

        String img_name = getSaveImgName(url);
        InputStream input = getUrlImage(url);

        if (input != null) {
            L.debug(null, "Save img to local success,Url=" + url);

            //compitable with oss
            String img_name_small = img_name.replace("big", "small");
            byte[] imgBuf = IOUtils.toByteArray(input);
            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(imgBuf));
            } catch (Exception e) {
                L.error(null, "read img error,img_name=" + img_name);

                //in this case, all image is ok, but fail to process in ImageIO, may caused by watermarker
                //save the original as the showing image
            }

            long sw = 0;
            long sh = 0;
            if (image != null) {
                long bw = image.getWidth();
                long bh = image.getHeight();
                long scale = linkImageScale;

                if (bw > bh) {
                    sh = scale;
                    sw = (bw * scale) / bh;
                } else {
                    sw = scale;
                    sh = (bh * scale) / bw;
                }
            }
//            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name, String.valueOf(bw), String.valueOf(bh), StringUtils.substringAfterLast(img_name, "."));
            //SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name_small, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(img_name_small, "."));
            LinkImageHelper.saveLinkImage(linkSFS, imgBuf, img_name_small, (int)sw, (int)sh);

            String imgUrl = StringUtils.isNotBlank(img_name_small) ? String.format(linkImgUrlPattern, img_name_small) : img_name_small;
            L.debug(null, "make img success,new url=:" + imgUrl);
            ja.add(imgUrl);
        }

        out_rec.put("many_img_url", ja.toString());
        if (ja.size() > 0) {
            out_rec.put("img_url", ja.get(0).toString());
        } else {
            out_rec.put("img_url", "");
        }
        out_recs.add(out_rec);
        L.debug(null, "out_recs=" + out_recs.toJson(false, false));
        return out_recs;
    }

    private final static int retryIntervalSeconds = 2;
    private final static int retry_count = 2;

    public RecordSet shareLink(String url) throws Exception {
        RecordSet out_recs = new RecordSet();
        String newURL = url;

        int retriedCount = 0;
        for (retriedCount = 0; retriedCount < retry_count; retriedCount++) {
            try {
                URL ur = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) ur.openConnection();
                if (conn.getResponseCode() != 200 ||
                        retriedCount == retry_count) {
                    L.error(null, "url:" + url + " can not allow to read!");
                    return out_recs;
                }
                if (!conn.getURL().toString().trim().equalsIgnoreCase(url.trim()))
                    newURL = conn.getURL().toString().trim();
            } catch (MalformedURLException e) {
                L.error(null, "new URL error");
            } catch (Exception ne) {
                if (retriedCount == retry_count) {
                    throw ne;
                } else {
                    try {
                        L.info(null, "Sleeping " + retryIntervalSeconds + " seconds until the next retry. url=" + url);
                        Thread.sleep(retryIntervalSeconds * 1000);
                    } catch (InterruptedException ignore) {
                        //nothing to do
                    }
                    //do next time
                    continue;
                }
            }

            boolean fetch_ret = true;
            try {
                if (ifImgUrl(newURL)) {
                    out_recs = shareImgLink(newURL);
                } else {
                    out_recs = shareHtmlLink(newURL);
                }
            } catch (Exception ne) {
                fetch_ret = false;
                if (retriedCount == retry_count) {
                    throw ne;
                }
                try {
                    L.info(null, "Sleeping " + retryIntervalSeconds + " seconds until the next retry.");
                    Thread.sleep(retryIntervalSeconds * 1000);
                } catch (InterruptedException ignore) {
                    //nothing to do
                }
            }

            if (fetch_ret) {
                break;
            }
        }
        return out_recs;
    }

    //==================================only share html====================================

    public RecordSet shareHtmlLink(String url) throws Exception {
        RecordSet out_recs = new RecordSet();
        Record out_rec = new Record();
        // put url
        out_rec.put("url", url);

        URL ur = null;
        try {
            ur = new URL(url);
        } catch (MalformedURLException e) {
            L.error(null, "new URL error");
        }

        //put host
        String host = ur.getHost();
        out_rec.put("host", host);

        byte[] buff = null;
        buff = IOUtils.toByteArray(ur.openStream());
        String charset = CharsetDetector.guessCharset(buff);
        if (charset == null || charset.equals(""))
            charset = "utf-8";

        String s = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ur.openStream(), charset));
        StringBuffer sb = new StringBuffer();
        while ((s = in.readLine()) != null) {
            sb.append(s);
        }
        String result = sb.toString().trim();
        if (result.isEmpty())
            L.error(null, "this page is not allow to read!");

        String regex = "<script[\\s\\S]*?</script>|<!--[\\s\\S]*?-->|<style[\\s\\S]*?</style>|<![\\s\\S]*?>";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher match = pattern.matcher(result);
        result = match.replaceAll("");

        Parser parser = Parser.createParser(result, charset);
        HtmlPage pagetext = new HtmlPage(parser);
        parser.reset();
        parser.visitAllNodesWith(pagetext);

        out_rec.put("title", pagetext.getTitle());

        String img_id = "";
        Record rec_privacy = new Record();//ss.getLink(host);

        if (rec_privacy.isEmpty()) {
            String backStr = getLongText(getContext(result));
            out_rec.put("description", StringUtils.substring(backStr, 0, 100));
        } else {
        }
        if (out_rec.getString("description").length() == 0) {
            String backStr = getLongText(getContext(result));
            out_rec.put("description", StringUtils.substring(backStr, 0, 100));
        }

        JSONArray ja = new JSONArray();
        ja.clear();
        ja = getImgFromPage(url, getUrlHead(url) + host, pagetext, img_id);
        out_rec.put("many_img_url", ja.toString());
        if (ja.size() > 0) {
            out_rec.put("img_url", ja.get(0).toString());
        } else {
            out_rec.put("img_url", "");
        }

        out_recs.add(out_rec);
        parser.reset();
        L.debug(null, "out_recs=" + out_recs.toJson(false, false));
        return out_recs;
    }


    public String getSaveImgName(String URLName) throws Exception {
        URL url = new URL(URLName);

        String path = new File(url.getPath()).getName();
        String extendName = path;
        int lastPos = path.lastIndexOf(".");
        if (lastPos > 0) {
            extendName = getImageFileName(path.substring(lastPos, path.length()));
        }

        String imgName = Long.toString(RandomHelper.generateId()) + "_big" + extendName;
        return imgName;
    }

    public String getUrlImgAndSave(String URLName, String saveUrl) throws Exception {
        int HttpResult = 0;
        try {
            URL url = new URL(URLName);
            //String[] ss = StringUtils2.splitArray(url.toString(), ".", true);

            //any exception???

            String path = new File(url.getPath()).getName();
            String extendName = path;
            int lastPos = path.lastIndexOf(".");
            if (lastPos > 0) {
                extendName = getImageFileName(path.substring(lastPos, path.length()));
            }

            String imgName = Long.toString(RandomHelper.generateId()) + "_big" + extendName;
            String target = saveUrl + imgName;

            URLConnection urlConn = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            HttpResult = httpConn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) {
            } else {
                BufferedInputStream bis = new BufferedInputStream(urlConn.getInputStream());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
                byte[] buffer = new byte[1024];
                int num = -1;
                while (true) {
                    num = bis.read(buffer);
                    if (num == -1) {
                        bos.flush();
                        break;
                    }
                    bos.flush();
                    bos.write(buffer, 0, num);
                }
                bos.close();
                bis.close();
            }
            httpConn.setConnectTimeout(10000);
            httpConn.disconnect();
            return target;
        } catch (Exception e) {
            L.error(null, e.toString(), e);
            return "";
        }
    }

    public String getAndMakeImg(String urlHead, NodeList imgNodeList, Pattern pat, int imgIndex) throws Exception {
        String outTarget = "";
        ImageTag imglink = (ImageTag) imgNodeList.elementAt(imgIndex);
        String src = "";
        try {
            src = imglink.getImageURL();
        } catch (Exception e) {
            L.debug(null, "getImageURL error , ");
            return outTarget;
        }

        if (src != null) {
            Matcher matcher = pat.matcher(src);
//            String target = "";
            InputStream input = null;
            String img_name = "";
            if (matcher.find()) {
//                target = getUrlImgAndSave(src, linkImagAddr);
                img_name = getSaveImgName(src);
                input = getUrlImage(src);
            } else {
                Matcher matcher1 = pat.matcher(urlHead + src);
                if (matcher1.find()) {
//                    target = getUrlImgAndSave(urlHead + src, linkImagAddr);
                    img_name = getSaveImgName(urlHead + src);
                    input = getUrlImage(urlHead + src);
                } else {
                    return outTarget;
                }
            }

            if (input != null) {

                //compitable with oss
                String img_name_small = img_name.replace("big", "small");
                byte[] imgBuf = IOUtils.toByteArray(input);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imgBuf));
                long bw = image.getWidth();
                long bh = image.getHeight();
                long scale = linkImageScale;
                long sw = 0;
                long sh = 0;
                if (bw > bh) {
                    sh = scale;
                    sw = (bw * scale) / bh;
                } else {
                    sw = scale;
                    sh = (bh * scale) / bw;
                }
//                SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name, String.valueOf(bw), String.valueOf(bh), StringUtils.substringAfterLast(img_name, "."));
                //SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name_small, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(img_name_small, "."));
                LinkImageHelper.saveLinkImage(linkSFS, imgBuf, img_name_small, (int)sw, (int)sh);

                String imgUrl = StringUtils.isNotBlank(img_name_small) ? String.format(linkImgUrlPattern, img_name_small) : img_name_small;
                outTarget = imgUrl;
            } else {
                outTarget = "";
            }
        } else {
            outTarget = "";
        }
        return outTarget;
    }

    public JSONArray getImgFromPage(String url, String urlHead, HtmlPage page, String tagName) throws Exception {
        NodeList nodeList = page.getBody();
        TagNameFilter imgFilter = new TagNameFilter("IMG");
        NodeList imgNodeList = nodeList.extractAllNodesThatMatch(imgFilter, true);

        List<String> img_ll = new ArrayList<String>();
        for (int i = 0; i < imgNodeList.size(); i++) {
            ImageTag imglink = (ImageTag) imgNodeList.elementAt(i);
            String src = imglink.getImageURL();
            if (!img_ll.contains(src))
                img_ll.add(src);
        }

        Pattern pat = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
//        Configuration conf = Configuration.loadFiles(Constants.confPath).expandMacros();
        boolean hasNode = false;

        int imgIndex = 0;
        for (int i = 0; i < imgNodeList.size(); i++) {
            ImageTag imglink = (ImageTag) imgNodeList.elementAt(i);
            String imgname = imglink.getTagName();
            if (imgname.equals(tagName)) {
                hasNode = true;
                imgIndex = i;
                break;
            }
        }

        JSONArray ja = new JSONArray();
        ja.clear();

        if (hasNode) {
            String img_node = getAndMakeImg(urlHead, imgNodeList, pat, imgIndex);
            if (!StringUtils.isEmpty(img_node))
                ja.add(img_node);
        } else {
            if (img_ll.size() > 0) {
                int saveImgCount = 0;
                for (int i = 0; i < img_ll.size(); i++) {
//                    ImageTag imglink = (ImageTag) imgNodeList.elementAt(i);
//                    String src = imglink.getImageURL();
                    String src = img_ll.get(i);
//                    String target = "";
                    InputStream input = null;
                    String img_name = "";
                    Matcher matcher = pat.matcher(src);
                    if (matcher.find()) {
                        L.debug(null, "matcher find");
                    } else {
                        src = newUrlFormat(url, src);
                    }
                    img_name = getSaveImgName(src);
                    StringBuilder retryUrl=new StringBuilder();
                    input = getUrlImage(src,retryUrl);
                    if(retryUrl.length() > 0)
                    {
                        img_name = getSaveImgName(retryUrl.toString());
                    }

                    if (input != null) {
                        L.debug(null, "Save img to local success,Url=" + src);

                        //compitable with oss
                        String img_name_small = img_name.replace("big", "small");
                        byte[] imgBuf = IOUtils.toByteArray(input);
                        BufferedImage image = null;
                        try {
                            image = ImageIO.read(new ByteArrayInputStream(imgBuf));
                        } catch (Exception e) {
                            L.error(null, "read img error,img_name=" + img_name);
                        }
                        if (image == null)
                            continue;
                        long bw = image.getWidth();
                        long bh = image.getHeight();

                        L.debug(null, urlHead + src + ",the size is width=" + bw + ";and height=" + bh);
                        if (bw > 150 && bh > 100) {
                            L.debug(null, "save no:" + saveImgCount + ",old url=" + src);
                            long scale = linkImageScale;
                            long sw = 0;
                            long sh = 0;
                            if (bw > bh) {
                                sh = scale;
                                sw = (bw * scale) / bh;
                            } else {
                                sw = scale;
                                sh = (bh * scale) / bw;
                            }

                            L.debug(null, "img_name: " + img_name);
                            L.debug(null, "img_name_small: " + img_name_small);
//                            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name, String.valueOf(bw), String.valueOf(bh), StringUtils.substringAfterLast(img_name, "."));
                            //SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name_small, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(img_name_small, "."));
                            LinkImageHelper.saveLinkImage(linkSFS, imgBuf, img_name_small, (int)sw, (int)sh);

                            String imgUrl = StringUtils.isNotBlank(img_name_small) ? String.format(linkImgUrlPattern, img_name_small) : img_name_small;
                            L.debug(null, "save no:" + saveImgCount + ",new url=:" + imgUrl);
                            ja.add(imgUrl);
                            saveImgCount += 1;
                            if (saveImgCount >= IMGCOUNT)
                                break;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return ja;
    }

    //==================================tools function====================================

    public String getContext(String html) {
        Parser parser0 = new Parser();
        String str0 = "";
        try {
            //get content from body
            String str1[] = html.split("<body");
            if (str1.length > 1) {
                html = "<body" + str1[1];
            }

            parser0.setInputHTML(html);
            StringBean sb0 = new StringBean();
            sb0.setLinks(false);
            sb0.setReplaceNonBreakingSpaces(true);
            sb0.setCollapse(true);
            parser0.visitAllNodesWith(sb0);
            str0 = sb0.getStrings();
        } catch (ParserException e) {
            L.debug(null, "parser content error", e);
        }
        return str0;
    }

    public String getLongText(String inStr) {
        String outStr = "";
        String s001 = inStr.replaceAll("[a-zA-Z_]*", "");
        if ((int) (inStr.length() / s001.length()) <= 2) {    //chinese
            String s1[] = inStr.split(" ");
            for_one:
            for (String s0 : s1) {
                String s2[] = inStr.split("\n");
                for (String s00 : s2) {
                    int l = s00.length();
                    if (l > 50 && s00.replaceAll("|", "").length() > l - 3 && s00.replaceAll("-", "").length() > l - 3 && s00.replaceAll(" ", "").length() > l - 3) {
                        outStr = StringUtils.substring(s00, 0, 100);
                        break for_one;
                    }
                }
            }
        } else             //english
        {
            String s2[] = inStr.split("\n");
            for (String s00 : s2) {
                int l = s00.length();
                if (l > 50) {
                    outStr = StringUtils.substring(s00, 0, 100);
                    break;
                }
            }
            if (outStr.length() == 0)
                outStr = StringUtils.substring(inStr, 0, 100);
        }
        L.debug(null, "outStr=" + outStr);
        return outStr;
    }

    public static InputStream getUrlImage(String URLName) throws Exception {
        return getUrlImage(URLName, null ) ;
    }
    public static  InputStream getUrlImage(String URLName, StringBuilder retryUrl) throws Exception {
        int HttpResult = 0;
        try {
            URL url = new URL(URLName);
            URLConnection urlConn = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            HttpResult = httpConn.getResponseCode();
            if (HttpResult != HttpURLConnection.HTTP_OK) {
                return null;
            } else {

                if(retryUrl != null)
                    retryUrl.append(httpConn.getURL().toString());
                return new BufferedInputStream(urlConn.getInputStream());
            }
        } catch (Exception e) {
            L.error(null, e.toString(), e);
            return null;
        }
    }

    public boolean ifImgUrl(String imgUrl) {
        boolean b = false;
        try {
            URL ur = new URL(imgUrl);
            HttpURLConnection c = (HttpURLConnection) ur.openConnection();
            if (c.getContentType().contains("image/"))
                b = true;
            c.setConnectTimeout(10000);
            c.disconnect();
        } catch (Exception e) {
        }
        return b;
    }

    public String getUrlHead(String url) {
        String[] a = url.split("//");
        if (url.toUpperCase().startsWith("HTTP") || url.toUpperCase().startsWith("FTP") || url.toUpperCase().startsWith("HTTPS")) {
            return a[0] + "//";
        } else {
            return "http://";
        }
    }

    private static String getImageFileName(String filename) {
        if (filename.contains("=") || filename.contains("?") || filename.contains("&") || filename.contains("%")) {
            filename = filename.replace("?", "");
            filename = filename.replace("=", "");
            filename = filename.replace("&", "");
            filename = filename.replace("%", "");
        }

        return filename;
    }

    //
    private String newUrlFormat(String pageUrl, String relativeUrl) {
        //           "http://tech.sina.com.cn/t/m/2012-07-02/08077335412.shtml"
        //           "../../1.jpg"
        if (relativeUrl.startsWith("/"))
            relativeUrl = ".." + relativeUrl;
        String[] aa = pageUrl.split("/");
        int len1 = aa.length;
        String[] bb = relativeUrl.split("/");
        int temp = 0;
        for (String a : bb) {
            if (a.equals(".."))
                temp += 1;
        }
        int diff = len1 - 1 - temp;
        String p = "";
        for (int i = 0; i < diff; i++) {
            p += aa[i] + "/";
        }
        p = p.substring(0, p.length() - 1);
        for (String a : bb) {
            if (!a.equals("..")) {
                p += "/" + a;
            }
        }
        return p;
    }
}
