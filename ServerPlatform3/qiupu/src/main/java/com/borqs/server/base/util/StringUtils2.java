package com.borqs.server.base.util;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StringUtils2 {

    public static String[] splitArray(String s, String sep, boolean strip) {
        List<String> l = splitList(s, sep, strip);
        return l.toArray(new String[l.size()]);
    }
    
    public static String[] splitArray(String s, String sep, int len, boolean strip) {
    	String[] arr0 = new String[len];
    	for(int i = 0; i < len; i++)
    	{
    		arr0[i] = "";
    	}
    	
    	String[] arr1 = s.split(sep);
    	
    	for(int i = 0; i < arr1.length; i++)
    	{
    		if(strip)
    			arr1[i] = StringUtils.strip(arr1[i]);
    		
    		arr0[i] = arr1[i];
    	}
    	
    	return arr0;
    }

    public static List<String> splitList(String s, String sep, boolean strip) {
        ArrayList<String> l = new ArrayList<String>();
        for (String e : StringUtils.split(s, sep))  {
            String e0 = strip ? StringUtils.strip(e) : e;
            if (!StringUtils.isEmpty(e0))
                l.add(e0);
        }
        return l;
    }

    public static Set<String> splitSet(String s, String sep, boolean strip) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String e : StringUtils.split(s, sep))  {
            String e0 = strip ? StringUtils.strip(e) : e;
            if (!StringUtils.isEmpty(e0))
                set.add(e0);
        }
        return set;
    }

    public static List<Long> splitIntList(String s, String sep) {
        ArrayList<Long> l = new ArrayList<Long>();
        for (String e : StringUtils.split(s, sep)) {
            String e0 = StringUtils.strip(e);
            if (!StringUtils.isEmpty(e0))
                l.add(Long.parseLong(e0));
        }
        return l;
    }

    public static long[] splitIntArray(String s, String sep) {
        List<Long> l = splitIntList(s, sep);
        Long[] a = l.toArray(new Long[l.size()]);
        return ArrayUtils.toPrimitive(a);
    }

    public static String stripItems(String items, String sep, boolean strip) {
        return StringUtils.join(splitSet(items, sep, strip), sep);
    }

    public static String joinIgnoreNull(Object... objs) {
        StringBuilder buff = new StringBuilder();
        for (Object obj : objs) {
            if (obj != null)
                buff.append(obj.toString());
        }
        return buff.toString();
    }

    public static String join(String sep, Object... objs) {
        return StringUtils.join(objs, sep);
    }

    public static String joinComma(Object[] a) {
        return StringUtils.join(a, ",");
    }
    public static String joinComma(Collection c) {
        return StringUtils.join(c, ",");
    }
    public static String joinComma(long[] a) {
        return join(a, ",");
    }

     public static String join(long[] a, String sep) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                buff.append(sep);
            buff.append(a[i]);
        }
        return buff.toString();
    }
    public static String joinIgnoreBlank(String sep, Object... objs) {
        return joinIgnoreBlank(sep, Arrays.asList(objs));
    }

    public static String joinIgnoreBlank(String sep, Collection<?> c) {
        StringBuilder buff = new StringBuilder();
        int i = 0;
        for (Object obj : c) {
            if (StringUtils.isNotBlank(ObjectUtils.toString(obj, null))) {
                if (i > 0)
                    buff.append(sep);

                buff.append(obj.toString());
                i++;
            }
        }
        return buff.toString();
    }

    public static Map trandsUserName(String inStr) {
        Map name_map = new HashMap();
        if (ifChineseName(inStr)) {   // has  chinese code
            List<String> l = StringUtils2.splitList(inStr, " ", true);
            if (l.size() > 1) {
                name_map = splitStrHasBlank(inStr);
            } else {
                String doubleLastName = hasDoubleLastName(inStr);
                int len = inStr.length();
                if (doubleLastName.equals("")) {
                    if (len == 1) {
                        name_map.put("first_name", inStr);
                    } else if (len == 2) {
                        name_map.put("first_name", inStr.substring(1, 2));
                        name_map.put("last_name", inStr.substring(0, 1));
                    } else if (len == 3) {
                        name_map.put("first_name", inStr.substring(2, 3));
                        name_map.put("middle_name", inStr.substring(1, 2));
                        name_map.put("last_name", inStr.substring(0, 1));
                    } else {
                        name_map.put("first_name", inStr.substring(2, len));
                        name_map.put("middle_name", inStr.substring(1, 2));
                        name_map.put("last_name", inStr.substring(0, 1));
                    }
                } else {
                    if (len == 2) {
                        name_map.put("first_name", inStr);
                    } else if (len == 3) {
                        name_map.put("first_name", inStr.substring(2, 3));
                        name_map.put("last_name", inStr.substring(0, 2));
                    } else if (len == 4) {
                        name_map.put("first_name", inStr.substring(3, 4));
                        name_map.put("middle_name", inStr.substring(2, 3));
                        name_map.put("last_name", inStr.substring(0, 2));
                    } else {
                        name_map.put("first_name", inStr.substring(3, len));
                        name_map.put("middle_name", inStr.substring(2, 3));
                        name_map.put("last_name", inStr.substring(0, 2));
                    }
                }
            }
        } else {                      //no has chinese code
            name_map = splitStrHasBlank(inStr);
        }

        return name_map;
    }

    public static Map splitStrHasBlank(String inStr) {
        Map name_map = new HashMap();
        List<String> l = StringUtils2.splitList(inStr, " ", true);
        if (l.size() == 1) {
            name_map.put("first_name", l.get(0));
        } else if (l.size() == 2) {
            name_map.put("first_name", l.get(0));
            name_map.put("last_name", l.get(1));
        } else if (l.size() == 3) {
            name_map.put("first_name", l.get(0));
            name_map.put("middle_name", l.get(1));
            name_map.put("last_name", l.get(2));
        } else {
            String a = "";
            for (int i = 2; i < l.size(); i++) {
                a += l.get(i).toString() + " ";
            }
            name_map.put("first_name", l.get(0));
            name_map.put("middle_name", l.get(1));
            name_map.put("last_name", a.trim());
        }
        return name_map;
    }

    public static boolean ifChineseName(String inStr) {
//        Pattern p = Pattern.compile("[\\u4E00-\\u9FA5]+ ");
//        Matcher m = p.matcher(inStr);
//        int count=0;
//        boolean result = m.find();
        return inStr.getBytes().length==inStr.length()?false:true;
    }

    public static String hasDoubleLastName(String inStr) {
        String regx = "宇文、尉迟、延陵、羊舌、羊角、乐正、诸葛、颛孙、仲孙、仲长、长孙、钟离、宗政、左丘、主父、" +
                "宰父、子书、子车、子桑、百里、北堂、北野、哥舒、谷梁、闻人、王孙、王官、王叔、巫马、微生、淳于、" +
                "单于、成公、叱干、叱利、褚师、端木、东方、东郭、东宫、东野、东里、东门、第二、第五、公祖、公玉、" +
                "公西、公孟、公伯、公仲、公孙、公广、公上、公冶、公羊、公良、公户、公仪、公山、公门、公坚、公乘、" +
                "欧阳、濮阳、青阳、漆雕、壤驷、上官、司徒、司马、司空、司寇、士孙、申屠、叔孙、叔仲、侍其、令狐、" +
                "梁丘、闾丘、刘傅、慕容、万俟、谷利、高堂、南宫、南门、南荣、南野、女娲、纳兰、澹台、拓跋、太史、" +
                "太叔、太公、秃发、夏侯、西门、鲜于、轩辕、相里、皇甫、赫连、呼延、胡母、亓官、夹谷、即墨、独孤、" +
                "段干、达奚";
        String doubleLastName = "";
        List<String> l = StringUtils2.splitList(regx, "、", true);
        for (String l0 : l) {
            if (inStr.contains(l0)) {
                if (inStr.startsWith(l0)) {
                    doubleLastName = l0;
                    break;
                }
            }
        }

        return doubleLastName;
    }

    public static String join(String sep, long[] a) {
        StringBuilder buff = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0)
                buff.append(sep);
            buff.append(a[i]);
        }
        return buff.toString();
    }

    public static String compress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        return out.toString("ISO-8859-1");
    }

    public static String uncompress(String str) throws IOException {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
        GZIPInputStream gunzip = new GZIPInputStream(in);
        byte[] buffer = new byte[1024];
        int n;
        while ((n = gunzip.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        return out.toString();
    }

    public static String removeStartAndEnd(String s, String start, String end) {
        s = StringUtils.removeStart(s, start);
        return StringUtils.removeEnd(s, end);
    }
}
