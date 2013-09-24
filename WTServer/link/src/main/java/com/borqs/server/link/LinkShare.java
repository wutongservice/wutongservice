package com.borqs.server.link;

import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.mq.MQCollection;
import com.borqs.server.base.sfs.SFSUtils;
import com.borqs.server.base.sfs.StaticFileStorage;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.CharsetDetector;
import com.borqs.server.base.util.ClassUtils2;
import com.borqs.server.base.util.Encoders;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.wutong.link.LinkCacheImpl;
import net.sf.json.JSONArray;
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

public class LinkShare {
    private StaticFileStorage linkStorage;
    private static final Logger L = Logger.getLogger(LinkShare.class);
    private static final int IMGCOUNT = 5;
    Configuration conf;


    public LinkShare(Configuration conf) {
        this.conf = conf;
        linkStorage = (StaticFileStorage) ClassUtils2.newInstance(conf.getString("platform.servlet.linkImgStorage", "{\"class\":\"com.borqs.server.base.sfs.local.LocalSFS\", \"args\":{\"dir\":\"/home/zhengwei/data/link\"}}"));
        linkStorage.init();
    }

    //==================================only share image====================================
    public RecordSet onlyShareImageLink(Context ctx, String url) throws Exception {
        Configuration conf = GlobalConfig.get();
        RecordSet out_recs = new RecordSet();
        Record out_rec = new Record();
        out_rec.put("url", url);
        URL ur = null;

        try {
            ur = new URL(url);
        } catch (MalformedURLException e) {
            L.warn(ctx,e,"illegal url");
        }

        String host = ur.getHost();
        out_rec.put("host", host);
        out_rec.put("title", "");
        out_rec.put("description", "share image");

        JSONArray ja = new JSONArray();
        ja.clear();

        String img_name = formatSavedImageNewName(url);
        InputStream input = getUrlImage(ctx,url);

        if (input != null) {
            L.debug(ctx,"save img success");

            //compitable with oss
            String img_name_small = img_name.replace("big", "small");
            byte[] imgBuf = IOUtils.toByteArray(input);
            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(imgBuf));
            } catch (Exception e) {
                L.error(ctx,e,"read img error,img_name=" + img_name);

                //in this case, all image is ok, but fail to process in ImageIO, may caused by watermarker
                //save the original as the showing image
            }

            long sw = 0;
            long sh = 0;
            if (image != null) {
                long bw = image.getWidth();
                long bh = image.getHeight();
                long scale = conf.getInt("platform.linkImgScale", 360);

                if (bw < scale || bh < scale) {
                    sw = bw;
                    sh = bh;
                } else {
                    if (bw > bh) {
                        sh = scale;
                        sw = (bw * scale) / bh;
                    } else {
                        sw = scale;
                        sh = (bh * scale) / bw;
                    }
                }
            }
            if (linkStorage instanceof OssSFS)
                img_name_small = "media/link/" + img_name_small;
            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name_small, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(img_name_small, "."));
            String imgUrl = StringUtils.isNotBlank(img_name_small) ? String.format(conf.checkGetString("platform.linkImgUrlPattern"), img_name_small) : img_name_small;
            L.debug(ctx,"make img success,new url=:" + imgUrl);
            ja.add(imgUrl);
        }

        out_rec.put("many_img_url", ja.toString());
        if (ja.size() > 0) {
            out_rec.put("img_url", ja.get(0).toString());
        } else {
            out_rec.put("img_url", "");
        }
        out_recs.add(out_rec);
        L.debug(ctx,"share image link success RecordSet=" + out_recs);
        return out_recs;
    }

    //==================================only share image end ====================================

    final static int retryIntervalSeconds = 2;
    final static int retry_count = 2;




    //=================================method in =====================================
    //=================================method in =====================================
    public RecordSet shareLinkTaskIn(Context ctx, String url) throws Exception {
        RecordSet out_recs = new RecordSet();
        String newURL = url;

        int retriedCount = 0;
        for (retriedCount = 0; retriedCount < retry_count; retriedCount++) {
            try {
                URL ur = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) ur.openConnection();
                if (conn.getResponseCode() != 200 ||
                        retriedCount == retry_count) {
                    L.debug(ctx, "url:" + url + " can not allow to read!");
                    return out_recs;
                }
                if (!conn.getURL().toString().trim().equalsIgnoreCase(url.trim()))
                    newURL = conn.getURL().toString().trim();
            } catch (MalformedURLException e) {
                L.error(ctx,e,"url illegal");
            } catch (Exception ne) {
                if (retriedCount == retry_count) {
                    throw ne;
                } else {
                    try {
                        L.info(ctx,"Sleeping " + retryIntervalSeconds + " seconds until the next retry. url=" + url);
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
                    out_recs = onlyShareImageLink(ctx, newURL);
                } else {
                    out_recs = onlyShareHtmlPage(ctx, newURL);
                }
            } catch (Exception ne) {
                fetch_ret = false;
                if (retriedCount == retry_count) {
                    throw ne;
                }
                try {
                    L.info(ctx,"Sleeping " + retryIntervalSeconds + " seconds until the next retry.");
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
    //=================================method in =====================================
    //=================================method in =====================================



    //==================================only share html====================================

    public RecordSet onlyShareHtmlPage(Context ctx, String url) throws Exception {
        RecordSet out_recs = new RecordSet();
        Record out_rec = new Record();
        out_rec.put("url", url);

        URL ur = null;
        try {
            ur = new URL(url);
        } catch (MalformedURLException e) {
            L.error(ctx,e,"url illegal");
        }
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
        //============get all buffer===================
        if (result.isEmpty())
            L.debug(ctx, "this page is not allow to read!");

        String regex = "<script[\\s\\S]*?</script>|<!--[\\s\\S]*?-->|<style[\\s\\S]*?</style>|<![\\s\\S]*?>";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher match = pattern.matcher(result);
        result = match.replaceAll("");
        //============delete tag of <script> and <style>===================
        Parser parser = Parser.createParser(result, charset);
        HtmlPage pageText = new HtmlPage(parser);

        parser.reset();
        parser.visitAllNodesWith(pageText);

        //============visit all node===================
        out_rec.put("title", pageText.getTitle());
        if (out_rec.getString("description").length() == 0) {
            //==========get long text in body=====================
            String backStr = getLongText(ctx,getContext(ctx, result));
            out_rec.put("description", StringUtils.substring(backStr, 0, 100));
        }


        //=================get images======================
        JSONArray ja = new JSONArray();
        ja.clear();
        ja = getImageFromHtmlFromBody(ctx, url, getUrlHead(url) + host, pageText);
        out_rec.put("many_img_url", ja);
        if (ja.size() > 0) {
            out_rec.put("img_url", ja.get(0).toString());
        } else {
            out_rec.put("img_url", "");
        }

        out_recs.add(out_rec);
        parser.reset();
        //=======================share link end========================
        L.debug(ctx,"url content get success, return RecordSet=" + out_recs);
        return out_recs;
    }


    public static String formatSavedImageNewName(String URLName) throws Exception {
        URL url = new URL(URLName);

        String path = new File(url.getPath()).getName();
        String extendName = path;
        int lastPos = path.lastIndexOf(".");
        if (lastPos > 0) {
            extendName = getImageFileName(path.substring(lastPos, path.length()));
        }

        String imgName = Long.toString(RandomUtils.generateId()) + "_big" + extendName;
        return imgName;
    }

    public JSONArray getImageFromHtmlFromBody(Context ctx, String url, String urlHead, HtmlPage page) throws Exception {
        NodeList nodeList = page.getBody();
        TagNameFilter imgFilter = new TagNameFilter("IMG");
        NodeList imgNodeList = nodeList.extractAllNodesThatMatch(imgFilter, true);

        List<String> img_ll = new ArrayList<String>();
        for (int i = 0; i < imgNodeList.size(); i++) {
            ImageTag imgLink = (ImageTag) imgNodeList.elementAt(i);
            String src = imgLink.getImageURL();
            if (!img_ll.contains(src))
                img_ll.add(src);
        }

        Pattern pat = Pattern.compile("(http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\!\\.,@?^=%&amp;:/~\\+#]*[\\w\\-\\!\\@?^=%&amp;/~\\+#])?", Pattern.CASE_INSENSITIVE);
        JSONArray ja = new JSONArray();
        ja.clear();

        if (img_ll.size() > 0) {
            int saveImgCount = 0;
            LinkCacheImpl linkCache = null;
            try{
                linkCache = new LinkCacheImpl();
                linkCache.init();
            } catch (Exception e){
                L.debug(ctx, "linkCache init error");
            }

            for (int i = 0; i < img_ll.size(); i++) {
                String src = img_ll.get(i);
                String o_src = src;
                boolean has = false;
                if (linkCache !=null)
                    has = linkCache.hasImgInHost(ctx, urlHead, o_src);
                if (!has) {
                    InputStream input = null;
                    String img_name = "";
                    Matcher matcher = pat.matcher(src);
                    if (matcher.find()) {
                        L.debug(ctx, "matcher find and get image url=" + matcher.group());
                    } else {
                        src = newUrlFormat(url, src);
                    }
                    img_name = formatSavedImageNewName(src);
                    StringBuilder retryUrl = new StringBuilder();
                    input = getUrlImage(ctx, src, retryUrl);
                    if (retryUrl.length() > 0) {
                        img_name = formatSavedImageNewName(retryUrl.toString());
                    }

                    if (input != null) {
                        String img_name_small = img_name.replace("big", "small");
                        byte[] imgBuf = IOUtils.toByteArray(input);
                        BufferedImage image = null;
                        try {
                            image = ImageIO.read(new ByteArrayInputStream(imgBuf));
                        } catch (Exception e) {
                            L.error(ctx, e, "read img error,img_name=" + img_name);
                        }
                        if (image == null)
                            continue;
                        long bw = image.getWidth();
                        long bh = image.getHeight();

                        L.debug(ctx, "get image success,url =" + urlHead + src + ",the size is width=" + bw + ";and height=" + bh);
                        if (bw > 150 && bh > 100) {
                            L.debug(ctx, "start save image," + "save no:" + saveImgCount + ",old url=" + src);
                            long scale = GlobalConfig.get().getInt("platform.linkImgScale", 360);
                            long sw = 0;
                            long sh = 0;

                            if (bw < scale || bh < scale) {
                                sw = bw;
                                sh = bh;
                            } else {
                                if (bw > bh) {
                                    sh = scale;
                                    sw = (bw * scale) / bh;
                                } else {
                                    sw = scale;
                                    sh = (bh * scale) / bw;
                                }
                            }

                            if (linkStorage instanceof OssSFS)
                                img_name_small = "media/link/" + img_name_small;
                            SFSUtils.saveScaledImage(new ByteArrayInputStream(imgBuf), linkStorage, img_name_small, String.valueOf(sw), String.valueOf(sh), StringUtils.substringAfterLast(img_name_small, "."));

                            String imgUrl = StringUtils.isNotBlank(img_name_small) ? String.format(GlobalConfig.get().checkGetString("platform.linkImgUrlPattern"), img_name_small) : img_name_small;
                            L.debug(ctx, "save no:" + saveImgCount + " success,new url=:" + imgUrl);
                            ja.add(imgUrl);
                            saveImgCount += 1;
                            try{
                                linkCache.saveLinkCache(ctx,urlHead,o_src);
                            }catch (Exception e){
                                L.debug(ctx, "save link src in db error!");
                            }

                            if (saveImgCount >= IMGCOUNT)
                                break;
                        }
                    } else {
                        L.debug(ctx, "get input stream method 'getUrlImage' error");
                        continue;
                    }
                }
            }
        }
        return ja;
    }

    //==================================tools function====================================

    public String getContext(Context ctx, String html) {
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
            L.debug(ctx, "parser content error,content is=" + html);
        }
        return str0;
    }

    public String getLongText(Context ctx,String inStr) {
        String outStr = "";
        String s001 = inStr.replaceAll("[a-zA-Z_]*", "");
        if ((int) (inStr.length() / s001.length()) <= 2) {    //chinese
            String s1[] = inStr.split(" ");
            for_one:
            for (String s0 : s1) {
                String s2[] = inStr.split("\n");
                for (String s00 : s2) {
                    int l = s00.length();
                    if (l > 50 && StringUtils.replace(s00,"|","").length() > l - 3 &&  StringUtils.replace(s00,"-","").length() > l - 3) {
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
        L.debug(ctx,"find long text out Str=" + outStr);
        return outStr;
    }

    public static InputStream getUrlImage(Context ctx,String URLName) throws Exception {
        return getUrlImage(ctx,URLName, null ) ;
    }
    public static  InputStream getUrlImage(Context ctx, String URLName, StringBuilder retryUrl) throws Exception {
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
            L.error(ctx,e,"method 'InputStream getUrlImage' error,can't open connection");
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
        if (len1==3 && !pageUrl.endsWith("/"))
            pageUrl+="/";
        String[] bb = relativeUrl.split("/");
        int temp = 0;
        for (String a : bb) {
            if (a.equals(".."))
                temp += 1;
        }
        int diff = 0;
        if (len1 == 3) {
            diff = len1 - temp;
        } else {
            diff = len1 - 1 - temp;
        }
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
