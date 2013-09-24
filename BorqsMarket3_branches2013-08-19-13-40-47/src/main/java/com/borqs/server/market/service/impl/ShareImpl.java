package com.borqs.server.market.service.impl;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.AccountService;
import com.borqs.server.market.service.ShareService;
import com.borqs.server.market.sfs.FileContent;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import org.apache.commons.fileupload.FileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

@Service("service.shareService")
public class ShareImpl extends ServiceSupport implements ShareService {
    protected AccountService accountService;
    private FileStorage recordStorage;

    @Autowired
    @Qualifier("storage.product")
    public void setProductStorage(FileStorage recordStorage) {
        this.recordStorage = recordStorage;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountService accountService) {
        this.accountService = accountService;
    }

    public ShareImpl() {
    }

    RecordsWithTotal getShares0(RecordSession session, ServiceContext ctx, Params params, int count, int pages) {

        String author_id = params.getString("author_id", null);
        String order_type = params.getString("order_type", null);

        RecordsWithTotal datas = session.selectListWithTotal("share.getShares", CC.map(
                "author_id=>", author_id,
                "order_type=>", order_type,
                "count=>", count,
                "pages=>", pages
        ), RecordResultMapper.get());

        return datas;
    }

    @Override
    public RecordsWithTotal getShares(final ServiceContext ctx, final Params params, final int count, final int pages) {
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return getShares0(session, ctx, params, count, pages);
            }
        });
    }

    Integer createShare0(RecordSession session, ServiceContext ctx, Params params, Record record) {
        params.putAll(record);
        return session.insert("share.createShare", params.getParams());

    }

    @Override
    public Integer createShare(final ServiceContext ctx, final Params params) {
        //UA
        params.put("device_id", ctx.getClientDeviceId());
        params.put("locale", ctx.getClientLocale());
        params.put("ip", ctx.getClientIP());
        params.put("ua", ctx.getClientUserAgent());

        //status
        params.put("status", SHARE_STATUS_APPROVED);

        int i = openSession(new RecordSessionHandler<Integer>() {
            @Override
            public Integer handle(RecordSession session) throws Exception {
                Record r = new Record(params.getParams());
                Record record = uploadFiles(ctx, r);
                return createShare0(session, ctx, params, record);
            }
        });

        return i;
    }

    private Record uploadFiles(ServiceContext ctx, Record record) throws IOException {
        String id = record.asString("id");
        String logoImageUrl = null;
        if (record.isType("logo_image", FileItem.class)) {
            logoImageUrl = saveImage(record.asFileItem("logo_image"), id, "logo");
        }
        record.put("logo_image", logoImageUrl);

        String coverImageUrl = null;
        if (record.isType("cover_image", FileItem.class)) {
            FileItem fi = record.asFileItem("cover_image");
            long size = fi.getSize();
            String md5 = null;
            try {
                md5 = FileUtils2.getFileMd5(fi.getInputStream());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            record.put("file_size", size);
            record.put("file_md5", md5);

            coverImageUrl = saveImage(fi, id, "cover");
        }
        record.put("cover_image", coverImageUrl);

        String fileUrl = null;
        if (record.isType("file", FileItem.class)) {
            fileUrl = saveImage(record.asFileItem("file"), id, "file");
        }
        record.put("url", fileUrl);

        String screenshot1ImageUrl = null;
        if (record.isType("screenshot1_image", FileItem.class)) {
            screenshot1ImageUrl = saveImage(record.asFileItem("screenshot1_image"), id, "screenshot1");
        }
        record.put("screenshot1ImageUrl", screenshot1ImageUrl);

        String screenshot2ImageUrl = null;
        if (record.isType("screenshot2_image", FileItem.class)) {
            screenshot2ImageUrl = saveImage(record.asFileItem("screenshot2_image"), id, "screenshot2");
        }
        record.put("screenshot2ImageUrl", screenshot2ImageUrl);

        String screenshot3ImageUrl = null;
        if (record.isType("screenshot3_image", FileItem.class)) {
            screenshot3ImageUrl = saveImage(record.asFileItem("screenshot3_image"), id, "screenshot3");
        }
        record.put("screenshot3ImageUrl", screenshot3ImageUrl);

        String screenshot4ImageUrl = null;
        if (record.isType("screenshot4_image", FileItem.class)) {
            screenshot4ImageUrl = saveImage(record.asFileItem("screenshot4_image"), id, "screenshot4");
        }
        record.put("screenshot4ImageUrl", screenshot4ImageUrl);

        String screenshot5ImageUrl = null;
        if (record.isType("screenshot5_image", FileItem.class)) {
            screenshot5ImageUrl = saveImage(record.asFileItem("screenshot5_image"), id, "screenshot5");
        }
        record.put("screenshot5ImageUrl", screenshot5ImageUrl);
        return record;
    }

    Integer deleteShare0(RecordSession session, ServiceContext ctx, Params params) {
        return session.delete("share.deleteShare", params.getParams());

    }

    @Override
    public Integer deleteShare(final ServiceContext ctx, final Params params) {
        int i = openSession(new RecordSessionHandler<Integer>() {
            @Override
            public Integer handle(RecordSession session) throws Exception {
                return deleteShare0(session, ctx, params);
            }
        });

        return i;
    }


    private String saveImage(FileItem fi, String id, String type) throws IOException {
        FileContent content = makeFileContent(fi, fi.getInputStream(), fi.getName());
        String fid = content != null ? FilenameUtils2.changeFilenameWithoutExt(content.filename, makeProductImageFilename(id, type)) : null;
        return recordStorage.write(fid, content);
    }

    private static FileContent makeFileContent(FileItem fileItem, InputStream content, String filename) throws IOException {
        if (fileItem != null && !fileItem.isFormField()) {
            return FileContent.createWithFileItem(fileItem).withFilename(fileItem.getName());
        } else {
            if (content != null) {
                String contentType = MimeTypeUtils.getMimeTypeByFilename(filename);
                return FileContent.create(content, contentType, content.available()).withFilename(filename);
            } else {
                return null;
            }
        }
    }

    public static String makeProductImageFilename(String id, String type) {
        return String.format("%s-%s-%06d", id, type, RandomUtils2.randomInt(0, 1000000));
    }
}
