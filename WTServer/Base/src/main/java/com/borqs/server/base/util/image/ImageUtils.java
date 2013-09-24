package com.borqs.server.base.util.image;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
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
//            try {
//                //加水印
//                float alpha = 8f;
//                int x = 0;
//                int y = 0;
//                int fontsize = 25;
//                String pressText = "梧桐树";
//                Graphics2D g = img.createGraphics();
//                g.setFont(new Font("微软雅黑", Font.BOLD, fontsize));
//                g.setColor(Color.green);
////            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));
//
//                int width_1 = fontsize * getLength(pressText);
//                int height_1 = fontsize;
//                int widthDiff = Integer.valueOf(img.getWidth()) - width_1;
//                int heightDiff = Integer.valueOf(img.getHeight()) - height_1;
//                if (x < 0) {
//                    x = widthDiff / 2;
//                } else if (x > widthDiff) {
//                    x = widthDiff;
//                }
//                if (y < 0) {
//                    y = heightDiff / 2;
//                } else if (y > heightDiff) {
//                    y = heightDiff;
//                }
//
//                g.drawString(pressText, widthDiff - 20, heightDiff);
//
//                String file_addr = "/home/wutong/work2/icon.gif";
//                File file =new File(file_addr) ;
//                if (!file.exists())
//                    file_addr = "/home/zhengwei/work2/icon.gif";
////            String file_addr = "D:\\icon.gif";
//                Image waterImage = ImageIO.read(new File(file_addr));    // 水印文件
//                int width_10 = waterImage.getWidth(null);
//                int height_10 = waterImage.getHeight(null);
//                g.drawImage(waterImage, widthDiff - 20 - width_10 - 10, heightDiff - 25, width_10, height_10, null);
//                g.dispose();
//            } catch (Exception e) {
//            }
            ImageIO.write(img, format, out);
        } catch (IOException e) {
            throw new ServerException(BaseErrors.PLATFORM_IMAGE_PROCESS_ERROR, e);
        } finally {
            in.close();
            out.flush();
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static int getLength(String text) {
        int textLength = text.length();
        int length = textLength;
        for (int i = 0; i < textLength; i++) {
            if (String.valueOf(text.charAt(i)).getBytes().length > 1) {
                length++;
            }
        }
        return (length % 2 == 0) ? length / 2 : length / 2 + 1;
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
            throw new ServerException(BaseErrors.PLATFORM_IMAGE_PROCESS_ERROR, e);
        } finally {
            IOUtils.closeQuietly(out2);
            IOUtils.closeQuietly(in2);
        }
    }
}
