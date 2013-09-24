package com.borqs.server.platform.friendship;


import com.borqs.server.base.ResponseError;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.data.Schemas;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Friendship;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.StringUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.borqs.server.service.platform.Constants.*;

public abstract class FriendshipBase extends RPCService implements Friendship {
    protected final Schema friendSchema = Schema.loadClassPath(FriendshipBase.class, "friend.schema");
    protected final Schema circleSchema = Schema.loadClassPath(FriendshipBase.class, "circle.schema");
    protected final Schema nameRemarkSchema = Schema.loadClassPath(FriendshipBase.class, "name_remark.schema");

    protected FriendshipBase() {
    }

    @Override
    public final Class getInterface() {
        return Friendship.class;
    }

    @Override
    public final Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        friendSchema.loadAliases(getConfig().getString("schema.friend.alias", null));
        circleSchema.loadAliases(getConfig().getString("schema.circle.alias", null));
        nameRemarkSchema.loadAliases(getConfig().getString("schema.name_remark.alias", null));
    }

    @Override
    public void destroy() {
    }

    protected abstract boolean saveCircle(Record circle);

    @Override
    public boolean createBuiltinCircles(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            saveCircle(Record.of("user", userId0, "name", "Blocked", "circle", BLOCKED_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Default", "circle", DEFAULT_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Address Book", "circle", ADDRESS_BOOK_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Family", "circle", FAMILY_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Closed Friends", "circle", CLOSE_FRIENDS_CIRCLE));
            saveCircle(Record.of("user", userId0, "name", "Acquaintance", "circle", ACQUAINTANCE_CIRCLE));
            return true;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence createCircle(CharSequence userId, CharSequence name) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String name0 = toStr(name);
            if (hasCircleName(userId0, name0))
                throw new FriendshipException("Circle name is exist (%s)", name0);

            int circleCount = getCircleCount(userId0);
            if (circleCount >= 100)
                throw new FriendshipException("Too many circle");

            int circleId = generateCustomCircleId(userId0);
            Record circle = Record.of("user", userId0, "name", name, "circle", circleId);
            Schemas.standardize(circleSchema, circle);
            boolean b = saveCircle(circle);
            if (!b)
                throw new FriendshipException("Save circle error");

            return toStr(circleId);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int generateCustomCircleId(String userId);

    protected abstract boolean destroyCircles0(String userId, String... circleIds);

    @Override
    public boolean destroyCircles(CharSequence userId, CharSequence circleIds) throws AvroRemoteException, ResponseError {
        try {
            String[] circlesId0 = StringUtils2.splitArray(toStr(circleIds), ",", true);
            for (String circleId : circlesId0) {
                if (Integer.parseInt(circleId) < 100)
                    return false;
            }
            return circlesId0.length == 0 || destroyCircles0(toStr(userId), circlesId0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateCircleName0(String userId, String circleId, String name);

    @Override
    public boolean updateCircleName(CharSequence userId, CharSequence circleId, CharSequence name) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String name0 = toStr(name);
            if (hasCircleName(userId0, name0))
                throw new FriendshipException("Circle name is exist (%s)", name0);

            return updateCircleName0(userId0, toStr(circleId), name0);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getCircleCount(String userId);

    protected abstract boolean hasCircleName(String userId, String circleName);

    protected abstract boolean hasActualCircle(String userId, String circleId);

    protected abstract RecordSet getCircles0(String userId, List<String> circleIds, boolean withMembers);

    @Override
    public ByteBuffer getCircles(CharSequence userId, CharSequence circleIds, boolean withMembers) throws AvroRemoteException, ResponseError {
        try {
            List<String> circlesIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            for (String circleId : circlesIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new FriendshipException("Invalidate circle ids");
            }

            return getCircles0(toStr(userId), circlesIds0, withMembers).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean updateCircleMemberCount0(String userId, String circleId,int member_count);

    @Override
    public boolean updateCircleMemberCount(CharSequence userId, CharSequence circleId,int member_count) throws AvroRemoteException, ResponseError {
        try {
            return updateCircleMemberCount0(toStr(userId), toStr(circleId),member_count);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateMyCircleMemberCount(CharSequence userId, CharSequence circleId) throws AvroRemoteException, ResponseError {
        try {
            return updateMyCircleMemberCount0(toStr(userId), toStr(circleId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract boolean setFriend0(String userId, String friendId, String circleId,int reason,boolean isadd);

    protected abstract boolean updateMyCircleMemberCount0(String userId, String circleId);

    protected abstract Record isDeleteRecent0(String userId, List<String> friendIds, long period);
    
    @Override
    public ByteBuffer isDeleteRecent(CharSequence userId, CharSequence friendIds, long period) throws AvroRemoteException, ResponseError
    {
    	try {
    		List<String> l0 = StringUtils2.splitList(toStr(friendIds), ",", true);
    		List<String> l = new ArrayList<String>();
    		for(String friend : l0)
    		{
    			l.add("delete_user_" + friend);
    		}
    		return isDeleteRecent0(toStr(userId), l, period).toByteBuffer();
    	} catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    @Override
    public boolean setFriends(CharSequence userId, CharSequence friendId, CharSequence circleId,int reason, boolean isadd) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);
            String circleId0 = toStr(circleId);
            if (!circleId0.equals("")) {
                if (!isActualCircle(Integer.parseInt(circleId0)) || !hasActualCircle(userId0, circleId0)) {
                    throw new FriendshipException("Circle id error (%s)", circleId);
                }
                if (!isadd) {
                    if (Integer.parseInt(circleId0) == BLOCKED_CIRCLE) {
                        List<String> circleIds0 = new ArrayList<String>();
                        circleIds0.add(circleId0);
                        circleIds0.retainAll(Arrays.asList(Integer.toString(BLOCKED_CIRCLE)));
                    }
                }
            }
            String friendId0 = toStr(friendId);
            if (StringUtils.isBlank(friendId0)) {
                return true;
            }

            return setFriend0(userId0, friendId0, circleId0,reason, isadd);
//            boolean b = updateMyCircleMemberCount0(userId0,circleId0);
//            return  b;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

//    protected abstract boolean setFriend1(String userId, String friendId, List<String> circleIds);

    @Override
    public boolean setFriend(CharSequence userId, CharSequence friendId, CharSequence circleIds, int reason) throws AvroRemoteException, ResponseError {
        try {
            String userId0 = toStr(userId);

            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.contains(Integer.toString(BLOCKED_CIRCLE))) {
                circleIds0.retainAll(Arrays.asList(Integer.toString(BLOCKED_CIRCLE)));
            }

            for (String circleId : circleIds0) {
                if (!isActualCircle(Integer.parseInt(circleId)) || !hasActualCircle(userId0, circleId)) {
                    throw new FriendshipException("Circle id error (%s)", circleId);
                }
            }

            String friendId0 = toStr(friendId);
            if (StringUtils.isBlank(friendId0)) {
                return true;
            }

            //find this guy in my old circles,then delete
            RecordSet recsf = getRelation0(toStr(friendId), toStr(userId), "");
            for (Record r : recsf) {
                setFriend0(toStr(userId), toStr(friendId), r.getString("circle_id"), reason, false);
            }

            for (String l : circleIds0) {
                setFriend0(userId0, friendId0, l, reason, true);
            }
//            boolean b = updateMyCircleMemberCount0(toStr(userId),"");
//            return b;
            return true;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getFriends0(String userId, List<String> circleIds, int page, int count);

    @Override
    public ByteBuffer getFriends(CharSequence userId, CharSequence circleIds, int page, int count) throws AvroRemoteException {
        try {
            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.isEmpty())
                return new RecordSet().toByteBuffer();

            for (String circleId : circleIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new FriendshipException("Invalid circle (%s)", circleId);
            }
            return getFriends0(toStr(userId), circleIds0, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getFollowers0(String userId, List<String> circleIds, int page, int count);

    @Override
    public ByteBuffer getFollowers(CharSequence userId, CharSequence circleIds, int page, int count) throws AvroRemoteException {
        try {
            List<String> circleIds0 = StringUtils2.splitList(toStr(circleIds), ",", true);
            if (circleIds0.isEmpty())
                return new RecordSet().toByteBuffer();

            for (String circleId : circleIds0) {
                if (!isFiniteCircle(Integer.parseInt(circleId)))
                    throw new FriendshipException("Invalid circle (%s)", circleId);
            }
            return getFollowers0(toStr(userId), circleIds0, page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getBothFriendsIds0(String viewerId, String userId, int page, int count);
    
    @Override
    public ByteBuffer getBothFriendsIds(CharSequence viewerId, CharSequence userId, int page, int count) throws AvroRemoteException {
        try {
            return getBothFriendsIds0(toStr(viewerId), toStr(userId), page, count).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    
    protected abstract RecordSet getRelation0(String sourceUserId, String targetUserId, String circleId);

    @Override
    public ByteBuffer getRelation(CharSequence sourceUserId, CharSequence targetUserId, CharSequence circleId) throws AvroRemoteException {
        try {
            String circleId0 = toStr(circleId);
            if (StringUtils.isNotBlank(circleId0) && !isFiniteCircle(Integer.parseInt(circleId0)))
                throw new FriendshipException("Invalid circle (%s)", circleId);
            return getRelation0(toStr(sourceUserId), toStr(targetUserId), circleId0).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean getIfHeInMyCircles0(String my_id,String other_id,String circle_id);

    @Override
    public boolean getIfHeInMyCircles(CharSequence my_id, CharSequence other_id, CharSequence circle_id) throws AvroRemoteException {
        try {
            return getIfHeInMyCircles0(toStr(my_id), toStr(other_id), toStr(circle_id));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getBidiRelation(CharSequence sourceUserId, CharSequence targetUserId, CharSequence circleId) throws AvroRemoteException {
        try {
            String circleId0 = toStr(circleId);
            if (!isFiniteCircle(Integer.parseInt(circleId0)))
                throw new FriendshipException("Invalid circle (%s)", circleId);

            String sourceUserId0 = toStr(sourceUserId);
            String targetUserId0 = toStr(targetUserId);
            Record rec = new Record();
            rec.put("relation1", getRelation0(sourceUserId0, targetUserId0, circleId0));
            rec.put("relation2", getRelation0(targetUserId0, sourceUserId0, circleId0));
            return rec.toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean setRemark0(String userId, String friendId, String remark);

    @Override
    public boolean setRemark(CharSequence userId, CharSequence friendId, CharSequence remark) throws AvroRemoteException {
        try {
            return setRemark0(toStr(userId), toStr(friendId), toStr(remark));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getRemarks0(String userId, String... friendIds);

    @Override
    public ByteBuffer getRemarks(CharSequence userId, CharSequence friendIds) throws AvroRemoteException {
        try {
            String[] friendIds0 = StringUtils2.splitArray(toStr(friendIds), ",", true);
            if (friendIds0.length == 0)
                return new RecordSet().toByteBuffer();

            return getRemarks0(toStr(userId), friendIds0).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getFollowersCount0(String userId);

    @Override
    public int getFollowersCount(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getFollowersCount0(toStr(userId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract int getFriendsCount0(String userId);

    @Override
    public int getFriendsCount(CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getFriendsCount0(toStr(userId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getFriendOrFollowers0(String userIds, String byFriendOrFollowers);

    @Override
    public ByteBuffer getFriendOrFollowers(CharSequence userIds, CharSequence byFriendOrFollowers) throws AvroRemoteException {
        try {
            return getFriendOrFollowers0(toStr(userIds), toStr(byFriendOrFollowers)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getAllRelation0(String viewerId, String userIds, String circleId, String inTheirOrInMine);

     @Override
    public ByteBuffer getAllRelation(CharSequence viewerId, CharSequence userIds, CharSequence circleId, CharSequence inTheirOrInMine) throws AvroRemoteException {
        try {
            return getAllRelation0(toStr(viewerId), toStr(userIds), toStr(circleId), toStr(inTheirOrInMine)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet topuserFollowers0(long userId,int limit);

     @Override
    public ByteBuffer topuserFollowers(long userId,int limit) throws AvroRemoteException {
        try {
            return topuserFollowers0(userId,limit).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract Record getMyFriends0(String userId, String friendId);

     @Override
    public ByteBuffer getMyFriends(CharSequence userId,CharSequence friendId) throws AvroRemoteException {
        try {
            return getMyFriends0(toStr(userId),toStr(friendId)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }
    protected abstract boolean deleteVirtualFriend0(String userId,String friendId,String name,String content);

    protected abstract RecordSet getExistVirtualFriendId0(String userId,String virtualFriendId);

    protected abstract boolean deleteVirtualFriendId0(String friendIds,String content);

    protected abstract boolean setContactFriend0(String userId,String friendId,String fname, String content, String circleId, int reason, boolean isadd);

    @Override
    public boolean setContactFriend(CharSequence userId,CharSequence friendId,CharSequence fname,CharSequence content,CharSequence circleIds,int reason,boolean isadd,boolean deleteOld) throws AvroRemoteException {
        try {
            if (deleteOld) {
                deleteVirtualFriend0(toStr(userId), toStr(friendId), toStr(fname), toStr(content));
            }
            List<String> ll = StringUtils2.splitList(toStr(circleIds), ",", true);
            for (String l : ll){
                 setContactFriend0(toStr(userId),toStr(friendId),toStr(fname),toStr(content),toStr(l),reason,isadd);
            }
            RecordSet recs =  getExistVirtualFriendId0(toStr(userId), toStr(friendId)) ;
            if (recs.size()==0){
                deleteVirtualFriendId0(toStr(friendId),toStr(content));
            }
            return true;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean createVirtualFriendId0(String userId,String friendId,String content,String name);

    @Override
    public boolean createVirtualFriendId(CharSequence userId,CharSequence friendId,CharSequence content,CharSequence name) throws AvroRemoteException {
        try {
            return createVirtualFriendId0(toStr(userId),toStr(friendId),toStr(content),toStr(name));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract boolean updateVirtualFriendIdToAct0(String friendId,String content);

    @Override
    public boolean updateVirtualFriendIdToAct(CharSequence friendId,CharSequence content) throws AvroRemoteException {
        try {
            return updateVirtualFriendIdToAct0(toStr(friendId),toStr(content));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String getUserFriendhasVirtualFriendId0(String userId,String content);

    @Override
    public CharSequence getUserFriendhasVirtualFriendId(CharSequence userId,CharSequence content) throws AvroRemoteException {
        try {
            return toStr(getUserFriendhasVirtualFriendId0(toStr(userId), toStr(content)));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getVirtualFriendId0(String content);

    @Override
    public ByteBuffer getVirtualFriendId(CharSequence content) throws AvroRemoteException {
        try {
            return getVirtualFriendId0(toStr(content)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getContactFriendO(String userIds);

    @Override
    public ByteBuffer getContactFriend(CharSequence userIds) throws AvroRemoteException {
        try {
            return getContactFriendO(toStr(userIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getContactFriendByFidO(String friendIds);

    @Override
    public ByteBuffer getContactFriendByFid(CharSequence friendIds) throws AvroRemoteException {
        try {
            return getContactFriendByFidO(toStr(friendIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract RecordSet getVirtualFriendIdByName0(String userId,String name);

    @Override
    public ByteBuffer getVirtualFriendIdByName(CharSequence userId,CharSequence name) throws AvroRemoteException {
        try {
            return getVirtualFriendIdByName0(toStr(userId),toStr(name)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

}
