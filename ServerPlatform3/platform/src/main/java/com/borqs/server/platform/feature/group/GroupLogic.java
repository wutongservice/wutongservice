package com.borqs.server.platform.feature.group;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.feature.friend.PeopleIds;
import com.borqs.server.platform.logic.Logic;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public interface GroupLogic extends Logic {



    //    long createGroup(long begin, string type, string name, int memberLimit, int isStreamPublic, int canSearch, int canViewMembers, int canJoin, int canMemberInvite, int canMemberApprove, int canMemberPost, long creator, string label, bytes properties) throws com.borqs.server.base.ResponseError;
    long createGroup(Context ctx, String type, String name, GroupOptions options);
    //    boolean updateGroup(long groupId, bytes info, bytes properties) throws com.borqs.server.base.ResponseError;
    boolean updateGroup(Context ctx, long groupId, Group group, Record properties);
    //    boolean destroyGroup(string groupIds) throws com.borqs.server.base.ResponseError;
    boolean destroyGroup(Context ctx, long[] groupIds);

    //
    //    bytes getGroup(long groupId, string cols) throws com.borqs.server.base.ResponseError;
    Group getGroup(Context ctx, long groupId);
    //    bytes getGroups(long begin, long end, string groupIds, string cols) throws com.borqs.server.base.ResponseError;
    Groups getGroups(Context ctx, long[] groupIds, GroupIdRange idRange);

    //    bytes findGroupsByMember(long begin, long end, long member, string cols) throws com.borqs.server.base.ResponseError;
    Groups findGroupsByMember(Context ctx, GroupIdRange idRange, long member);
    //    string findGroupIdsByMember(long begin, long end, long member) throws com.borqs.server.base.ResponseError;
    long[] findGroupIdsByMember(Context ctx, GroupIdRange idRange, long member);
    //    bytes findGroupsByName(long begin, long end, string name, string cols) throws com.borqs.server.base.ResponseError;
    Groups findGroupsByName(Context ctx, String name, GroupIdRange idRange);
    //
    //    boolean addMember(long groupId, long member, int role) throws com.borqs.server.base.ResponseError;
    boolean addMember(Context ctx, long groupId, long member, int role);
    //    boolean addMembers(long groupId, bytes roles) throws com.borqs.server.base.ResponseError;
    boolean addMembers(Context ctx, long groupId, Map<Long, Integer> roles); // TODO: ???
    //    boolean removeMembers(long groupId, string members) throws com.borqs.server.base.ResponseError;
    boolean removeMembers(Context ctx, long groupId, long[] memberIds);
    //
    //    boolean grant(long groupId, long member, int role) throws com.borqs.server.base.ResponseError;
    boolean grant(Context ctx, long groupId, long member, int role);
    //    boolean grants(long groupId, bytes roles) throws com.borqs.server.base.ResponseError;
    boolean grants(Context ctx, long groupId, Map<Long, Integer> roles);
    //    string getMembersByRole(long groupId, int role, int page, int count) throws com.borqs.server.base.ResponseError;
    long[] getMembersByRole(Context ctx, long groupId, int role, Page page);
    //    string getAdmins(long groupId, int page, int count) throws com.borqs.server.base.ResponseError;
    long[] getAdmins(Context ctx, long groupId, Page page);

    //    long getCreator(long groupId) throws com.borqs.server.base.ResponseError;
    long getCreator(Context ctx, long groupId);
    //    string getAllMembers(long groupId, int page, int count) throws com.borqs.server.base.ResponseError;
    long[] getAllMembers(Context ctx, long groupId, Page page);
    //    string getMembers(string groupIds, int page, int count) throws com.borqs.server.base.ResponseError;
    long[] getMembers(Context ctx, long[] groupIds, Page page);
    //    int getMembersCount(long groupId) throws com.borqs.server.base.ResponseError;
    int getMembersCount(Context ctx, long groupId);
    //    bytes getMembersCounts(string groupIds) throws com.borqs.server.base.ResponseError;
    Map<Long, Integer> getMembersCounts(Context ctx, long[] groupIds);
    //    boolean hasRight(long groupId, long member, int minRole) throws com.borqs.server.base.ResponseError;
    boolean hasRight(Context ctx, long groupId, long member, int minRole);
    //
    //    boolean addOrUpdatePendings(long groupId, bytes statuses) throws com.borqs.server.base.ResponseError;
    boolean addOrUpdatePendings(Context ctx, long groupId, List<GroupPending> statuses);
    //    bytes getPendingUsersByStatus(long groupId, long source, string status, int page, int count) throws com.borqs.server.base.ResponseError;
    GroupPendings getPendingUsersByStatus(Context ctx, long groupId, long source, String status, Page page);
    //    int getUserStatusById(long groupId, long userId) throws com.borqs.server.base.ResponseError;
    int getUserStatusById(Context ctx, long groupId, long userId);
    //    int getUserStatusByIdentify(long groupId, string identify) throws com.borqs.server.base.ResponseError;
    int getUserStatusByIdentify(Context ctx, long groupId, String identify);
    //    bytes getUserStatusByIds(long groupId, string userIds) throws com.borqs.server.base.ResponseError;
    Map<Long, Integer> getUserStatusByIds(Context ctx, long groupId, long[] userIds);
    //    bytes getUserStatusByIdentifies(long groupId, string identifies) throws com.borqs.server.base.ResponseError;
    Map<String, Integer> getUserStatusByIdentifies(Context ctx, long groupId, String[] identifies);
    //    bytes getUsersCounts(string groupIds, int status) throws com.borqs.server.base.ResponseError;
    Map<Long, Integer> getUsersCounts(Context ctx, long[] groupIds, int status);
    //    boolean updateUserIdByIdentify(string userId, string identify) throws com.borqs.server.base.ResponseError;
    boolean updateUserIdByIdentify(Context ctx, long userId, String identify);
    //    boolean isGroup(long id) throws com.borqs.server.base.ResponseError;
    boolean isGroup(Context ctx, long id);
    //    boolean isPublicCircle(long id) throws com.borqs.server.base.ResponseError;
    boolean isPublicCircle(Context ctx, long id);
    //    boolean isActivity(long id) throws com.borqs.server.base.ResponseError;
    boolean isActivity(Context ctx, long id);
    //    boolean isOrganization(long id) throws com.borqs.server.base.ResponseError;
    boolean isOrganization(Context ctx, long id);
    //    boolean isGeneralGroup(long id) throws com.borqs.server.base.ResponseError;
    boolean isGeneralGroup(Context ctx, long id);
}
