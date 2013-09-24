package com.borqs.server.base.sfs;


import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.borqs.server.base.io.Charsets;
import com.borqs.server.base.io.IOException2;
import com.borqs.server.base.sfs.oss.OssSFS;
import com.borqs.server.base.util.image.ImageUtils;
import com.borqs.server.base.web.WebUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class SFSUtils {
    public static void saveToOSS(OssSFS sfs, String file, long contentLength, InputStream is) throws Exception {
        String suffix = StringUtils.substringAfterLast(file, ".");
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

        client.putObject(sfs.getBucketName(), file, is, objectMeta);
        is.close();
    }

    public static void saveUpload(FileItem fileItem, StaticFileStorage sfs, String file) {
        Validate.notNull(sfs);
        Validate.notNull(fileItem);

        if (sfs instanceof OssSFS) {
            try {
                saveToOSS((OssSFS)sfs, file, fileItem.getSize(), fileItem.getInputStream());
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

            in = sfs.read(file);
            IOUtils.copy(in, resp.getOutputStream());
        } catch (IOException e) {
            throw new SFSException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static void writeResponse(HttpServletResponse resp, StaticFileStorage sfs, String file) {
        writeResponse(resp, sfs, file, null);
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

    public static void saveScaledImage(InputStream input, StaticFileStorage sfs, String file, String w, String h, String format) {
         if(w.equals("0") || h.equals("0"))
         {
             w="";
             h="";
         }

        Validate.notNull(sfs);
        Validate.notNull(input);
        OutputStream out = null;
        if (sfs instanceof OssSFS || true) {
            try {

                if(w.equals("") || h.equals(""))
                {
                    //if(ByteArrayInputStream.class.isInstance(input))
                    {
                        byte[] inArr = IOUtils.toByteArray(input);
                        input.close();
                        saveToOSS((OssSFS)sfs, file, inArr.length, new ByteArrayInputStream(inArr));
                    }
                }
                else
                {
                    byte[] inArr = IOUtils.toByteArray(input);
                    input.close();
                    byte[] outArr = ImageUtils.scale(inArr, w, h, format);
                    saveToOSS((OssSFS)sfs, file, outArr.length, new ByteArrayInputStream(outArr));
                }
            } catch (Exception e) {
                throw  new SFSException(e);
            }

        } else {
            try {
                out = new BufferedOutputStream(sfs.create(file), 1024 * 16);
                if(w.length() ==0 || h.length() ==0)
                {
                    //save to local directly
                    int len = 0;
                    byte[] buf = new byte[8*1024];
                    while((len = input.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                }
                else
                {
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
                saveToOSS((OssSFS)sfs, file, outArr.length, new ByteArrayInputStream(outArr));
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
                saveToOSS((OssSFS)sfs, file, bytes.length, new ByteArrayInputStream(bytes));
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

