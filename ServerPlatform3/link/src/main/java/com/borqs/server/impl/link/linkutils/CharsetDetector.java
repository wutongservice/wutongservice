package com.borqs.server.impl.link.linkutils;


import com.borqs.server.platform.util.ObjectHolder;
import org.apache.commons.lang.StringUtils;
import org.mozilla.intl.chardet.HtmlCharsetDetector;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CharsetDetector {
    private static BufferedInputStream getBuffered(InputStream in) {
        return in instanceof BufferedInputStream ? (BufferedInputStream) in : new BufferedInputStream(in);
    }

    private static String trimGBK(String charset) {
        // TODO: fix gb2312 & big5 => GBK
        return StringUtils.equalsIgnoreCase(charset, "gb2312") || StringUtils.equalsIgnoreCase(charset, "big5") ? "GBK" : charset;
    }

    public static String guessCharset(InputStream in) {
        final String DEF = "GBK";
        final ObjectHolder<String> r = new ObjectHolder<String>(DEF);
        try {
            int lang = nsPSMDetector.ALL;
            nsDetector det = new nsDetector(lang);
            boolean found = false;

            det.Init(new nsICharsetDetectionObserver() {
                public void Notify(String charset) {
                    HtmlCharsetDetector.found = true;
                    r.value = trimGBK(charset);
                }
            });

            BufferedInputStream imp = getBuffered(in);

            byte[] buf = new byte[4196];
            int len;
            boolean done = false;
            boolean isAscii = true;

            while ((len = imp.read(buf, 0, buf.length)) != -1) {
                if (isAscii)
                    isAscii = det.isAscii(buf, len);
                if (!isAscii && !done)
                    done = det.DoIt(buf, len, false);
            }
            det.DataEnd();

            if (isAscii)
                found = true;

            if (!found) {
                String prob[] = det.getProbableCharsets();
                return prob != null && prob.length > 0 ? trimGBK(prob[0]) : DEF;
            } else {
                return r.value;
            }
        } catch (IOException e) {
            return DEF;
        }
    }

    public static String guessCharset(byte[] bytes) {
        return guessCharset(new ByteArrayInputStream(bytes));
    }
}
