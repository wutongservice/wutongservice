package com.borqs.server.platform.group;

import com.borqs.server.base.ResponseError;
import com.borqs.server.base.conf.Configuration;
import com.borqs.server.base.data.*;
import com.borqs.server.base.rpc.RPCService;
import com.borqs.server.base.util.DateUtils;
import com.borqs.server.base.util.Errors;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.service.platform.Group;
import org.apache.avro.AvroRemoteException;
import org.apache.commons.lang.ObjectUtils;

import java.nio.ByteBuffer;

import static com.borqs.server.platform.group.GroupConstants.*;
import static com.borqs.server.service.platform.Constants.GENERAL_GROUP_ID_BEGIN;
import static com.borqs.server.service.platform.Constants.EVENT_ID_BEGIN;

public abstract class GroupBase extends RPCService implements Group {
    protected final Schema groupSchema = Schema.loadClassPath(GroupBase.class, "group.schema");

    protected final Schema notifSchema = Schema.loadClassPath(GroupBase.class, "notif.schema");

    protected abstract boolean saveGroup(Record info, Record properties);

    protected abstract long generateGroupId(long begin, String type);

    protected abstract boolean updateGroup0(long groupId, Record info, Record properties);

    protected abstract boolean deleteGroups(String groupIds);

    protected abstract RecordSet getGroups(long begin, long end, long[] groupIds, String... cols);

    protected abstract RecordSet findGroupsByMember0(long begin, long end, long member, String... cols);

    protected abstract String findGroupIdsByMember0(long begin, long end, long member);

    protected abstract String findGroupIdsByTopPost0(String postId);

    protected abstract RecordSet findGroupsByName0(long begin, long end, String name, String... cols);

    protected abstract boolean addOrGrantMembers(long groupId, Record memberRoles);

    protected abstract boolean deleteMembers(long groupId, String members);

    protected abstract boolean hasRight0(long groupId, long member, int minRole);

    protected abstract String getMembersByRole0(long groupId, int role, int page, int count, String searchKey);

    protected abstract String getMembers0(String groupIds, int page, int count);

    protected abstract int getMembersCount0(long groupId);

    protected abstract Record getMembersCounts0(String groupIds);

    protected abstract boolean addOrUpdatePendings0(long groupId, RecordSet statuses);

    protected abstract RecordSet getPendingUserByStatus0(long groupId, long source, String status, int page, int count, String searchKey);

    protected abstract int getUserStatusById0(long groupId, long userId);

    protected abstract int getUserStatusByIdentify0(long groupId, String identify);

    protected abstract Record getUserStatusByIds0(long groupId, String userIds);

    protected abstract Record getUserStatusByIdentifies0(long groupId, String identifies);

    protected abstract Record getUsersCounts0(String groupIds, int status);

    protected abstract boolean updateUserIdByIdentify0(String userId, String identify);

    protected abstract String getSourcesById0(long groupId, String userId);

    protected abstract String getSourcesByIdentify0(long groupId, String identify);

    protected abstract boolean defaultMemberNotification0(long groupId, String userIds);

    protected abstract boolean updateMemberNotification0(long groupId, String userId, Record notif);

    protected abstract RecordSet getMembersNotification0(long groupId, String userIds);

    protected abstract RecordSet getGroupUsersByStatus0(long groupId, String status, int page, int count, String searchKey);

    @Override
    public long createGroup(long begin, CharSequence type, CharSequence name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers,
                            int canJoin, int canMemberInvite, int canMemberApprove, int canMemberPost, int canMemberQuit, int needInvitedConfirm, long creator, CharSequence label, ByteBuffer properties) throws AvroRemoteException, ResponseError {
        try {
            if (memberLimit <= 0)
                throw new DataException("Invalid member limit");

            long groupId = generateGroupId(begin, toStr(type));
            Record info = new Record();
            info.put(COL_ID, groupId);
            info.put(COL_NAME, name);
            info.put(COL_MEMBER_LIMIT, memberLimit);
            info.put(COL_IS_STREAM_PUBLIC, isStreamPublic);
            info.put(COL_CAN_SEARCH, canSearch);
            info.put(COL_CAN_VIEW_MEMBERS, canViewMembers);
            info.put(COL_CAN_JOIN, canJoin);
            info.put(COL_CAN_MEMBER_INVITE, canMemberInvite);
            info.put(COL_CAN_MEMBER_APPROVE, canMemberApprove);
            info.put(COL_CAN_MEMBER_POST, canMemberPost);
            info.put(COL_CAN_MEMBER_QUIT, canMemberQuit);
            info.put(COL_NEED_INVITED_CONFIRM, needInvitedConfirm);
            info.put(COL_CREATOR, creator);
            info.put(COL_LABEL, label);
            info.put(COL_CREATED_TIME, DateUtils.nowMillis());
            info.put(COL_UPDATED_TIME, DateUtils.nowMillis());

            boolean r = saveGroup(info, Record.fromByteBuffer(properties));
            if (!r)
                throw new GroupException("save public circle error");

            return groupId;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateGroup(long groupId, ByteBuffer info, ByteBuffer properties) throws AvroRemoteException, ResponseError {
        try {
            Record group = Record.fromByteBuffer(info);
            Schemas.checkRecordColumnsIn(group, COL_NAME, COL_MEMBER_LIMIT, COL_IS_STREAM_PUBLIC, COL_CAN_SEARCH,
                    COL_CAN_VIEW_MEMBERS, COL_CAN_JOIN, COL_CAN_MEMBER_INVITE, COL_CAN_MEMBER_APPROVE, COL_CAN_MEMBER_POST, COL_CAN_MEMBER_QUIT, COL_NEED_INVITED_CONFIRM, COL_LABEL);
            group.put(COL_UPDATED_TIME, DateUtils.nowMillis());

            return updateGroup0(groupId, group, Record.fromByteBuffer(properties));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean destroyGroup(CharSequence groupIds) throws AvroRemoteException, ResponseError {
        try {
            return deleteGroups(toStr(groupIds));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getGroup(long groupId, CharSequence cols) throws AvroRemoteException, ResponseError {
        return RecordSet.fromByteBuffer(getGroups(0, 0, ObjectUtils.toString(groupId), cols))
                .getFirstRecord().toByteBuffer();
    }

    @Override
    public ByteBuffer getGroups(long begin, long end, CharSequence groupIds, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            return getGroups(begin, end, StringUtils2.splitIntArray(toStr(groupIds), ","),
                    StringUtils2.splitArray(toStr(cols), ",", true)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer findGroupsByMember(long begin, long end, long member, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            return findGroupsByMember0(begin, end, member, StringUtils2.splitArray(toStr(cols), ",", true)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence findGroupIdsByMember(long begin, long end, long member) throws AvroRemoteException, ResponseError {
        try {
            return findGroupIdsByMember0(begin, end, member);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence findGroupIdsByTopPost(CharSequence postId) throws AvroRemoteException, ResponseError {
        try {
            return findGroupIdsByTopPost0(toStr(postId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer findGroupsByName(long begin, long end, CharSequence name, CharSequence cols) throws AvroRemoteException, ResponseError {
        try {
            return findGroupsByName0(begin, end, toStr(name), StringUtils2.splitArray(toStr(cols), ",", true)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean addMember(long groupId, long member, int role) throws AvroRemoteException, ResponseError {
        try {
            String userId = ObjectUtils.toString(member);
            boolean r1 = addOrGrantMembers(groupId, Record.of(userId, role));
            boolean r2 = defaultMemberNotification(groupId, userId);
            return r1 && r2;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean addMembers(long groupId, ByteBuffer roles) throws AvroRemoteException, ResponseError {
        try {
            Record rec = Record.fromByteBuffer(roles);
            String userIds = StringUtils2.joinIgnoreBlank(",", rec.keySet());
            boolean r1 = addOrGrantMembers(groupId, rec);
            boolean r2 = defaultMemberNotification(groupId, userIds);
            return r1 && r2;
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }


    @Override
    public boolean removeMembers(long groupId, CharSequence members) throws AvroRemoteException, ResponseError {
        try {
            return deleteMembers(groupId, toStr(members));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean grant(long groupId, long member, int role) throws AvroRemoteException, ResponseError {
        try {
            return addOrGrantMembers(groupId, Record.of(ObjectUtils.toString(member), role));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean grants(long groupId, ByteBuffer roles) throws AvroRemoteException, ResponseError {
        try {
            return addOrGrantMembers(groupId, Record.fromByteBuffer(roles));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean hasRight(long groupId, long member, int minRole) throws AvroRemoteException, ResponseError {
        try {
            return hasRight0(groupId, member, minRole);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getMembersByRole(long groupId, int role, int page, int count, CharSequence searchKey) throws AvroRemoteException, ResponseError {
        try {
            return getMembersByRole0(groupId, role, page, count, toStr(searchKey));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getAdmins(long groupId, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getMembersByRole0(groupId, ROLE_ADMIN, page, count, "");
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public long getCreator(long groupId) throws AvroRemoteException, ResponseError {
        try {
            return Long.parseLong(getMembersByRole0(groupId, ROLE_CREATOR, -1, -1, ""));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getAllMembers(long groupId, int page, int count, CharSequence searchKey) throws AvroRemoteException, ResponseError {
        try {
            return getMembersByRole0(groupId, 0, page, count, toStr(searchKey));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getMembers(CharSequence groupIds, int page, int count) throws AvroRemoteException, ResponseError {
        try {
            return getMembers0(toStr(groupIds), page, count);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public int getMembersCount(long groupId) throws AvroRemoteException, ResponseError {
        try {
            return getMembersCount0(groupId);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getMembersCounts(CharSequence groupIds) throws AvroRemoteException, ResponseError {
        try {
            return getMembersCounts0(toStr(groupIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUsersCounts(CharSequence groupIds, int status) throws AvroRemoteException, ResponseError {
        try {
            return getUsersCounts0(toStr(groupIds), status).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean addOrUpdatePendings(long groupId, ByteBuffer statuses) throws AvroRemoteException, ResponseError {
        try {
            return addOrUpdatePendings0(groupId, RecordSet.fromByteBuffer(statuses));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getPendingUsersByStatus(long groupId, long source, CharSequence status, int page, int count, CharSequence searchKey) throws AvroRemoteException, ResponseError {
        try {
            return getPendingUserByStatus0(groupId, source, toStr(status), page, count, toStr(searchKey)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public int getUserStatusById(long groupId, long userId) throws AvroRemoteException, ResponseError {
        try {
            return getUserStatusById0(groupId, userId);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public int getUserStatusByIdentify(long groupId, CharSequence identify) throws AvroRemoteException, ResponseError {
        try {
            return getUserStatusByIdentify0(groupId, toStr(identify));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUserStatusByIds(long groupId, CharSequence userIds) throws AvroRemoteException, ResponseError {
        try {
            return getUserStatusByIds0(groupId, toStr(userIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getUserStatusByIdentifies(long groupId, CharSequence identifies) throws AvroRemoteException, ResponseError {
        try {
            return getUserStatusByIdentifies0(groupId, toStr(identifies)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateUserIdByIdentify(CharSequence userId, CharSequence identify) throws AvroRemoteException, ResponseError {
        try {
            return updateUserIdByIdentify0(toStr(userId), toStr(identify));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getSourcesById(long groupId, CharSequence userId) throws AvroRemoteException, ResponseError {
        try {
            return getSourcesById0(groupId, toStr(userId));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public CharSequence getSourcesByIdentify(long groupId, CharSequence identify) throws AvroRemoteException, ResponseError {
        try {
            return getSourcesByIdentify0(groupId, toStr(identify));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean defaultMemberNotification(long groupId, CharSequence userIds) throws AvroRemoteException, ResponseError {
        try {
            return defaultMemberNotification0(groupId, toStr(userIds));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean updateMemberNotification(long groupId, CharSequence userId, ByteBuffer notif) throws AvroRemoteException, ResponseError {
        try {
            return updateMemberNotification0(groupId, toStr(userId), Record.fromByteBuffer(notif));
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getMembersNotification(long groupId, CharSequence userIds) throws AvroRemoteException, ResponseError {
        try {
            return getMembersNotification0(groupId, toStr(userIds)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public ByteBuffer getGroupUsersByStatus(long groupId, CharSequence status, int page, int count, CharSequence searchKey) throws AvroRemoteException, ResponseError {
        try {
            return getGroupUsersByStatus0(groupId, toStr(status), page, count, toStr(searchKey)).toByteBuffer();
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    protected abstract String findGroupIdsByProperty0(String propKey, String propVal, int max);

    @Override
    public CharSequence findGroupIdsByProperty(CharSequence propKey, CharSequence propVal, int max) throws AvroRemoteException, ResponseError {
        try {
            return findGroupIdsByProperty0(toStr(propKey), toStr(propVal), max);
        } catch (Throwable t) {
            throw Errors.wrapResponseError(t);
        }
    }

    @Override
    public boolean isGroup(long id) throws AvroRemoteException, ResponseError {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id <= GROUP_ID_END;
    }

    @Override
    public boolean isPublicCircle(long id) throws AvroRemoteException, ResponseError {
        return id >= PUBLIC_CIRCLE_ID_BEGIN && id < ACTIVITY_ID_BEGIN;
    }

    @Override
    public boolean isActivity(long id) throws AvroRemoteException, ResponseError {
        return id >= ACTIVITY_ID_BEGIN && id < DEPARTMENT_ID_BEGIN;
    }

    @Override
    public boolean isOrganization(long id) throws AvroRemoteException, ResponseError {
        return id >= DEPARTMENT_ID_BEGIN && id < GENERAL_GROUP_ID_BEGIN;
    }

    @Override
    public boolean isGeneralGroup(long id) throws AvroRemoteException, ResponseError {
        return id >= GENERAL_GROUP_ID_BEGIN && id < EVENT_ID_BEGIN;
    }

    @Override
    public boolean isEvent(long id) throws AvroRemoteException, ResponseError {
        return id >= EVENT_ID_BEGIN && id < EVENT_ID_END;
    }

    @Override
    public boolean isSpecificType(long id, CharSequence type) throws AvroRemoteException, ResponseError {
        Configuration conf = getConfig();
        long begin = conf.getInt("group." + toStr(type) + ".begin", GENERAL_GROUP_ID_BEGIN);
        long end = conf.getInt("group." + toStr(type) + ".end", EVENT_ID_BEGIN);
        return id >= begin && id < end;
    }

    @Override
    public Class getInterface() {
        return Group.class;
    }

    @Override
    public Object getImplement() {
        return this;
    }

    @Override
    public void init() {
        groupSchema.loadAliases(getConfig().getString("schema.group.alias", null));
    }

    @Override
    public void destroy() {

    }
}
