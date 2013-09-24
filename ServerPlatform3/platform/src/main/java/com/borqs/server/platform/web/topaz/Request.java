package com.borqs.server.platform.web.topaz;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.feature.app.AppSign;
import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.web.UserAgent;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class Request {
    private static final int IN_MEMORY_FILE_SIZE_THRESHOLD = 1024 * 20; // 20KB
    private static final int MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 50; // 50MB

    private static final String TMP_DIR = getTempDir();

    private static String getTempDir() {
        try {
            String tempDir = FileUtils.getTempDirectoryPath() + "/upload_" + DateHelper.nowNano();
            FileUtils.forceMkdir(new File(tempDir));
            return tempDir;
        } catch (IOException e) {
            throw new ServerException(E.TOPAZ, e);
        }
    }


    public final HttpServletRequest httpRequest;
    private final Map<String, Object> params = new HashMap<String, Object>();

    public Request(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        fetchPostFiles();
    }

    public String getRawUserAgent() {
        return decodeHeader("User-Agent");
    }

    public UserAgent getUserAgent() {
        return UserAgent.parse(getRawUserAgent());
    }

    @SuppressWarnings("unchecked")
    private void fetchPostFiles() {
        try {
            if (ServletFileUpload.isMultipartContent(httpRequest)) {
                ServletFileUpload upload = createFileUpload();
                List<FileItem> fileItems = upload.parseRequest(httpRequest);
                for (FileItem fileItem : fileItems) {
                    if (fileItem.isFormField()) {
                        set(fileItem.getFieldName(), fileItem.getString(Charsets.DEFAULT));
                    } else {
                        params.put(fileItem.getFieldName(), fileItem);
                    }
                }
            }
        } catch (FileUploadException e) {
            throw new ServerException(E.TOPAZ, e);
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(E.TOPAZ, e);
        }
    }

    private static ServletFileUpload createFileUpload() {
        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory(IN_MEMORY_FILE_SIZE_THRESHOLD, new File(TMP_DIR)));
        upload.setSizeMax(MAX_UPLOAD_FILE_SIZE);
        return upload;
    }

    public boolean has(String key) {
        return params.containsKey(key) || httpRequest.getParameter(key) != null;
    }

    public String checkHeader(String headerKey) {
        String s = httpRequest.getHeader(headerKey);
        if (s == null)
            throw new ServerException(E.MISSING_HTTP_HEADER, "Missing header " + headerKey);
        return s;
    }

    public String getHeader(String headerKey, String def) {
        String s = httpRequest.getHeader(headerKey);
        return s != null ? s : def;
    }

    private static String decodeHeaderHelper(String s) {
        try {
            return URLDecoder.decode(s, Charsets.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(E.DECODE_HTTP_HEADER, "Decode http header '%s'", s);
        }
    }

    public String decodeHeader(String headerKey) {
        String s = getHeader(headerKey, "");
        return s.isEmpty() ? "" : decodeHeaderHelper(s);
    }

    public String checkDecodeHeader(String headerKey) {
        String s = checkHeader(headerKey);
        return s.isEmpty() ? "" : decodeHeaderHelper(s);
    }

    public Set<String> getHttpKeys(String... excepted) {
        HashSet<String> keys = new HashSet<String>();
        Enumeration e = httpRequest.getParameterNames();
        while (e.hasMoreElements()) {
            Object pn = e.nextElement();
            keys.add(ObjectUtils.toString(pn));
        }
        if (ArrayUtils.isNotEmpty(excepted))
            keys.removeAll(Arrays.asList(excepted));
        return keys;
    }

    public Set<String> getKeys(String... excepted) {
        Set<String> keys = getHttpKeys();
        keys.addAll(params.keySet());
        if (ArrayUtils.isNotEmpty(excepted))
            keys.removeAll(Arrays.asList(excepted));
        return keys;
    }

    public String[] getHttpKeysArray(String... excepted) {
        Set<String> keys = getHttpKeys(excepted);
        return keys.toArray(new String[keys.size()]);
    }

    public String[] getKeysArray(String... excepted) {
        Set<String> keys = getKeys(excepted);
        return keys.toArray(new String[keys.size()]);
    }

    public Map<String, String> getParams() {
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (String key : getKeys())
            params.put(key, checkString(key));

        return params;
    }


    public Request set(String key, Object o) {
        params.put(key, o);
        return this;
    }

    public Object get(String key, Object def) {
        Object o = params.get(key);
        if (o != null)
            return o;

        String v = httpRequest.getParameter(key);
        return v != null ? v : def;
    }

    public Object get(String prefKey, String secondKey, Object def) {
        Object v = get(prefKey, null);
        return v != null ? v : get(secondKey, def);
    }

    public String getString(String key, String def) {
        Object v = get(key, null);
        return v != null ? ObjectUtils.toString(v) : def;
    }

    public String getString(String prefKey, String secondKey, String def) {
        String v = getString(prefKey, null);
        return v != null ? v : getString(secondKey, def);
    }

    public String getString(String key, int maxLen, String def) {
        String s = getString(key, def);
        return checkStringLength(s, maxLen);
    }

    public String getString(String prefKey, String secondKey, int maxLen, String def) {
        String s = getString(prefKey, secondKey, def);
        return checkStringLength(s, maxLen);
    }

    public String getDebase64String(String key, String def) {
        String s = getString(key, null);
        return s != null ? Encoders.fromBase64String(s) : def;
    }

    public String getDebase64String(String prefKey, String secondKey, String def) {
        String v = getDebase64String(prefKey, null);
        return v != null ? v : getDebase64String(secondKey, def);
    }

    private static boolean parseBoolean(String s) {
        return s != null && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("y"));
    }

    public boolean getBoolean(String key, boolean def) {
        String s = getString(key, null);
        return s != null ? parseBoolean(s) : def;
    }

    public boolean getBoolean(String prefKey, String secondKey, boolean def) {
        String s = getString(prefKey, null);
        return s != null ? parseBoolean(s) : getBoolean(secondKey, def);
    }

    public int getInt(String key, int def) {
        String s = getString(key, null);
        return s != null ? Integer.parseInt(s) : def;
    }

    public int getInt(String prefKey, String secondKey, int def) {
        String s = getString(prefKey, null);
        return s != null ? Integer.parseInt(s) : getInt(secondKey, def);
    }

    public long getLong(String key, long def) {
        String s = getString(key, null);
        return s != null ? Long.parseLong(s) : def;
    }

    public long getLong(String prefKey, String secondKey, long def) {
        String s = getString(prefKey, null);
        return s != null ? Long.parseLong(s) : getLong(secondKey, def);
    }

    public int[] getIntArray(String key, String sep, int[] def) {
        String s = getString(key, null);
        return s != null ? StringHelper.splitIntArray(s, sep) : def;
    }

    public int[] getIntArray(String prefKey, String secondKey, String sep, int[] def) {
        String s = getString(prefKey, null);
        return s != null ? StringHelper.splitIntArray(s, sep) : getIntArray(secondKey, sep, def);
    }

    public long[] getLongArray(String key, String sep, long[] def) {
        String s = getString(key, null);
        return s != null ? StringHelper.splitLongArray(s, sep) : def;
    }

    public long[] getLongArray(String prefKey, String secondKey, String sep, long[] def) {
        String s = getString(prefKey, null);
        return s != null ? StringHelper.splitLongArray(s, sep) : getLongArray(secondKey, sep, def);
    }

    public String[] getStringArray(String key, String sep, String[] def) {
        String s = getString(key, null);
        return s != null ? StringHelper.splitArray(s, sep, true) : def;
    }

    public String[] getStringArray(String prefKey, String secondKey, String sep, String[] def) {
        String s = getString(prefKey, null);
        return s != null ? StringHelper.splitArray(s, sep, true) : getStringArray(secondKey, sep, def);
    }

    public String[] getStringArrayExcept(String key, String sep, String[] def, String[] except) {
        String[] a = getStringArray(key, sep, def);
        return ArrayUtils.isNotEmpty(except) ? CollectionsHelper.removeElements(a, except) : a;
    }

    public String[] getStringArrayExcept(String prefKey, String secondKey, String sep, String[] def, String[] excepted) {
        String[] a = getStringArray(prefKey, sep, null);
        if (a != null)
            return ArrayUtils.isNotEmpty(excepted) ? CollectionsHelper.removeElements(a, excepted) : a;
        else
            return getStringArrayExcept(secondKey, sep, def, excepted);
    }

    public Map<String, String> getMap(String keyPrefix, boolean removePrefix, Map<String, String> reuse) {
        if (reuse == null)
            reuse = new LinkedHashMap<String, String>();
        for (String key : getKeys()) {
            if (key.startsWith(keyPrefix)) {
                String v = checkString(key);
                if (removePrefix)
                    reuse.put(StringUtils.removeStart(key, keyPrefix), v);
                else
                    reuse.put(key, v);
            }
        }
        return reuse;
    }

    public FileItem getFile(String key) {
        Object v = params.get(key);
        return (v != null && v instanceof FileItem) ? (FileItem) v : null;
    }

    public FileItem getFile(String prefKey, String secondKey) {
        FileItem f = getFile(prefKey);
        return f != null ? f : getFile(secondKey);
    }

    private static String checkValue(String key, String v) {
        if (v == null)
            throw new ServerException(E.PARAM, "Missing parameter '%s'", key);
        return v;
    }

    public String checkString(String key) {
        return checkValue(key, getString(key, null));
    }

    public String checkString(String prefKey, String secondKey) {
        return checkValue(prefKey, getString(prefKey, secondKey, null));
    }

    private String checkStringLength(String s, int maxLen) {
        if (s.length() > maxLen)
            throw new ServerException(E.PARAM, "The parameter is too long");
        return s;
    }

    public String checkString(String key, int maxLen) {
        String s = checkString(key);
        return checkStringLength(s, maxLen);
    }

    public String checkString(String prefKey, String secondKey, int maxLen) {
        String s = checkString(prefKey, secondKey);
        return checkStringLength(s, maxLen);
    }

    public String checkDebase64String(String key) {
        return checkValue(key, getDebase64String(key, null));
    }

    public String checkDebase64String(String prefKey, String secondKey) {
        return checkValue(prefKey, getDebase64String(prefKey, secondKey, null));
    }

    public long checkLong(String key) {
        String v = checkString(key);
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", key);
        }
    }

    public long checkLong(String prefKey, String secondKey) {
        String v = checkValue(prefKey, getString(prefKey, secondKey, null));
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", prefKey);
        }
    }

    public boolean checkBoolean(String key) {
        String v = checkString(key);
        try {
            return parseBoolean(v);
        } catch (Exception e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", key);
        }
    }

    public boolean checkBoolean(String prefKey, String secondKey) {
        String v = checkString(prefKey, secondKey);
        try {
            return parseBoolean(v);
        } catch (Exception e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", prefKey);
        }
    }

    public int checkInt(String key) {
        String v = checkString(key);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", key);
        }
    }

    public int checkInt(String prefKey, String secondKey) {
        String v = checkString(prefKey, secondKey);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ServerException(E.PARAM, "Invalid parameter '%s'", prefKey);
        }
    }

    public int[] checkIntArray(String key, String sep) {
        String s = checkString(key);
        return StringHelper.splitIntArray(s, sep);
    }

    public int[] checkIntArray(String prefKey, String secondKey, String sep) {
        String s = checkString(prefKey, secondKey);
        return StringHelper.splitIntArray(s, sep);
    }

    public long[] checkLongArray(String key, String sep) {
        String s = checkString(key);
        return StringHelper.splitLongArray(s, sep);
    }

    public long[] checkLongArray(String prefKey, String secondKey, String sep) {
        String s = checkString(prefKey, secondKey);
        return StringHelper.splitLongArray(s, sep);
    }

    public String[] checkStringArray(String key, String sep) {
        String s = checkString(key);
        return StringHelper.splitArray(s, sep, true);
    }

    public String[] checkStringArray(String prefKey, String secondKey, String sep) {
        String s = checkString(prefKey, secondKey);
        return StringHelper.splitArray(s, sep, true);
    }

    public FileItem checkFile(String key) {
        FileItem f = getFile(key);
        if (f == null)
            throw new ServerException(E.TOPAZ, "Missing file upload parameter '%s'", key);
        return f;
    }

    public FileItem checkFile(String prefKey, String secondKey) {
        FileItem f = getFile(prefKey, secondKey);
        if (f == null)
            throw new ServerException(E.TOPAZ, "Missing file upload parameter '%s'", prefKey);
        return f;
    }

    public void deleteUploadedFiles() {
        for (Object v : params.values()) {
            if (v instanceof FileItem)
                ((FileItem) v).delete();
        }
    }

    public String getBody() {
        Reader reader = null;
        try {
            reader = httpRequest.getReader();
            return IOUtils.toString(reader);
        } catch(IOException e) {
            throw new ServerException(E.WEB_SERVER, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public Page getPage(String pageParamName, String countParamName, int defCount, int maxCount) {
        int page = getInt(pageParamName, 0);
        int count = getInt(countParamName, defCount);
        if (count > maxCount)
            count = maxCount;

        if(page == -1 || count == -1){
            return new Page(0, 1000);
        }
        else
            return new Page(page,count);
    }

    public Page getPage(int defCount, int maxCount) {
        return getPage("page", "count", defCount, maxCount);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(httpRequest.getMethod()).append(" ").append(httpRequest.getRequestURL());
        Map<String, String> params = getParams();
        if (!params.isEmpty()) {
            buff.append(" params:");
            for (Map.Entry<String, String> e : params.entrySet()) {
                Object v = e.getValue();
                buff.append(e.getKey()).append("=\'").append(StringEscapeUtils.escapeJavaScript(ObjectUtils.toString(v))).append("\' ; ");
            }
        }
        return buff.toString();
    }
}
