package com.borqs.server.base.util;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ChineseSegmentUtils {
    private static IKSegmenter createSegmenter(Reader in) {
        return new IKSegmenter(in, true);
    }

    public static String[] segmentArray(String text, Options opts) {
        StringReader in = new StringReader(text);
        boolean hasStopWords = CollectionUtils.isNotEmpty(opts.stopWords);
        boolean hasMinEnglishWordLength = opts.minEnglishWordLength > 0;
        try {
            IKSegmenter seg = createSegmenter(in);
            ArrayList<String> l = new ArrayList<String>();
            for (;;) {
                Lexeme word = seg.next();
                if (word == null)
                    break;

                String s = word.getLexemeText();
                boolean b = true;
                if (hasStopWords) {
                    if (opts.stopWords.contains(s))
                        b = false;
                }
                if (hasMinEnglishWordLength) {
                    if (StringUtils2.isEnglishWord(s)&& s.length() < opts.minEnglishWordLength)
                        b = false;
                }
                if (b)
                    l.add(s);
            }

            return l.toArray(new String[l.size()]);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_CHSEG);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static String segmentString(String text, String sep, Options opts) {
        String[] a = segmentArray(text, opts);
        return StringUtils.join(a, sep);
    }

    public static String segmentString(String text, Options opts) {
        return segmentString(text, " ", opts);
    }

    public static String[] segmentArray(String text) {
        return segmentArray(text, Options.DEFAULT_OPTIONS);
    }

    public static String segmentString(String text, String sep) {
        return segmentString(text, sep, Options.DEFAULT_OPTIONS);
    }

    public static String segmentString(String text) {
        return segmentString(text, " ", Options.DEFAULT_OPTIONS);
    }

    public static String[] segmentNameArray(String text) {
        ArrayList<String> l = new ArrayList<String>();
        String[] a = segmentArray(text, Options.create((Set<String>)null, 3));
        for (String s : a) {
            if (StringUtils2.isEnglishWord(s)) {
                l.add(s);
            } else {
                for (int i = 0; i < s.length(); i++)
                    l.add(s.substring(i, i + 1));
            }
        }
        return l.toArray(new String[l.size()]);
    }

    public static String segmentNameString(String text, String sep) {
        String[] a = segmentNameArray(text);
        return StringUtils.join(a, sep);
    }

    public static String segmentNameString(String text) {
        return segmentNameString(text, " ");
    }

    public static class Options {
        public static final Options DEFAULT_OPTIONS = new Options(null, 0);

        public final Set<String> stopWords;
        public final int minEnglishWordLength;


        private Options(Set<String> stopWords, int minEnglishWordLength) {
            this.stopWords = stopWords;
            this.minEnglishWordLength = minEnglishWordLength;
        }

        public static Options create(int minEnglishWordLength) {
            return new Options(null, minEnglishWordLength);
        }

        public static Options create(Set<String> stopWords, int minEnglishWordLength) {
            return new Options(stopWords, minEnglishWordLength);
        }

        public static Options create(String stopWords, int minEnglishWordLength) {
            String[] ss = StringUtils.split(stopWords, "\r\n ");
            HashSet<String> set = new HashSet<String>();
            for (String s : ss) {
                if (!s.isEmpty())
                    set.add(s);
            }
            return new Options(set, minEnglishWordLength);
        }
    }
}
