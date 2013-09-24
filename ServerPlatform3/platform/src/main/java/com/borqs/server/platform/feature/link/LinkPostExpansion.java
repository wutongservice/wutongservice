package com.borqs.server.platform.feature.link;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostExpansion;
import com.borqs.server.platform.feature.stream.Posts;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class LinkPostExpansion implements PostExpansion {
    private static final Logger L = Logger.get(LinkPostExpansion.class);

    private String prefix;

    public LinkPostExpansion() {
    }

    public LinkPostExpansion(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void expand(Context ctx, String[] expCols, Posts data) {
        if (CollectionUtils.isEmpty(data))
            return;

        if (expCols == null || ArrayUtils.contains(expCols, Post.COL_ATTACHMENTS))
            expandLinkAttachments(ctx, data);
    }

    private void expandLinkAttachments(Context ctx, Posts posts) {
        try {
            for (Post post : posts) {
                if (post.getType() != Post.POST_LINK)
                    continue;

                String attachments = post.getAttachments();
                if (StringUtils.isNotEmpty(attachments) && !StringUtils.equals(attachments, "[]")) {
                    LinkEntities les = LinkEntities.fromJson(post.getAttachments());
                    for (LinkEntity le : les) {
                        String[] imageUrls = le.getImageUrls();
                        if (ArrayUtils.isNotEmpty(imageUrls)) {
                            for (int i = 0; i < imageUrls.length; i++) {
                                imageUrls[i] = addImageUrlPrefix(imageUrls[i]);
                            }
                            le.setImageUrls(imageUrls);
                        }
                    }
                    attachments = les.toJson(true);
                    post.setAttachments(attachments);
                }
            }
        } catch (Exception e) {
            L.warn(ctx, e, "Expand link image url error");
        }
    }

    private String addImageUrlPrefix(String imageUrl) {
        if (StringUtils.isBlank(imageUrl))
            return imageUrl;

        String fileName = imageUrl.contains("/") ? StringUtils.substringAfterLast(imageUrl, "/") : imageUrl;
        String px = prefix;
        if (!px.endsWith("/"))
            px = px + "/";

        return StringHelper.addPrefix(fileName, px, false);
    }
}
