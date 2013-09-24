
package com.borqs.server.wutong.tag;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;

import java.util.Map;

public interface TagLogic  {
    Record createTag(Context ctx,Record tag);

    boolean destroyedTag(Context ctx,Record tag);

    boolean hasTag(Context ctx,String userId, String tag,String scope,String target_id,String type);

    RecordSet findTagByUser(Context ctx,String user_id,String scope, int page, int count) ;

    RecordSet findUserByTagScopeType(Context ctx, String tag, String scope, String type, int page, int count);

    RecordSet findTargetsByUser(Context ctx,String userId,String scope, String type, int page, int count);

    RecordSet findUserTagByTarget(Context ctx,String scope,String target_id, String type, int page, int count);

    RecordSet findAllUserByTargetTag(Context ctx,String tag,String scope,String target_id, String type);

    String[] getTagContentsByTarget(Context ctx, String target_id, String type);

    Map<String, String[]> getTagContentsByTargets(Context ctx, String[] targetIds,String scope, String type);
}