package com.borqs.server.wutong.ignore;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.RecordSet;

import java.util.List;

public interface IgnoreLogic {
    boolean createIgnore(Context ctx,String userId, String targetType, String targetId) ;

    boolean deleteIgnore(Context ctx,String userId, String targetType, String targetId) ;

    RecordSet getIgnoreList(Context ctx,String user_id, String target_type, int page, int count) ;

    boolean getExistsIgnore(Context ctx,String userId, String targetType, String targetId) ;

    String formatIgnoreUsers(Context ctx,String viewerId, String userIds);

    RecordSet getIgnoreListSimpleP(Context ctx,String userId, String target_type, int page, int count);

    RecordSet formatIgnoreStreamOrCommentsP(Context ctx,String viewerId, String sORc, RecordSet recs);

    // Platform
    List<Long> formatIgnoreUserListP(Context ctx, List<Long> userList,String post_id,String comment_id);
}