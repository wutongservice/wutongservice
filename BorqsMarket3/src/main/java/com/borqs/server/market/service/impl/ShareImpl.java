package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.deploy.TemporaryDirectories;
import com.borqs.server.market.models.FileStorageUtils;
import com.borqs.server.market.models.Tags;
import com.borqs.server.market.models.UrlCompleter;
import com.borqs.server.market.models.ValidateUtils;
import com.borqs.server.market.resfile.ResourceFile;
import com.borqs.server.market.resfile.ResourceFileUtils;
import com.borqs.server.market.service.ShareService;
import com.borqs.server.market.sfs.FileStorage;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;

@Service("service.shareService")
public class ShareImpl extends ServiceSupport implements ShareService {
    protected AccountImpl accountService;
    private FileStorage sharesStorage;
    protected UrlCompleter sharesUrlCompleter;
    private static final String uploadTemporaryDir = TemporaryDirectories.getUploadTempDirPath();


    public ShareImpl() {
    }

    @Autowired
    @Qualifier("storage.shares")
    public void setSharesStorage(FileStorage recordStorage) {
        this.sharesStorage = recordStorage;
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Autowired
    @Qualifier("helper.sharesUrlCompleter")
    public void setSharesUrlCompleter(UrlCompleter urlCompleter) {
        this.sharesUrlCompleter = urlCompleter;
    }

    // 客户端的同志们太JB懒了，非得要让share兼容product，日
    private void shareToProduct(Record share) {
        if (share == null)
            return;

        share.set("category_name", StringUtils.capitalize(share.asString("category_id")));
        share.set("cmcc_mm_amount", 1);
        share.set("cmcc_mm_paycode", "");
        share.set("default_locale", share.asString("locale", "en_US"));
        share.set("google_iab_sku", "");
        share.set("paid", 1);
        share.set("payment_type", 1);
        share.set("price", null);
        share.set("promotion_image", "");
        share.set("purchasable", true);
        share.set("purchase_count", 0L);
        share.set("download_count", share.asLong("download_count", 0L));
        share.set("purchased", false);
        share.set("recent_change", "");
        share.set("version_name", "1.0");
    }

    private void shareToProduct(Records shares) {
        if (shares == null)
            return;

        for (Record share : shares)
            shareToProduct(share);
    }

    private void downloadResultToPurchaseResult(Record r) {
        r.set("action", 1);
        r.set("first_purchase", true);
        r.set("order_id", "");
    }


    Records listShares0(RecordSession session, ServiceContext ctx, String appId, String categoryId, Long sinceIdTS, Params opts, Paging paging) {
        String authorId = opts.param("author_id").asString();
        String orderType = opts.param("order").asString("created_at");
        Integer status = opts.param("status").asIntObject();
        String tag = opts.param("tag").asString();
        String appMod = opts.param("app_mod").asString();
        Integer minAppVersion = opts.param("min_app_version").asIntObject();
        Integer maxAppVersion = opts.param("max_app_version").asIntObject();

        Records rs = session.selectList("share.listShares", CC.map(
                "app_id=>", appId,
                "category_id=>", categoryId,
                "since_id_ts=>", sinceIdTS,
                "author_id=>", authorId,
                "order_type=>", orderType,
                "status=>", status,
                "min_app_version=>", minAppVersion,
                "max_app_version=>", maxAppVersion,
                "tag=>", tag,
                "app_mod=>", appMod,
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ), SharesMapper.get());
        sharesUrlCompleter.completeUrl(rs);
        shareToProduct(rs);
        return rs;
    }

    @Override
    public Records listShares(final ServiceContext ctx, final String appId, final String categoryId, final Long sinceIdTS, final Params opts, final Paging paging) {
        Validate.notNull(ctx);
        Validate.notNull(appId);
        Validate.notNull(opts);
        Validate.notNull(paging);

        return openSession(new RecordSessionHandler<Records>() {
            @Override
            public Records handle(RecordSession session) throws Exception {
                return listShares0(session, ctx, appId, categoryId, sinceIdTS, opts, paging);
            }
        });
    }

    Record createShare0(RecordSession session, ServiceContext ctx, Record share) throws Exception {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_ILLEGAL_TICKET, "not login");

        long now = DateTimeUtils.nowMillis();
        long idTS = RandomUtils2.randomLong();
        String id = "ush_" + idTS;

        // process / save attachment file
        processFiles(ctx, id, share);


        // check uniqueness of file_id as product_id
        String fileId = share.asString("file_id", "");
        if (StringUtils.isNotEmpty(fileId)) {
            if (!ValidateUtils.validateFileIdForUserShare(fileId))
                throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product id in attachment file (" + fileId + ")");

            boolean productExists = hasFileId(session, ctx, fileId);
            if (productExists)
                throw new ServiceException(Errors.E_ILLEGAL_PRODUCT, "Illegal product id in attachment file (" + fileId + ")");
        }

        // insert
        session.insert("share.createShare", CC.map(
                "id=>", id,
                "id_ts=>", idTS,
                "file_id=>", fileId,
                "now=>", now,
                "app_id=>", share.asString("app_id", ""),
                "category_id=>", share.asString("category_id", ""),
                "author_id=>", ctx.getAccountId(),
                "author_name=>", ctx.getAccountName(),
                "author_email=>", ctx.getAccountEmail(),
                "name=>", share.asString("name", ""),
                "description=>", share.asString("description", ""),
                "content=>", share.asString("content", ""),
                "url=>", share.asString("url", ""),
                "file_size=>", share.asLong("file_size", 0L),
                "file_md5=>", share.asString("file_md5", ""),
                "app_data_1=>", share.asString("app_data_1", ""),
                "app_data_2=>", share.asString("app_data_2", ""),
                "logo_image=>", share.asString("logo_image", ""),
                "cover_image=>", share.asString("cover_image", ""),
                "screenshot1_image=>", share.asString("screenshot1_image", ""),
                "screenshot2_image=>", share.asString("screenshot2_image", ""),
                "screenshot3_image=>", share.asString("screenshot3_image", ""),
                "screenshot4_image=>", share.asString("screenshot4_image", ""),
                "screenshot5_image=>", share.asString("screenshot5_image", ""),
                "type1=>", share.asString("type1", ""),
                "type2=>", share.asString("type2", ""),
                "type3=>", share.asString("type3", ""),
                "tags=>", Tags.trimTags(share.asString("tags", "")),
                "download_count=>", 0L,
                "rating=>", 0.6,
                "rating_count=>", 0L,
                "like_count=>", 0L,
                "dislike_count=>", 0L,
                "comment_count=>", 0L,
                "share_count=>", 0L,
                "status=>", SHARE_STATUS_APPROVED,
                "app_version=>", share.asInt("app_version", 0),
                "supported_mod=>", Tags.trimTags(share.asString("supported_mod", "")),
                "device_id=>", share.asString("device_id", ""),
                "locale=>", ctx.getClientLocale(""),
                "ip=>", ObjectUtils.toString(ctx.getClientIP()),
                "ua=>", ObjectUtils.toString(ctx.getClientUserAgent())
        ));
        return getShare0(session, ctx, id);
    }


    @Override
    public Record createShare(final ServiceContext ctx, final Record share) {
        Validate.notNull(ctx);
        Validate.notNull(share);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return createShare0(session, ctx, share);
            }
        });
    }

    Record getShare0(RecordSession session, ServiceContext ctx, String id) {
        if (StringUtils.isEmpty(id))
            return null;

        Record shareRec = session.selectOne("share.getShare", CC.map("id=>", id), SharesMapper.get());
        if (shareRec != null) {
            sharesUrlCompleter.completeUrl(shareRec);
            shareToProduct(shareRec);
        }
        return shareRec;
    }

    @Override
    public Record getShare(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getShare0(session, ctx, id);
            }
        });
    }

    String getShareIdByFileId(RecordSession session, ServiceContext ctx, String fileId) {
        return session.selectStringValue("share.getShareIdByFileId", CC.map("file_id=>", fileId), null);
    }

    Record getShareByFileId0(RecordSession session, ServiceContext ctx, String fileId) {
        String id = getShareIdByFileId(session, ctx, fileId);
        if (StringUtils.isEmpty(id))
            return null;

        return getShare0(session, ctx, id);
    }

    @Override
    public Record getShareByFileId(final ServiceContext ctx, final String fileId) {
        Validate.notNull(ctx) ;

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getShareByFileId0(session, ctx, fileId);
            }
        });
    }

    boolean deleteShare0(RecordSession session, ServiceContext ctx, String id) {
        Record shareRec = getShare0(session, ctx, id);
        if (shareRec == null)
            return false;

        if (!StringUtils.equals(ctx.getAccountId(), shareRec.asString("author_id")))
            throw new ServiceException(Errors.E_PERMISSION, "You are not the author of the post");

        int n = session.delete("share.deleteShare", CC.map("id=>", id));
        return n > 0;
    }

    @Override
    public boolean deleteShare(final ServiceContext ctx, final String id) {
        return openSession(new RecordSessionHandler<Boolean>() {
            @Override
            public Boolean handle(RecordSession session) throws Exception {
                return deleteShare0(session, ctx, id);
            }
        });
    }

    boolean hasFileId(RecordSession session, ServiceContext ctx, String fileId) {
        if (StringUtils.isEmpty(fileId))
            return false;

        boolean b = session.selectBooleanValue("share.hasProductIdInProducts", CC.map("id=>", fileId), false);
        if (b)
            return b;
        return session.selectBooleanValue("share.hasProductIdInUserShare", CC.map("id=>", fileId), false);
    }

    private void processFiles(ServiceContext ctx, String id, Record share) throws Exception {
        final String[] imageFields = {
                "logo_image", "cover_image",
                "screenshot1_image", "screenshot2_image", "screenshot3_image", "screenshot4_image", "screenshot5_image"
        };
        File tmp = null;
        ResourceFile resFile = null;
        try {
            FileItem fi = share.asFileItem("file");
            if (fi != null) {
                tmp = new File(FilenameUtils.concat(uploadTemporaryDir, FilenameUtils2.changeFilenameWithoutExt(fi.getName(), "share-" + RandomUtils2.randomLong())));
                fi.write(tmp);
                if (ResourceFileUtils.isResourceFile(tmp))
                    resFile = ResourceFile.createQuietly(tmp);
            }

            // texts
            if (resFile != null) {
                share.set("file_id", ObjectUtils.toString(resFile.getId()));
                if (StringUtils.isBlank(share.asString("name")))
                    share.set("name", resFile.getDefaultName(""));
                if (StringUtils.isBlank(share.asString("description")))
                    share.set("description", resFile.getDefaultDescription(""));
            }

            // images
            for (String imageField : imageFields) {
                if (share.hasField(imageField)) {
                    if (share.isType(imageField, FileItem.class)) {
                        FileItem ifi = share.asFileItem(imageField);
                        String iid = FileStorageUtils.saveImageWithFileItem(sharesStorage, imageField, id, ifi);
                        share.set(imageField, iid);
                    }
                } else {
                    if (resFile != null) {
                        InputStream content = resFile.readImage(imageField);
                        if (content != null) {
                            String iid = FileStorageUtils.saveImageWithContent(sharesStorage, imageField,
                                    id, content, FilenameUtils.getName(resFile.getImagePath(imageField)));
                            share.set(imageField, iid);
                        }
                    }
                }
            }

            // file
            if (tmp != null) {
                long size = FileUtils2.getFileSize(tmp.getAbsolutePath());
                String md5 = FileUtils2.getFileMd5(tmp.getAbsolutePath());
                String fid = FileStorageUtils.saveProduct(sharesStorage, tmp, id, 1);
                share.set("url", fid).set("file_size", size).set("file_md5", md5);
            }

        } finally {
            if (tmp != null)
                FileUtils.deleteQuietly(tmp);
        }
    }

    Record downloadFile0(RecordSession session, ServiceContext ctx, String id) {
        Record share = getShare0(session, ctx, id);
        if (share != null) {
            String url = share.asString("url", "");
            if (StringUtils.isBlank(url))
                return null;

            session.update("share.incShareDownloadCount", CC.map("id=>", id));
            Record r = new Record().set("url", url)
                    .set("id", share.asString("id"))
                    .set("file_id", share.asString("file_id"))
                    .set("app_id", share.asString("app_id"))
                    .set("file_size", share.asLong("file_size", 0L))
                    .set("file_md5", share.asString("file_md5", ""));

            downloadResultToPurchaseResult(r);
            return r;
        } else {
            return null;
        }
    }

    @Override
    public Record downloadFile(final ServiceContext ctx, final String id) {
        Validate.notNull(ctx);
        Validate.notNull(id);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return downloadFile0(session, ctx, id);
            }
        });
    }

    Record downloadFileByFileId0(RecordSession session, ServiceContext ctx, String fileId) {
        String id = getShareIdByFileId(session, ctx, fileId);
        if (StringUtils.isEmpty(id))
            return null;

        return downloadFile0(session, ctx, id);
    }

    @Override
    public Record downloadFileByFileId(final ServiceContext ctx, final String fileId) {
        Validate.notNull(ctx);
        Validate.notNull(fileId);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return downloadFileByFileId0(session, ctx, fileId);
            }
        });
    }
}
