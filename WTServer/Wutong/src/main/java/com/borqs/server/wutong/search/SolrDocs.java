package com.borqs.server.wutong.search;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;



public class SolrDocs extends DocSupport {

    public static SolrInputDocument createPostDoc(PostDoc postDoc) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(FIELD_ID, makePostId(postDoc.getId()));
        doc.addField(FIELD_OBJECT_TYPE, "post");
        doc.addField(FIELD_CREATED_TIME, postDoc.getCreatedTime());
        doc.addField(FIELD_UPDATED_TIME, postDoc.getUpdatedTime());

        addNotBlankText(doc, FIELD_FROM, withPinyin(postDoc.getFrom()), 1.2f);
        addNotNullId(doc, FIELD_FROM_ID, postDoc.getFromId());

        addNotBlankText(doc, FIELD_TO, joinWithPinyin(postDoc.getTo()), 1.1f);
        addNotNullIds(doc, FIELD_TO_ID, postDoc.getToIds());

        addNotBlankText(doc, FIELD_ADDTO, joinWithPinyin(postDoc.getAddTos()), 1.1f);
        addNotNullIds(doc, FIELD_ADDTO_ID, postDoc.getAddToIds());

        addNotBlankText(doc, FIELD_GROUP, join(postDoc.getGroups()), 1.1f);
        addNotNullIds(doc, FIELD_GROUP_ID, postDoc.getGroupIds());

        addNotNullId(doc, FIELD_CATEGORY_ID, postDoc.getCategoryId());
        addNotBlankText(doc, FIELD_CATEGORY, postDoc.getCategory(), 1.3f);

        addNotBlankStrings(doc, FIELD_TAGS, postDoc.getTags());

        doc.addField(FIELD_PRIVATE, postDoc.isPrivate_() ? 1 : 0);

        addNotBlankText(doc, FIELD_CONTENT, postDoc.getMessage(), 1.0f);
        return doc;
    }


    private static void addNotNullId(SolrInputDocument doc, String field, long id) {
        if (id > 0)
            doc.addField(field, id);
    }

    private static void addNotBlankText(SolrInputDocument doc, String field, String val, float boost) {
        if (StringUtils.isNotBlank(val))
            doc.addField(field, val, boost);
    }

    private static void addNotBlankTexts(SolrInputDocument doc, String field, String[] vals, float boost) {
        if (vals == null)
            return;

        for (String val : vals) {
            if (StringUtils.isNotBlank(val))
                doc.addField(field, val, boost);
        }
    }

    private static void addNotNullIds(SolrInputDocument doc, String field, long[] ids) {
        if (ids == null)
            return;

        for (long id : ids) {
            if (id > 0)
                doc.addField(field, id);
        }
    }

    private static void addNotBlankStrings(SolrInputDocument doc, String field, String[] vals) {
        if (vals == null)
            return;

        for (String val : vals) {
            if (StringUtils.isNotBlank(val))
                doc.addField(field, val);
        }
    }
}
