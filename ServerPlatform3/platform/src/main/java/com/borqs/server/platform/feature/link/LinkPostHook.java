package com.borqs.server.platform.feature.link;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostHook;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.ThreadPool;
import com.borqs.server.platform.util.URLHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class LinkPostHook implements PostHook {
    private static final Logger L = Logger.get(LinkPostHook.class);

    private LinkLogic link;
    private StreamLogic stream;
    private ThreadPool threadPool;

    public LinkPostHook() {
    }

    public LinkPostHook(LinkLogic link) {
        this.link = link;
    }

    public LinkLogic getLink() {
        return link;
    }

    public void setLink(LinkLogic link) {
        this.link = link;
    }

    public void setStream(StreamLogic stream) {
        this.stream = stream;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    private static final String CACHE_URL_KEY = "cache_url";

    @Override
    public void before(Context ctx, Post post) {
        if (post == null)
            return;

        String url = null;
        if (post.getType() == Post.POST_LINK) {
            String[] attIds = post.getAttachmentIds();
            if (ArrayUtils.isNotEmpty(attIds)) {
                url = ObjectUtils.toString(attIds[0]);
            } else {
                url = catchUrl(post.getMessage());
            }
        } else if (post.getType() == Post.POST_TEXT) {
            url = catchUrl(post.getMessage());
            if (StringUtils.isNotEmpty(url))
                post.setType(Post.POST_LINK);
        }

        if (StringUtils.isNotEmpty(url)) {
            post.setAttachmentIds(new String[]{post.getType()+":"+url});
            ctx.putSession(CACHE_URL_KEY, url);
        }
    }

    private String catchUrl(Context ctx, String url) {
        LinkEntity le = null;
        try {
            le = link.get(ctx, url);
        } catch (Exception e) {
            L.warn(ctx, e, "Fetch link error '%s'", url);
        }
        return (le != null ? new LinkEntities(le).toJson(false) : "[]");
    }

    private String catchUrl(String s) {
        return URLHelper.catchHttpUrl(s);
    }

    private void catchUrlComplete(Context ctx, long postId, String attachments) {
        Post post = new Post(postId);
        post.setAttachments(attachments);
        post.setSourceId(ctx.getViewer());
        stream.updatePost(ctx, post);
    }

    @Override
    public void after(final Context ctx, final Post post) {
        final long postId = post.getPostId();
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                String url = (String) ctx.getSession(CACHE_URL_KEY, null);
                if (url != null) {
                    String attachments = catchUrl(ctx, url);
                    catchUrlComplete(ctx, postId, attachments);
                }
            }
        });
    }
}
