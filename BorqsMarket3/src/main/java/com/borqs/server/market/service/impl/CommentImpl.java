package com.borqs.server.market.service.impl;


import com.borqs.server.market.Errors;
import com.borqs.server.market.ServiceException;
import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.service.CommentService;
import com.borqs.server.market.utils.*;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import com.borqs.server.market.utils.mybatis.record.RecordSessionHandler;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("service.commentService")
public class CommentImpl extends ServiceSupport implements CommentService {
    protected AccountImpl accountService;
    protected PublishImpl publishService;

    public CommentImpl() {
    }

    @Autowired
    @Qualifier("service.account")
    public void setAccountService(AccountImpl accountService) {
        this.accountService = accountService;
    }

    @Autowired
    @Qualifier("service.defaultPublishService")
    public void setPublishService(PublishImpl publishService) {
        this.publishService = publishService;
    }

    void resolveCommenterName(RecordSession session, ServiceContext ctx, Record comment) {
        if (comment != null)
            accountService.fillDisplayName(session, ctx, comment, "commenter_id", "commenter_name");
    }

    void resolveCommenterName(RecordSession session, ServiceContext ctx, Records comments) {
        if (comments != null)
            accountService.fillDisplayName(session, ctx, comments, "commenter_id", "commenter_name");
    }

    RecordsWithTotal listComments0(RecordSession session, ServiceContext ctx, String productId, Integer version, Paging paging) {
        RecordsWithTotal rs = session.selectListWithTotal("comment.listComments", CC.map(
                "product_id=>", productId,
                "version=>", version,
                "offset=>", paging.getOffset(),
                "count=>", paging.getCount()
        ), GenericMapper.get());

        resolveCommenterName(session, ctx, rs.getRecords());
        localeSelector.selectLocale(rs, ctx);
        return rs;
    }

    @Override
    public RecordsWithTotal listComments(final ServiceContext ctx, final String productId, final Integer version, final Paging paging) {
        return openSession(new RecordSessionHandler<RecordsWithTotal>() {
            @Override
            public RecordsWithTotal handle(RecordSession session) throws Exception {
                return listComments0(session, ctx, productId, version, paging);
            }
        });
    }

    Record updateComment0(RecordSession session, ServiceContext ctx, String productId, int version, String message, double rating) {
        if (!ctx.hasAccountId())
            throw new ServiceException(Errors.E_ILLEGAL_TICKET, "Need signin");

        Record versionRec = publishService.getVersion(session, ctx, productId, version);
        if (versionRec == null || versionRec.asInt("status") != PV_STATUS_PUBLISHED)
            throw new ServiceException(Errors.E_ILLEGAL_VERSION, "Illegal version");

//        String orderId = session.selectStringValue("market.findOrderIdForPurchase", CC.map(
//                "id=>", productId,
//                "purchaser_id=>", ctx.getAccountId(),
//                "device_id=>", null
//        ), null);

//        String downloadsId = session.selectStringValue("market.findFirstDownloadIdForPurchase", CC.map(
//                "product_id=>", productId,
//                "purchaser_id=>", ctx.getAccountId(),
//                "device_id=>", ctx.getClientDeviceId()
//        ), null);
//
//        if (StringUtils.isBlank(orderId) && StringUtils.isBlank(downloadsId))
//            throw new ServiceException(Errors.E_ILLEGAL_COMMENT, "not download or buy the product yet ,can not create comment");

        String commentId;
        Record myComment = getMyCommentForProduct(session, ctx, productId, version, false);
        if (myComment == null) {
            commentId = "pcm_" + RandomUtils2.randomLong();
            long now = DateTimeUtils.nowMillis();
            session.insert("comment.createComment", CC.map(
                    "id=>", commentId,
                    "account_id=>", ctx.getAccountId(),
                    "product_id=>", productId,
                    "version=>", version,
                    "now=>", now,
                    "message=>", message,
                    "device=>", ctx.getClientDeviceId(""),
                    "rating=>", rating
            ));

            // get avg rating
            Records ratings = session.selectList("comment.getAvgRating", CC.map(
                    "product_id=>", productId,
                    "version=>", version
            ));
            double versionRating = ratings.get(0).asDouble("avg_rating", 0.0);
            double productRating = ratings.get(1).asDouble("avg_rating", 0.0);
            session.update("comment.updateCommentCountOfProductVersions", CC.map(
                    "product_id=>", productId,
                    "version=>", version,
                    "version_rating=>", versionRating
            ));
            session.update("comment.updateCommentCountOfProducts", CC.map(
                    "product_id=>", productId,
                    "product_rating=>", productRating
            ));
        } else {
            commentId = myComment.asString("id");
            session.update("comment.updateComment", CC.map(
                    "message=>", ObjectUtils.toString(message),
                    "rating=>", rating,
                    "device=>", ctx.getClientDeviceId(""),
                    "now=>", DateTimeUtils.nowMillis(),
                    "id=>", commentId
            ));
        }

        return getComment0(session, ctx, commentId, true);
    }

    @Override
    public Record updateComment(final ServiceContext ctx, final String productId, final int version, final String message, final double rating) {
        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return updateComment0(session, ctx, productId, version, message, rating);
            }
        });
    }


    private Record getComment0(RecordSession session, ServiceContext ctx, String id, boolean resolveCommenterName) {
        Record commentRec = session.selectOne("comment.getComment", CC.map("id=>", id), GenericMapper.get());
        if (commentRec != null) {
            localeSelector.selectLocale(commentRec, ctx);
            if (resolveCommenterName)
                resolveCommenterName(session, ctx, commentRec);
        }
        return commentRec;
    }

    Record getMyCommentForProduct(RecordSession session, ServiceContext ctx, String productId, Integer version, boolean resolveCommenterName) {
        if (!ctx.hasAccountId())
            return null;

        Record commentRec = session.selectOne("comment.getMyCommentForProduct", CC.map(
                "product_id=>", productId,
                "version=>", version,
                "account_id=>", ctx.getAccountId()
        ), GenericMapper.get());
        if (commentRec != null) {
            localeSelector.selectLocale(commentRec, ctx);
            if (resolveCommenterName)
                resolveCommenterName(session, ctx, commentRec);
        }
        return commentRec;
    }

    @Override
    public Record getMyCommentForProduct(final ServiceContext ctx, final String productId, final Integer version) {
        Validate.notNull(ctx);
        Validate.notNull(productId);

        return openSession(new RecordSessionHandler<Record>() {
            @Override
            public Record handle(RecordSession session) throws Exception {
                return getMyCommentForProduct(session, ctx, productId, version, true);
            }
        });
    }
}
