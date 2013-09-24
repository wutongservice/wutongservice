package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Paging;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;


public interface ShareService extends ServiceConsts {

    Record createShare(ServiceContext ctx, Record shareRec);

    Records listShares(ServiceContext ctx, String appId, String categoryId, Long sinceIdTS, Params opts, Paging paging);

    Record getShare(ServiceContext ctx, String id);

    Record getShareByFileId(ServiceContext ctx, String fileId);

    boolean deleteShare(ServiceContext ctx, String id);

    Record downloadFile(ServiceContext ctx, String id);

    Record downloadFileByFileId(ServiceContext ctx, String fileId);
}
