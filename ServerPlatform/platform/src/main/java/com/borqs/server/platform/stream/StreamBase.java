package com.borqs.server.platform.stream;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.RandomUtils;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Constants;
import com.borqs.server.service.platform.Stream;
import org.apache.avro.AvroRemoteException;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static com.borqs.server.service.platform.Constants.NULL_APP_ID;

public abstract class StreamBase extends RPCService implements Stream {
    public final Schema streamSchema = Schema.loadClassPath(StreamBase.class, "stream.schema");
    private static final Logger L = LoggerFactory.getLogger(StreamBase.class);
    protected StreamBase() {
    }

    @Override
    public final Class getInterface() {
        return Stream.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        streamSchema.loadAliases(getConfig().getString("schema.stream.alias", null));
    }

    @Override
    public void destroy() {
    }

    public static String genPostId() {
        return Long.toString(RandomUtils.generateId());
    }

    protected abstract boolean savePost(Record post);

    private static Record addPostIdStrCol(Record rec) {
        if (rec != null) {
            if (rec.has("post_id") && !rec.has("post_id_s"))
                rec.put("post_id_s", rec.getString("post_id"));
            if (rec.has("quote") && !rec.has("quote_s"))
                rec.put("quote_s", rec.getString("quote"));
        }
        return rec;
    }

    private static RecordSet addPostIdsStrCol(RecordSet recs) {
        if (recs != null) {
            for (Record rec : recs)
                addPostIdStrCol(rec);
        }
        return recs;
    }

    @Override
    public CharSequence createPost(CharSequence userId, ByteBuffer post) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            Record post0 = Record.fromByteBuffer(post);
            Schemas.checkRecordIncludeColumns(post0, "type", "message");

            long now = DateUtils.nowMillis();
            post0.put("created_time", now);
            post0.put("updated_time", now);
//            post0.putMissing("mentions", "");
            post0.putMissing("quote", 0L);
            post0.putMissing("root", 0L);
            post0.putMissing("app", NULL_APP_ID);
            post0.putMissing("attachments", "[]");
            post0.putMissing("app_data", "");
            post0.putMissing("can_comment", true);
            post0.putMissing("can_like", true);
            post0.putMissing("can_reshare", true);
            post0.putMissing("destroyed_time", 0);
            post0.putMissing("device", "");
            
            String postId = genPostId();
            post0.put("post_id", postId);
            post0.put("source", userId0);
            if (post0.getString("location").length()>0){
                String longitude = Constants.parseLocation(post0.getString("location"),"longitude");
                String latitude = Constants.parseLocation(post0.getString("location"),"latitude");
                post0.put("longitude",longitude);
                post0.put("latitude",latitude);
            }
            Schemas.standardize(streamSchema, post0);
            L.debug("long message,record post0="+post0.toString(false,false));
            boolean b = savePost(post0);
            if (!b)
                throw new StreamException("Save formPost error");

            return postId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean disablePosts(String userId, List<String> postIds);

    @Override
    public ByteBuffer destroyPosts(CharSequence userId, CharSequence postIds) throws AvroRemoteException, ResponseError {
        try {
            List<String> postIds0 = StringUtils2.splitList(toStr(postIds), ",", true);
            if (postIds0.isEmpty()) {
                return new RecordSet().toByteBuffer();
            }
            return Record.of("result", disablePosts(toStr(userId), postIds0)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record findPostTemp(String postId, List<String> cols);
    @Override
    public ByteBuffer findStreamTemp(CharSequence postId, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            return findPostTemp(toStr(postId), cols0).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    protected abstract RecordSet getPostsNearBy0(String viewerId, String cols, long since, long max, int type, String appId, int page, int count);
    @Override
    public ByteBuffer getPostsNearBy(CharSequence viewerId, CharSequence cols,long since, long max, int type, CharSequence appId, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getPostsNearBy0(toStr(viewerId), toStr(cols),since,max,type,toStr(appId),page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet topOneStreamByTarget0(int type, String target);
    
    @Override
    public ByteBuffer topOneStreamByTarget(int type, CharSequence target) throws AvroRemoteException, ResponseError {
        try {
            return addPostIdsStrCol(topOneStreamByTarget0(type, toStr(target))).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public CharSequence createRepost(CharSequence userId, CharSequence mentions,boolean secretly,CharSequence postId, CharSequence message, CharSequence device, CharSequence location,CharSequence appData,boolean can_comment,boolean can_like,boolean can_reshare,CharSequence add_to,CharSequence add_contact,boolean has_contact) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String postId0 = toStr(postId);
            String message0 = toStr(message);
             L.debug("**for long message test,send post,message0="+message0);
            Record rec = findPost(postId0, streamSchema.getNames());
            if (rec.isEmpty())
                throw new StreamException("The quote formPost '%s' is not exists", postId0);

            //if (rec.getString("source").equals(userId0))
            //    throw new StreamException("Can't repost by self");

            rec.put("source", userId0);
            rec.put("quote", postId0);
            rec.put("type", Constants.TEXT_POST);
            
            rec.put("root", rec.getInt("root") != 0 ? rec.getInt("root") : postId0);

            rec.put("message", message0);

            String newPostId = genPostId();
            rec.put("app_data",appData);
            rec.put("attachments", "[]");
            rec.put("mentions", toStr(mentions));
            rec.put("privince", secretly);
            rec.put("post_id", newPostId);
            long now = DateUtils.nowMillis();
            rec.put("created_time", now);
            rec.put("updated_time", now);
            rec.putMissing("app", NULL_APP_ID);
            rec.put("device", device);
            rec.put("location", location);
            rec.put("can_comment", can_comment);
            rec.put("can_like", can_like);
            rec.put("can_reshare", can_reshare);
            rec.put("add_to", add_to);
            rec.put("has_contact", has_contact);
            rec.put("add_contact", add_contact);
            if (rec.getString("location").length()>0){
                String longitude = Constants.parseLocation(rec.getString("location"),"longitude");
                String latitude = Constants.parseLocation(rec.getString("location"),"latitude");
                rec.put("longitude",longitude);
                rec.put("latitude",latitude);
            }
            boolean b = savePost(rec);
            if (!b)
                throw new StreamException("Save formPost error");

            return newPostId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePost(String userId, String postId, Record post);

    @Override
    public boolean updatePost(CharSequence userId, CharSequence postId, ByteBuffer post) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String postId0 = toStr(postId);
            Record post0 = Record.fromByteBuffer(post);
            Schemas.checkRecordColumnsIn(post0, "message", "can_comment", "can_like", "can_reshare");
            if (post0.has("message"))
                post0.put("updated_time", DateUtils.nowMillis());

            boolean b = updatePost(userId0, postId0, post0);
            if (!b)
                throw new StreamException("Update formPost error");
            return b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record findPost(String postId, List<String> cols);

    protected abstract RecordSet findPosts(List<String> postIds, List<String> cols);

    @Override
    public ByteBuffer getPosts(CharSequence postIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            List<String> postIds0 = StringUtils2.splitList(toStr(postIds), ",", true);
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            if (postIds0.isEmpty() || cols0.isEmpty())
                return new RecordSet().toByteBuffer();

            return addPostIdsStrCol(findPosts(postIds0, cols0)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean hasPost(CharSequence postId) throws AvroRemoteException, ResponseError {
        Record rec = findPost(toStr(postId), Arrays.asList("post_id"));
        return !rec.isEmpty();
    }

    protected abstract RecordSet getUsersPosts0(String viewerId , List<String> userIds, List<String> circleIds, List<String> cols, long since, long max, int type, String appId, int page, int count);

    @Override
    public ByteBuffer getUsersPosts(CharSequence viewerId, CharSequence userIds, CharSequence circleIds, CharSequence cols, long since, long max, int type, CharSequence appId, int page, int count) throws AvroRemoteException, ResponseError {
        if (count < 0 || count >= 1000)
            count = 1000;

        try {
            List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            
             List<String> circleId0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (cols0.isEmpty())
                return new RecordSet().toByteBuffer();

            Schemas.checkSchemaIncludeColumns(streamSchema, cols0.toArray(new String[cols0.size()]));
            return addPostIdsStrCol(getUsersPosts0(toStr(viewerId),userIds0,circleId0, cols0, since, max, type, toStr(appId), page, count)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getMySharePosts0(String viewerId , List<String> userIds, List<String> cols, long since, long max, int type, String appId, int page, int count);

    @Override
    public ByteBuffer getMySharePosts(CharSequence viewerId, CharSequence userIds,  CharSequence cols, long since, long max, int type, CharSequence appId, int page, int count) throws AvroRemoteException, ResponseError {
        if (count < 0 || count >= 100000)
            count = 100000;

        try {
            List<String> userIds0 = StringUtils2.splitList(toStr(userIds), ",", true);
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);

            if (cols0.isEmpty())
                return new RecordSet().toByteBuffer();

            Schemas.checkSchemaIncludeColumns(streamSchema, cols0.toArray(new String[cols0.size()]));
            return addPostIdsStrCol(getMySharePosts0(toStr(viewerId),userIds0,cols0, since, max, type, toStr(appId), page, count)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateAttachments(String post_id, String Attachments);
     //@Override
    public boolean updateAttachment(CharSequence post_id, CharSequence Attachments) throws AvroRemoteException, ResponseError {
         try{
            return updateAttachments(toStr(post_id),toStr(Attachments));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
     
    protected abstract RecordSet selectPosts(String sql);
    //@Override
    public ByteBuffer selectPostsBySql(CharSequence sql) throws ResponseError {
        try {
            return addPostIdsStrCol(selectPosts(toStr(sql))).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean touch0(String postId);

    @Override
    public boolean touch(CharSequence postId) throws AvroRemoteException {
        try {
            return touch0(toStr(postId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet myTopOneStreamByTarget0(String userId, int type, String target,List<String> cols);
            
    @Override
    public ByteBuffer myTopOneStreamByTarget(CharSequence userId,int type,CharSequence target,CharSequence cols) throws AvroRemoteException {
        try {
            List<String> cols0 = StringUtils2.splitList(toStr(cols), ",", true);
            return addPostIdsStrCol(myTopOneStreamByTarget0(toStr(userId),type,toStr(target),cols0)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }    
    
    protected abstract RecordSet findWhoSharedApp0(String packageName, int limit);
    protected abstract RecordSet findWhoRetweetStream0(String target, int limit);
    
    @Override
    public ByteBuffer findWhoSharedApp(CharSequence packageName,int limit) throws AvroRemoteException {
        try {
            return findWhoSharedApp0(toStr(packageName),limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    } 
    
    @Override
    public ByteBuffer findWhoRetweetStream(CharSequence target, int limit) throws AvroRemoteException, ResponseError {
    	try {
            return findWhoRetweetStream0(toStr(target),limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet topOneStreamBySetFriend0(int type, String source,long created_time);

    @Override
    public ByteBuffer topOneStreamBySetFriend(int type,CharSequence source, long created_time) throws AvroRemoteException, ResponseError {
    	try {
            return addPostIdsStrCol(topOneStreamBySetFriend0(type,toStr(source),created_time)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePostFor0(String post_id, String newPost_id, String Attachments,long created_time,long updated_time);

    @Override
    public CharSequence updatePostFor(CharSequence post_id, CharSequence Attachments, long created_time, long updated_time) throws AvroRemoteException {
        try {
            String new_postId = genPostId();
            boolean b = updatePostFor0(toStr(post_id), toStr(new_postId), toStr(Attachments), created_time, updated_time);
            return b ? new_postId : "";
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePostForAttachmentsAndUpdateTime0(String post_id, String Attachments, long updated_time);

    @Override
    public boolean updatePostForAttachmentsAndUpdateTime(CharSequence post_id, CharSequence Attachments,  long updated_time) throws AvroRemoteException {
        try {
            boolean b = updatePostForAttachmentsAndUpdateTime0(toStr(post_id),  toStr(Attachments),  updated_time);
            return b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet topSendStreamUser0(int limit);

    @Override
    public ByteBuffer topSendStreamUser(int limit) throws AvroRemoteException, ResponseError {
    	try {
            return topSendStreamUser0(limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getApkSharedToMe0(String viewerId, String userIds,boolean tome,String packageName,int page,int count);

    @Override
    public ByteBuffer getApkSharedToMe(CharSequence viewerId,CharSequence userIds,boolean tome,CharSequence packageName,int page,int count) throws AvroRemoteException, ResponseError {
    	try {
            return getApkSharedToMe0(toStr(viewerId),toStr(userIds),tome,toStr(packageName),page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePostForCommentOrLike0(String post_id,String viewerId,String column, int value);

    @Override
    public boolean updatePostForCommentOrLike(CharSequence post_id,CharSequence viewerId,CharSequence column,int value) throws AvroRemoteException, ResponseError {
    	try {
            return updatePostForCommentOrLike0(toStr(post_id),toStr(viewerId),toStr(column),value);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSharedPost0(String viewerId, String postId);

    @Override
    public ByteBuffer getSharedPost(CharSequence viewerId,CharSequence postId) throws AvroRemoteException, ResponseError {
    	try {
            return getSharedPost0(toStr(viewerId),toStr(postId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet formatOldDataToConversation0(String viewerId);
    protected abstract RecordSet formatOldDataToConversation1(String viewerId);
    protected abstract RecordSet formatOldDataToConversation2(String viewerId);
    @Override
    public ByteBuffer formatOldDataToConversation(CharSequence viewerId) throws AvroRemoteException, ResponseError {
    	try {
            formatOldDataToConversation2(toStr(viewerId)).toByteBuffer();
            formatOldDataToConversation1(toStr(viewerId)).toByteBuffer();
            return formatOldDataToConversation0(toStr(viewerId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getSharedCount0(String viewerId, String userId, int type)  ;

    @Override
    public int getSharedCount(CharSequence viewerId,CharSequence userId,int type) throws AvroRemoteException, ResponseError {
    	try {
            return getSharedCount0(toStr(viewerId),toStr(userId),type);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSharedPostHasContact11(String contact);

    @Override
    public ByteBuffer getSharedPostHasContact1(CharSequence contact) throws AvroRemoteException, ResponseError {
    	try {
            return getSharedPostHasContact11(toStr(contact)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSharedPostHasContact12(String contact);

    @Override
    public ByteBuffer getSharedPostHasContact2(CharSequence virtual_friendId) throws AvroRemoteException, ResponseError {
    	try {
            return getSharedPostHasContact12(toStr(virtual_friendId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updatePostHasContact12(String postId,String newMentions,String newAddContact,boolean newHasContact);

    @Override
    public boolean updatePostHasContact2(CharSequence postId,CharSequence newMentions,CharSequence newAddContact,boolean newHasContact) throws AvroRemoteException, ResponseError {
    	try {
            return updatePostHasContact12(toStr(postId),toStr(newMentions),toStr(newAddContact),newHasContact);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean formatLocation(String sql);

    @Override
    public boolean formatStreamLocation() throws AvroRemoteException, ResponseError {
    	try {
            return formatLocation("");
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getSharedByType0(String userIds,int type,String cols,int page,int count);

    @Override
    public ByteBuffer getSharedByType(CharSequence userIds,int type,CharSequence cols,int page,int count) throws AvroRemoteException, ResponseError {
    	try {
            return getSharedByType0(toStr(userIds),type,toStr(cols),page,count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet topOneStreamByShare0(int type, String source,String message, String mentions,int privince, long dateDiff);

    @Override
    public ByteBuffer topOneStreamByShare(int type, CharSequence source, CharSequence message, CharSequence mentions,int privince, long dateDiff) throws AvroRemoteException, ResponseError {
        try {
            return topOneStreamByShare0(type, toStr(source),toStr(message), toStr(mentions),privince, dateDiff).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean getPhoto0(String viewerId,String photo_id);

    @Override
    public boolean getPhoto(CharSequence viewerId,CharSequence photo_id) throws AvroRemoteException, ResponseError {
        try {
            return getPhoto0(toStr(viewerId),toStr(photo_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean getFile0(String viewerId,String file_id);

    @Override
    public boolean getFile(CharSequence viewerId,CharSequence file_id) throws AvroRemoteException, ResponseError {
        try {
            return getFile0(toStr(viewerId), toStr(file_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getVideo0(String viewerId,String file_id);

    @Override
    public ByteBuffer getVideo(CharSequence viewerId,CharSequence file_id) throws AvroRemoteException, ResponseError {
        try {
            return getVideo0(toStr(viewerId), toStr(file_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getAudio0(String viewerId, String file_id);

    @Override
    public ByteBuffer getAudio(CharSequence viewerId, CharSequence file_id) throws AvroRemoteException, ResponseError {
        try {
            return getAudio0(toStr(viewerId), toStr(file_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getStaticFile0(String viewerId, String file_id);

    @Override
    public ByteBuffer getStaticFile(CharSequence viewerId, CharSequence file_id) throws AvroRemoteException, ResponseError {
        try {
            return getStaticFile0(toStr(viewerId), toStr(file_id)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getAppliesToUser0(String viewerId, String appId, String userId, String cols) throws AvroRemoteException;

    @Override
    public ByteBuffer getAppliesToUser(CharSequence viewerId, CharSequence appId, CharSequence userId, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            return getAppliesToUser0(toStr(viewerId), toStr(appId), toStr(userId), toStr(cols)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
}
