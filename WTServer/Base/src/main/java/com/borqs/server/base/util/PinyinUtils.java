package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;

public class PinyinUtils {
    private static final HanyuPinyinOutputFormat FULL_PINYIN_FORMAT;

    static {
        FULL_PINYIN_FORMAT = new HanyuPinyinOutputFormat();
        FULL_PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FULL_PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    public static String toFullPinyin(String text) {
        return toFullPinyin(text, "");
    }

    public static String toFullPinyin(String text, String sep) {
        String[] a = toFullPinyinArray(text);
        return StringUtils.join(a, sep);
    }

    public static String[] toFullPinyinArray(String text) {
        String[] ss = ChineseSegmentUtils.segmentArray(text, ChineseSegmentUtils.Options.create(0));
        try {
            ArrayList<String> l = new ArrayList<String>();
            for (String s : ss) {
                if (StringUtils2.isChineseWord(s)) {
                    for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        String[] pyArray = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, FULL_PINYIN_FORMAT);
                        if (ArrayUtils.isNotEmpty(pyArray))
                            l.add(pyArray[0]);
                    }
                } else if (StringUtils2.isEnglishWord(s)) {
                    l.add(s);
                }
            }
            return l.toArray(new String[l.size()]);
        } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
            throw new ServerException(BaseErrors.PLATFORM_PINYIN);
        }
    }

    public static String toShortPinyin(String text) {
        String[] a = toFullPinyinArray(text);
        StringBuilder buff = new StringBuilder();
        for (String s : a) {
            if (StringUtils.isNotEmpty(s))
                buff.append(s.charAt(0));
        }
        return buff.toString();
    }
}
