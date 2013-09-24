package com.borqs.server.wutong.search;


import com.borqs.server.ServerException;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.conf.GlobalConfig;
import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.Initializable;
import com.borqs.server.wutong.Constants;
import com.borqs.server.wutong.GlobalLogics;
import com.borqs.server.wutong.WutongErrors;
import com.borqs.server.wutong.group.GroupLogic;
import com.borqs.server.wutong.stream.StreamLogic;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.util.*;

public class SolrSearchImpl extends AbstractSearch implements Initializable {

    private static final Logger L = Logger.getLogger(SolrSearchImpl.class);

    private SolrServer solr;

    @Override
    public void init() {
        Configuration conf = GlobalConfig.get();
        String solrAddr = conf.getString("search.solr.server", null);
        solr = new HttpSolrServer(solrAddr);
    }

    @Override
    public void destroy() {
        solr = null;
    }

    @Override
    public void addPosts(Collection<PostDoc> postDocs) {
        ArrayList<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        if (CollectionUtils.isNotEmpty(postDocs)) {
            for (PostDoc postDoc : postDocs) {
                if (postDoc != null)
                    docs.add(SolrDocs.createPostDoc(postDoc));
            }
        }
        if (!docs.isEmpty()) {
            try {
                solr.add(docs);
                solr.commit();
            } catch (Exception e) {
                throw new ServerException(WutongErrors.SEARCH_ADD_ERROR, e);
            }
        }
    }

    @Override
    public void deletePosts(long[] postIds) {
        try {
            solr.deleteById(SolrDocs.makePostIds(postIds));
            solr.commit();
        } catch (Exception e) {
            throw new ServerException(WutongErrors.SEARCH_DELETE_ERROR, e);
        }
    }

    private static SolrResponse solrQuery(SolrServer solr, SolrQuery sq) {
        try {
            return solr.query(sq);
        } catch (SolrServerException e) {
            throw new ServerException(WutongErrors.SEARCH_QUERY_ERROR, e);
        }
    }


    @Override
    public Record search(Context ctx, String q, String type, Map<String, String> options, int page, int count) {
        final String METHOD_NAME = "SolrSearchImpl.search";
        L.traceStartCall(ctx, METHOD_NAME, q, options, page, count);
        SolrDocumentList docs = new SolrDocumentList();
        boolean allowSearch;
        if ("post".equals(type)) {
            StringBuilder qe = new StringBuilder();
            String qw = trimQueryWord(q);
            qe.append("(text:").append(qw).append(") ");
            if (options.containsKey("group")) {
                long groupId = Long.parseLong(options.get("group"));
                GroupLogic group = GlobalLogics.getGroup();
                Record groupRec = group.getGroup(ctx, ctx.getViewerIdString(), groupId, Constants.GROUP_LIGHT_COLS, false);

                if (groupRec.getInt("is_stream_public") != 0) {
                    allowSearch = true;
                } else {
                    allowSearch = ctx.getViewerId() > 0
                            && group.hasRight(ctx, groupId, ctx.getViewerId(), Constants.ROLE_MEMBER);
                }

                qe.append(" AND group_id:").append(groupId).append(" ");
            } else {
                allowSearch = true;
                qe.append(" AND -group_id:[* TO *]");
                qe.append(" AND (private:0)");
            }

            if (allowSearch) {
                String sortField = "score";
                if (options.containsKey("sort"))
                    sortField = options.get("sort");

                SolrQuery sq = new SolrQuery(qe.toString());
                sq.setStart(page * count);
                sq.setRows(count);
                sq.setFields(SolrDocs.FIELD_ID, SolrDocs.FIELD_CONTENT);
                sq.setSortField(sortField, SolrQuery.ORDER.desc);

                SolrResponse sr = solrQuery(solr, sq);
                docs.addAll((SolrDocumentList) sr.getResponse().get("response"));
            }
        } else {
            throw new IllegalArgumentException("Illegal search type " + type);
        }
        L.traceEndCall(ctx, METHOD_NAME);
        return docsToResult(ctx, docs);
    }

    private Record docsToResult(Context ctx, SolrDocumentList docs) {
        ArrayList<Long> postIds = new ArrayList<Long>();
        for (SolrDocument doc : docs) {
            String id = (String) doc.getFieldValue("id");
            if (id != null && id.endsWith("@post")) {
                postIds.add(Long.parseLong(StringUtils.removeEnd(id, "@post")));
            }
        }

        Record r = new Record();
        if (!postIds.isEmpty()) {
            StreamLogic stream = GlobalLogics.getStream();
            Map<Long, Record> recsMap = stream.getPostsP(ctx, StringUtils.join(postIds, ","), Constants.POST_FULL_COLUMNS).toIntRecordMap("post_id");
            RecordSet recs = new RecordSet();
            for (long postId : postIds) {
                Record postRec = recsMap.get(postId);
                if (postRec != null)
                    recs.add(postRec);
            }
            if (!recs.isEmpty())
                r.put("posts", recs);
        }

        return r;
    }
}
