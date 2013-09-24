package com.borqs.server.market.controllers;

import com.borqs.server.market.sfs.FileContent;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.MimeTypeUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/")
public class ProductFilesController {
    private FileStorage imageStorage;
    private FileStorage productStorage;
    private FileStorage sharesStorage;

    public ProductFilesController() {
    }

    public FileStorage getImageStorage() {
        return imageStorage;
    }

    @Autowired
    @Qualifier("storage.image")
    public void setImageStorage(FileStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    public FileStorage getProductStorage() {
        return productStorage;
    }

    @Autowired
    @Qualifier("storage.product")
    public void setProductStorage(FileStorage productStorage) {
        this.productStorage = productStorage;
    }

    @Autowired
    @Qualifier("storage.shares")
    public void setSharesStorage(FileStorage sharesStorage) {
        this.sharesStorage = sharesStorage;
    }

    private static void writeError(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType("text/plain");
        resp.getWriter().print(msg);
    }

    private static void writeFileToResponse(FileStorage fs, String fileId, HttpServletResponse resp) throws IOException {
        FileContent content;
        try {
            content = fs.read(fileId);
        } catch (IOException e) {
            content = null;
        }

        OutputStream out = null;
        if (content == null) {
            writeError(resp, 404, "File not found");
        } else {
            try {
                resp.setStatus(200);
                if (content.size >= 0) {
                    resp.setContentLength((int) content.size);
                }
                String fileName = content.filename != null ? content.filename : fileId;
                String contentType = content.contentType != null ? content.contentType : MimeTypeUtils.getMimeTypeByFilename(fileName);
                resp.setContentType(contentType);
                if (!contentType.startsWith("text/") && !contentType.startsWith("image/")) {
                    resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                }

                out = resp.getOutputStream();
                IOUtils.copy(content.stream, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(content.stream);
                IOUtils.closeQuietly(out);
            }
        }
    }

    @RequestMapping(value = "/static/products/{fileId:.+}", method = RequestMethod.GET)
    public void writeProductFile(@PathVariable("fileId") String fileId, HttpServletResponse resp) throws IOException {
        writeFileToResponse(productStorage, fileId, resp);
    }

    @RequestMapping(value = "/static/products/images/{fileId:.+}", method = RequestMethod.GET)
    public void writeProductImagesFile(@PathVariable("fileId") String fileId, HttpServletResponse resp) throws IOException {
        writeFileToResponse(imageStorage, fileId, resp);
    }

    @RequestMapping(value = "/static/shares/{fileId:.+}", method = RequestMethod.GET)
    public void writeSharesFile(@PathVariable("fileId") String fileId, HttpServletResponse resp) throws IOException {
        writeFileToResponse(sharesStorage, fileId, resp);
    }
}
