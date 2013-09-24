package com.borqs.server.wutong.request;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

public interface RequestLogic {
    String createRequest(Context ctx,String userId, String sourceId, String app, String type, String message, String data, String options);

    boolean createRequests(Context ctx,String userIds, String sourceId, String app, String type, String message, String data, String options);

    boolean destroyRequests(Context ctx,String userId, String requests) ;

    RecordSet getRequests(Context ctx,String userId, String app, String type);

    boolean doneRequest(Context ctx,String userId, String requestIds);

    int getCount(Context ctx,String userId, String app, String type);

    String getPendingRequests(Context ctx, String source, String user);

    RecordSet getPendingRequestsAll(Context ctx, String source, String userIds);

    String getRelatedRequestIds(Context ctx, String userId, String sourceIds, String datas);



    // Platform
    boolean dealRelatedRequestsP(Context ctx, String userId, String sourceIds, String datas);


    int getRequestCountP(Context ctx, String userId, String app, String type);

    boolean createRequestAttentionP(Context ctx, String userId, String friendId);

    RecordSet getRequestsP(Context ctx, String userId, String app, String type);

    boolean doneRequestsP(Context ctx, String userId, String requestIds, String type, String data, boolean accept);

    String createRequestP(Context ctx, String userId, String sourceId, String app, String type, String message, String data, boolean addAddressCircle);

    Record getRequestSummary(Context ctx,String appId);

    boolean isRequestsExist(Context ctx,String userIds, String sourceId, String app, String type, String message, String data, String options);

    RecordSet getRequestsNewTop(Context ctx,String userId, String appId, String type, String scene);

    RecordSet getUnDoneRequestsGroupByScene(Context ctx, String userId, String appId, String type);
}