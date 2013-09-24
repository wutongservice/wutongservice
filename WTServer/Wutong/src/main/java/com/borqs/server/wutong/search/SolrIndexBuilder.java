package com.borqs.server.wutong.search;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordHandler;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.sql.ConnectionFactory;
import com.borqs.server.base.sql.SQLBuilder;
import com.borqs.server.base.sql.SQLExecutor;
import com.borqs.server.base.sql.SQLTemplate;
import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.FastHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.util.*;

public class SolrIndexBuilder {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String type = args[0];
        String mysqlConnStr = args[1];
        String solrAddr = args[2];

        if ("post".equalsIgnoreCase(type)) {
            buildPostIndex(mysqlConnStr, solrAddr);
        }
    }

    private static void printUsage() {
        System.out.printf("java -cp ... %s post accountMySqlConn solrServer", SolrIndexBuilder.class.getName());
    }

    private static void buildPostIndex(String mysqlConnStr, String solrAddr) throws Exception {
        final SolrServer solrServer = new HttpSolrServer(solrAddr);
        ConnectionFactory cf = ConnectionFactory.getConnectionFactory("dbcp");
        solrServer.deleteByQuery("object_type:post");
        solrServer.commit();
        try {
            final SQLExecutor se = new SQLExecutor(cf, mysqlConnStr);
            final String sql = "SELECT * FROM stream ORDER BY created_time DESC";
            final RecordSet recs = new RecordSet();
            se.executeRecordHandler(sql, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    if (rec.getInt("destroyed_time") == 0) {
                        recs.add(rec);
                    }
                    if (recs.size() > 100) {
                        try {
                            addPostsIndexes(recs, se, solrServer);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        recs.clear();
                    }
                }
            });
            if (!recs.isEmpty()) {
                try {
                    addPostsIndexes(recs, se, solrServer);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            cf.close();
        }
    }

    private static void addPostsIndexes(RecordSet recs, SQLExecutor se, SolrServer solr) throws Exception {
        ArrayList<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        HashSet<Long> userIds = new HashSet<Long>();
        HashSet<Long> groupIds = new HashSet<Long>();
        for (Record rec : recs) {
            userIds.add(rec.getInt("source"));
            long[] to = StringUtils2.splitIntArray(rec.getString("mentions"), "");
            long[] addTo = StringUtils2.splitIntArray(rec.getString("add_to"), "");
            for (long to0 : to) {
                if (to0 > 0 && to0 < Constants.GROUP_ID_BEGIN)
                    userIds.add(to0);
                else if (to0 >= Constants.GROUP_ID_BEGIN && to0 < Constants.GROUP_ID_END)
                    groupIds.add(to0);
            }
            for (long to0 : addTo) {
                if (to0 > 0 && to0 < Constants.GROUP_ID_BEGIN)
                    userIds.add(to0);
                else if (to0 >= Constants.GROUP_ID_BEGIN && to0 < Constants.GROUP_ID_END)
                    groupIds.add(to0);
            }
        }
        Map<Long, String> userNames = getUserNames(se, userIds);
        Map<Long, String> groupNames = getGroupNames(se, groupIds);

        for (Record rec : recs) {
            long postId = rec.getInt("post_id");

            long createdTime = rec.getInt("created_time");
            long updatedTime = rec.getInt("updated_time");

            long fromId = rec.getInt("source");
            String from = userNames.get(rec.getInt("source"));

            LinkedHashSet<Long> toIds = new LinkedHashSet<Long>();
            LinkedHashSet<String> to = new LinkedHashSet<String>();

            LinkedHashSet<Long> addToIds = new LinkedHashSet<Long>();
            LinkedHashSet<String> addTo = new LinkedHashSet<String>();

            LinkedHashSet<Long> groupIds1 = new LinkedHashSet<Long>();
            LinkedHashSet<String> groupNames1 = new LinkedHashSet<String>();

            boolean private_ = rec.getInt("privince") != 0L;

            String message = rec.getString("message", "");

            long[] to1 = StringUtils2.splitIntArray(rec.getString("mentions"), "");
            long[] addTo1 = StringUtils2.splitIntArray(rec.getString("add_to"), "");
            for (long to0 : to1) {
                if (to0 > 0 && to0 < Constants.GROUP_ID_BEGIN) {
                    String name = userNames.get(to0);
                    to.add(name);
                    toIds.add(to0);
                } else if (to0 >= Constants.GROUP_ID_BEGIN && to0 < Constants.GROUP_ID_END) {
                    groupIds1.add(to0);
                    groupNames1.add(groupNames.get(to0));
                }
            }

            for (long to0 : addTo1) {
                if (to0 > 0 && to0 < Constants.GROUP_ID_BEGIN) {
                    String name = userNames.get(to0);
                    addTo.add(name);
                    addToIds.add(to0);
                }
            }

            for (long groupId : groupIds1) {
                groupNames1.add(groupNames.get(groupId));
            }


            PostDoc postDoc = new PostDoc();
            postDoc.setId(postId);
            postDoc.setCreatedTime(createdTime);
            postDoc.setUpdatedTime(updatedTime);
            postDoc.setFrom(from);
            postDoc.setFromId(fromId);
            postDoc.setTo(to.toArray(new String[to.size()]));
            postDoc.setToIds(CollectionUtils2.toLongArray(toIds));
            postDoc.setAddTos(addTo.toArray(new String[addTo.size()]));
            postDoc.setAddToIds(CollectionUtils2.toLongArray(addToIds));
            postDoc.setGroups(groupNames1.toArray(new String[groupNames1.size()]));
            postDoc.setGroupIds(CollectionUtils2.toLongArray(groupIds1));
            postDoc.setCategory("");
            postDoc.setCategoryId(0L);
            postDoc.setTags(null);
            postDoc.setMessage(message);
            postDoc.setPrivate_(private_);

            docs.add(SolrDocs.createPostDoc(postDoc));
        }


        for (SolrInputDocument doc : docs) {
            System.out.println(doc);
        }
        solr.add(docs);
        solr.commit();
        System.out.println("----------------");
    }

    private static SolrInputDocument postToDoc(Record rec) {
        return null;
    }

    private static Map<Long, String> getUserNames(SQLExecutor se, Collection<Long> userIds) {
        final HashMap<Long, String> names = new HashMap<Long, String>();
        if (CollectionUtils.isNotEmpty(userIds)) {
            String sql1 = SQLTemplate.merge("SELECT user,`value` FROM accounts.user_property WHERE `key`=13 AND sub=3 AND user IN (${user_ids})", "user_ids", StringUtils.join(userIds, ","));
            String sql2 = SQLTemplate.merge("SELECT user,`value` FROM accounts.user_property WHERE `key`=13 AND sub=1 AND user IN (${user_ids})", "user_ids", StringUtils.join(userIds, ","));
            se.executeRecordHandler(sql1, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    names.put(rec.getInt("user"), rec.getString("value"));
                }
            });
            se.executeRecordHandler(sql2, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    long userId = rec.getInt("user");
                    String val = rec.getString("value");
                    if (names.containsKey(userId)) {
                        names.put(userId, names.get(userId) + " " + val);
                    } else {
                        names.put(userId, val);
                    }
                }
            });
        }
        return names;
    }

    private static Map<Long, String> getGroupNames(SQLExecutor se, Collection<Long> groupIds) {
        final HashMap<Long, String> names = new HashMap<Long, String>();
        if (CollectionUtils.isNotEmpty(groupIds)) {
            String sql = SQLTemplate.merge("SELECT `id`, `name` FROM group_ WHERE `id` IN (${group_ids})", "group_ids", StringUtils.join(groupIds, ","));
            se.executeRecordHandler(sql, new RecordHandler() {
                @Override
                public void handle(Record rec) {
                    names.put(rec.getInt("id"), rec.getString("name"));
                }
            });

        }
        return names;
    }
}
