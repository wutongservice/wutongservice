package com.borqs.server.wutong.reportabuse;


import com.borqs.server.base.context.Context;

import java.util.Map;

public interface ReportAbuseLogic {

    int getReportAbuseCount(Context ctx,int target_type,String target_id,int appid) ;

    int iHaveReport(Context ctx, String viewerId, int target_type,String target_id,int appid) ;

    boolean reportAbuserCreate(Context ctx, String viewerId, int target_type,String target_id,String reason,int appid, String ua, String loc) ;

    Map<String, Boolean> iHaveReport(Context ctx, String viewerId, String[] postIds,int target_type,int appid);
}