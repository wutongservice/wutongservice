package com.borqs.server.base.util.image;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageUtils {

    private static int calcLength(int org, String len) {
        len = StringUtils.trimToNull(len);
        if (len == null)
            return org;

        if (len.endsWith("%")) {
            return (int)((org * Integer.parseInt(StringUtils.removeEnd(len, "%")) / 100.0));
        } else {
            return Integer.parseInt(len);
        }
    }

    public static BufferedImage scale(BufferedImage image, String width, String height) {
        Validate.notNull(image);

        int dstWidth = calcLength(image.getWidth(), width);
        int dstHeight = calcLength(image.getHeight(), height);

        AffineTransform transform = new AffineTransform();
        double widthScale = (double)dstWidth / image.getWidth();
        double heightScale = (double)dstHeight / image.getHeight();
        transform.setToScale(widthScale, heightScale);

        BufferedImage dst = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = null;
        try {
            g2 = dst.createGraphics();
            g2.drawImage(image, transform, null);
            return dst;
        } finally {
            if (g2 != null)
                g2.dispose();
        }
    }

    public static void scale(InputStream in, OutputStream out, String width, String height, String format) throws IOException {
        Validate.notNull(in);
        Validate.notNull(out);
        Validate.notNull(format);
        try {
            BufferedImage img = scale(ImageIO.read(in), width, height);
            ImageIO.write(img, format, out);
        } catch (IOException e) {
            throw new ImageException(e);
        } finally {
            in.close();
            out.flush();
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static byte[] scale(byte[] in, String width, String height, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scale(new ByteArrayInputStream(in), out, width, height, format);
        return out.toByteArray();
    }

    public static void scale(File in, File out, String width, String height, String format) {
        FileInputStream in2 = null;
        FileOutputStream out2 = null;
        try {
            in2 = new FileInputStream(in);
            out2 = new FileOutputStream(out);
            scale(in2, out2, width, height, format);
        } catch (IOException e) {
            throw new ImageException(e);
        } finally {
            IOUtils.closeQuietly(out2);
            IOUtils.closeQuietly(in2);
        }
    }
}
