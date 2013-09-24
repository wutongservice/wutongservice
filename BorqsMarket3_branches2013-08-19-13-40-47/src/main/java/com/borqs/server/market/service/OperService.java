package com.borqs.server.market.service;


import com.borqs.server.market.context.ServiceContext;
import com.borqs.server.market.utils.Params;
import com.borqs.server.market.utils.mybatis.record.RecordsWithTotal;
import com.borqs.server.market.utils.record.CheckResult;
import com.borqs.server.market.utils.record.Record;
import com.borqs.server.market.utils.record.Records;

import java.util.List;
import java.util.Map;

public interface OperService extends ServiceConsts {
    int DEFAULT_PARTITION_COUNT_PER_PAGE = 20;

    Records listApps(ServiceContext ctx);
    Records listCategories(ServiceContext ctx, String appId);
    RecordsWithTotal listPartitions(ServiceContext ctx, String appId, String categoryId, Params options);
    Record getPartition(ServiceContext ctx, String id);
    Record addPartition(ServiceContext ctx, String appId, String categoryId, Params params);
    void deletePartition(ServiceContext ctx, String id);
    Record updatePartition(ServiceContext ctx, String id, Params params);
    Record setPartitionList(ServiceContext ctx, String id, List<String> productIds);
    Record setPartitionRule(ServiceContext ctx, String id, String rule);

    Records getActivePartitions(ServiceContext ctx, String appId, String categoryId);
    Records activePartition(ServiceContext ctx, String appId, String categoryId, String partitionId, int status);

    void setOperator(ServiceContext ctx, String appId, String accountId);

    CheckResult checkPartition(ServiceContext ctx, String appId, String categoryId, Params params);
}
