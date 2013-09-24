package com.borqs.server.impl.stream;

import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.cache.memcached.Memcached;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.feature.opline.OpLineLogic;
import com.borqs.server.platform.feature.stream.Post;
import com.borqs.server.platform.feature.stream.PostHook;
import com.borqs.server.platform.feature.stream.StreamLogic;
import com.borqs.server.platform.util.ArrayHelper;
import com.borqs.server.platform.util.DateHelper;
import org.codehaus.plexus.util.StringUtils;


public class StreamCreateHookImpl implements PostHook {
    private OpLineLogic opline;
    private StreamLogic post;
    private Memcached cache;

    public void setOpline(OpLineLogic opline) {
        this.opline = opline;
    }

    public void setPost(StreamLogic post) {
        this.post = post;
    }

    public void setCache(Memcached cache) {
        this.cache = cache;
    }

    @Override
    public void before(Context ctx, Post data) {
        // compose photo
        if (data != null && data.getPostTarget().type == Post.POST_PHOTO) {
            if(cache.getValue("photoPost")==null)
                return;
            Post post0 = (Post)cache.getValue("photoPost");
            long divideTime = DateHelper.nowMillis() - post0.getCreatedTime();

            if (divideTime < 1000 * 60 * 10) {
                Post oldPost = post.getPost(ctx, Post.STANDARD_COLUMNS, post0.getPostId());
                boolean b = checkPost(oldPost, data);
                if (b) {
                    //compose
                    /*JsonNode jn = JsonHelper.parse(data.getAttachments());
                    JsonNode jnOld = JsonHelper.parse(oldPost.getAttachments());
                    ArrayNode an = JsonNodeFactory.instance.arrayNode();
                    an.add(jnOld.get(0));
                    an.add(jn.get(0));
                    String attachments = an.toString();*/

                    //oldPost.setAttachments(attachments);
                    String[] attIds = data.getAttachmentIds();
                    if (attIds.length < 1)
                        throw new ServerException(E.DATA, "AttachmentIds is null!");

                    String id = attIds[0];
                    String[] attachmentIds = oldPost.getAttachmentIds();
                    oldPost.setAttachmentIds(ArrayHelper.addAsSet(attachmentIds, id));
                    post.updatePost(ctx, oldPost);

                    // hook should be modify with this param
                    ctx.putSession("return", true);
                }
            }
        }
    }

    private boolean checkPost(Post oldPost, Post newPost) {
        if (oldPost == null || newPost == null)
            return false;

        PeopleIds toOld = oldPost.getTo();
        PeopleIds toNew = newPost.getTo();
        boolean to = ArrayHelper.equalsAsSet(toOld.getUserIds(), toNew.getUserIds());

        PeopleIds addtoOld = oldPost.getAddTo();
        PeopleIds addtoNew = newPost.getAddTo();
        boolean addTo = ArrayHelper.equalsAsSet(addtoOld.getUserIds(), addtoNew.getUserIds());

        String messageOld = oldPost.getMessage();
        String messageNew = newPost.getMessage();
        boolean message = StringUtils.equals(messageOld, messageNew);

        Boolean CCOld = oldPost.getCanComment();
        Boolean CCNew = newPost.getCanComment();
        boolean CC = CCOld & CCNew;

        Boolean CQOld = oldPost.getCanQuote();
        Boolean CQNew = newPost.getCanQuote();
        boolean CQ = CQOld & CQNew;

        Boolean CLOld = oldPost.getCanLike();
        Boolean CLNew = newPost.getCanLike();
        boolean CL = CLOld & CLNew;

        return to & addTo & message & CC & CQ & CL;
    }

    @Override
    public void after(Context ctx, Post post) {
        // add photo post to memcached
        if (post != null && post.getPostTarget().type == Post.POST_PHOTO) {
            if (cache.getValue("photoPost") == null){
                cache.put("photoPost", post);
            }
            else{
                cache.delete("photoPost");
                cache.put("photoPost",post);
            }
        }

        //conversation
/*        Target target = new Target(Target.POST,post.getPostId()+"");
        conversationLogic.create(ctx,new ConversationBase(target, Reasons.STREAM_POST));

        //conversation add_to
        if(StringUtils.isNotEmpty(post.getAddTo())){
            conversationLogic.create(ctx,new ConversationBase(new Target(Target.POST,post.getPostId()+""),Reasons.STREAM_ADDTO));
        }
        //conversation to
        if(StringUtils.isNotEmpty(post.getTo())){
            conversationLogic.create(ctx,new ConversationBase(new Target(Target.POST,post.getPostId()+""),Reasons.STREAM_TO));
        }
        //conversation apk_share
        if(post.getType() == Post.POST_APK){
            conversationLogic.create(ctx,new ConversationBase(new Target(Target.APK,post.getPostId()+""),Reasons.APK_SHARE));
        }
        //conversation photo_share
        if(post.getType() == Post.POST_PHOTO){
            conversationLogic.create(ctx,new ConversationBase(new Target(Target.PHOTO,post.getPostId()+""),Reasons.PHOTO_SHARE));
        }*/

        //timeline

    }
}
