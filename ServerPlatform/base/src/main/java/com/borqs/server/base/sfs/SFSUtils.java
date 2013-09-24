package com.borqs.server.base.sfs;


import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.image.ImageMagickHelper;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.io.IOException2;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.SystemHelper;
import com.borqs.server.base.util.image.ImageUtils;
import com.borqs.server.base.web.WebUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class SFSUtils {
    public static final String TEMP_PHOTO_IMAGE_DIR = SystemHelper.getPathInTempDir("temp_photo_image");
    private static final Logger L = LoggerFactory.getLogger(SFSUtils.class);
    public static void saveToOSS(OssSFS sfs, String file, long contentLength, InputStream is) throws Exception {
        String suffix = StringUtils.substringAfterLast(file, ".");
        L.debug("===============OSS INFO=========is.length="+ is.toString().length() );
        OSSClient client = sfs.getOSSClient();
        ObjectMetadata objectMeta = new ObjectMetadata();
        objectMeta.setContentLength(contentLength);
        if (StringUtils.equalsIgnoreCase(suffix, "png"))
            objectMeta.setContentType("image/png");
        else if (StringUtils.equalsIgnoreCase(suffix, "jpg")
                || StringUtils.equalsIgnoreCase(suffix, "jpeg"))
            objectMeta.setContentType("image/jpeg");
        else if (StringUtils.equalsIgnoreCase(suffix, "gif"))
            objectMeta.setContentType("image/gif");
        else if (StringUtils.equalsIgnoreCase(suffix, "tif")
                || StringUtils.equalsIgnoreCase(suffix, "tiff"))
            objectMeta.setContentType("image/tiff");

        L.debug("===============OSS INFO=========contentLength"+contentLength + "ContentType="+objectMeta.getContentType() + "BucketName="+sfs.getBucketName() + "length="+file.length());
        client.putObject(sfs.getBucketName(), file, is, objectMeta);
        is.close();
    }

    public static void deleteObjectFromOSS(StaticFileStorage sfs, String key) throws Exception {
        Validate.notNull(sfs);
        deleteFromOSS((OssSFS) sfs, key);
    }

    private static void deleteFromOSS(OssSFS sfs, String key) throws Exception {
        OSSClient client = sfs.getOSSClient();
        client.deleteObject(sfs.getBucketName(), key);
    }

    public static void saveUpload(FileItem fileItem, StaticFileStorage sfs, String file) {
        Validate.notNull(sfs);
        Validate.notNull(fileItem);

        if (sfs instanceof OssSFS) {
            try {
                L.debug("===============OSS INFO=========fileItem.content_type="+ fileItem.getContentType() );
                saveToOSS((OssSFS) sfs, file, fileItem.getSize(), fileItem.getInputStream());
            } catch (Exception e) {
                throw new SFSException(e);
            }
        } else {
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(sfs.create(file), 1024 * 16);
                IOUtils.copy(fileItem.getInputStream(), out);
                out.flush();
            } catch (IOException e) {
                throw new SFSException(e);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

    public static void writeResponse(HttpServletResponse resp, StaticFileStorage sfs, String file, String contentType) {
        Validate.notNull(resp);
        Validate.notNull(sfs);
        InputStream in = null;
        try {
            if (StringUtils.isBlank(contentType)) {
                contentType = WebUtils.getMimeTypeByFileName(file);
            }
            resp.setContentType(contentType);
            resp.addHeader("Content-Disposition", "attachment; filename=\"" + file + "\"");

            in = sfs.read(file, resp);
            int length = IOUtils.copy(in, resp.getOutputStream());
            resp.setContentLength(length);
        } catch (IOException e) {
            throw new SFSException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static void writeResponse(HttpServletResponse resp, StaticFileStorage sfs, String file) {
        writeResponse(resp, sfs, file, null);
    }

    public static void saveScaledUploadImage(File fileItem, StaticFileStorage sfs, String file, String w, String h, String format) {
        Validate.notNull(sfs);
        Validate.notNull(fileItem);
        InputStream input = null;
        OutputStream out = null;

        try {
            input = new FileInputStream(fileItem);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (sfs instanceof OssSFS) {
            try {
                //input = fileItem.getInputStream();
                byte[] inArr = IOUtils.toByteArray(input);

                byte[] outArr = ImageUtils.scale(inArr, w, h, format);

                input.close();

                saveToOSS((OssSFS) sfs, file, outArr.length, new ByteArrayInputStream(outArr));
            } catch (Exception e) {
                throw new SFSException(e);
            }

        } else {
            try {
                out = new BufferedOutputStream(sfs.create(file), 1024 * 16);
                ImageUtils.scale(input, out, w, h, format);
                input.close();
            } catch (IOException e) {
                throw new SFSException(e);
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                        IOUtils.closeQuietly(out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static void saveScaledUploadImage(FileItem fileItem, StaticFileStorage sfs, String file, String w, String h, String format) {
        Validate.notNull(sfs);
        Validate.notNull(fileItem);
        OutputStream out = null;
        if (sfs instanceof OssSFS) {
            try {
                InputStream input = fileItem.getInputStream();
                byte[] inArr = IOUtils.toByteArray(input);
                input.close();
                byte[] outArr = ImageUtils.scale(inArr, w, h, format);

                saveToOSS((OssSFS)sfs, file, outArr.length, new ByteArrayInputStream(outArr));
            } catch (Exception e) {
                throw  new SFSException(e);
            }

        } else {
            try {
                out = new BufferedOutputStream(sfs.create(file), 1024 * 16);
                ImageUtils.scale(fileItem.getInputStream(), out, w, h, format);
            } catch (IOException e) {
                throw new SFSException(e);
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                        IOUtils.closeQuietly(out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static String revertPhoto(FileItem fileItem, String expName, Record record) throws IOException {

        long date = DateUtils.nowMillis();
                String tmp = TEMP_PHOTO_IMAGE_DIR + File.separator ;
                String tmpFile = tmp +date + "."+expName;
                File f0 = new File(tmp);
                if(!f0.exists()){
                    f0.mkdirs();
                }
                File f = new File(tmpFile);
                try {
                    fileItem.write(f);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                int orientation = getOrientation(record.getString("orientation"));
                String file = tmpFile;
                switch (orientation) {
                    case 90: {
                        String _90 = date+"_90."+expName;
                        String file_90 = tmp+_90;
                        ImageMagickHelper.rotate(tmpFile, _90, "90");
                        file = file_90;
                        break;
                    }
                    case 180: {
                        String _180 = date+"_180."+expName;
                        String file_180 = tmp+_180 ;
                        ImageMagickHelper.rotate(tmpFile, _180, "180");
                        file = file_180;
                        break;
                    }
                    case 270: {
                        String _270 = date + "_270."+expName;
                        String file_270 = tmp+_270;
                        ImageMagickHelper.rotate(tmpFile, _270, "270");
                        file = file_270;
                        break;
                    }
                    default: {
                        String _default = date +"_0."+expName;
                        String file_0 = tmp + _default;
                        ImageMagickHelper.rotate(tmpFile, _default, "0");
                        file = file_0;
                        break;
                    }

                }

        f.delete();

        return file;
    }



    private static int getOrientation(String orientation) {
        if ("Bottom, right side (Rotate 180)".equals(orientation))
            return 180;
        else if ("Right side, top (Rotate 90 CW)".equals(orientation))
            return 90;
        else if ("Left side, bottom (Rotate 270 CW)".equals(orientation))
            return 270;
        else
            return 0;
    }

    public static void saveScaledImage(InputStream input, StaticFileStorage sfs, String file, String w, String h, String format) {
        if (w.equals("0") || h.equals("0")) {
            w = "";
            h = "";
        }

        Validate.notNull(sfs);
        Validate.notNull(input);
        OutputStream out = null;
        if (sfs instanceof OssSFS || true) {
            try {

                if (w.equals("") || h.equals("")) {
                    //if(ByteArrayInputStream.class.isInstance(input))
                    {
                        byte[] inArr = IOUtils.toByteArray(input);
                        input.close();
                        if (inArr.length > 0) {
                            saveToOSS((OssSFS) sfs, file, inArr.length, new ByteArrayInputStream(inArr));
                        }
                    }
                } else {
                    byte[] inArr = IOUtils.toByteArray(input);
                    input.close();
                    byte[] outArr = ImageUtils.scale(inArr, w, h, format);
                    if (outArr.length <= 0) {
                        outArr = inArr;
                    }
                    saveToOSS((OssSFS) sfs, file, outArr.length, new ByteArrayInputStream(outArr));
                }
            } catch (Exception e) {
                throw new SFSException(e);
            }

        } else {
            try {
                out = new BufferedOutputStream(sfs.create(file), 1024 * 16);
                if (w.length() == 0 || h.length() == 0) {
                    //save to local directly
                    int len = 0;
                    byte[] buf = new byte[8 * 1024];
                    while ((len = input.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } else {
                    ImageUtils.scale(input, out, w, h, format);
                }
            } catch (IOException e) {
                throw new SFSException(e);
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                        IOUtils.closeQuietly(out);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void saveText(String text, StaticFileStorage sfs, String file) {
        Validate.notNull(text);
        Validate.notNull(sfs);
        Validate.notNull(file);
        OutputStream out = null;
        if (sfs instanceof OssSFS) {
            try {
                byte[] outArr = text.getBytes();
                saveToOSS((OssSFS) sfs, file, outArr.length, new ByteArrayInputStream(outArr));
            } catch (Exception e) {
                throw new SFSException(e);
            }
        } else {
            try {
                out = sfs.create(file);
                IOUtils.write(text, out, Charsets.DEFAULT);
            } catch (IOException e) {
                throw new IOException2(e);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

    public static void saveBytes(byte[] bytes, StaticFileStorage sfs, String file) {
        Validate.notNull(bytes);
        Validate.notNull(sfs);
        Validate.notNull(file);

        OutputStream out = null;
        if (sfs instanceof OssSFS) {
            try {
                saveToOSS((OssSFS) sfs, file, bytes.length, new ByteArrayInputStream(bytes));
            } catch (Exception e) {
                throw new SFSException(e);
            }
        } else {
            try {
                out = sfs.create(file);
                out.write(bytes);
            } catch (IOException e) {
                throw new IOException2(e);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

}

