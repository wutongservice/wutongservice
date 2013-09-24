package com.borqs.server.wutong.search;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;

import java.util.Collection;
import java.util.Map;

public interface SearchLogic {
    void addPosts(Collection<PostDoc> postDocs);
    void deletePosts(long[] postIds);

    Record search(Context ctx, String q, String type, Map<String, String> options, int page, int count);
}
