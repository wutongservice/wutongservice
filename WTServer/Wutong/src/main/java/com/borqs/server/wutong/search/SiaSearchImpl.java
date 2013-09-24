package com.borqs.server.wutong.search;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.Initializable;

import java.util.Collection;
import java.util.Map;

public class SiaSearchImpl extends AbstractSearch implements Initializable {

    public SiaSearchImpl() {
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void addPosts(Collection<PostDoc> postDocs) {
        // TODO: xx
    }

    @Override
    public void deletePosts(long[] postIds) {
        // TODO: xx
    }

    @Override
    public Record search(Context ctx, String q, String type, Map<String, String> options, int page, int count) {
        // TODO: xx
        return new Record();
    }
}
