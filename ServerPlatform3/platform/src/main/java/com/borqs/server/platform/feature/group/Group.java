package com.borqs.server.platform.feature.group;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Record;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class Group extends Addons {

    public static final String TYPE_PUBLIC_CIRCLE = "public_circle";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_ORGANIZATION = "organization";
    public static final String TYPE_GENERAL_GROUP = "group";

    public static final long PUBLIC_CIRCLE_ID_BEGIN = 10000000000L;
    public static final long ACTIVITY_ID_BEGIN = 11000000000L;
    public static final long ORGANIZATION_ID_BEGIN = 12000000000L;
    public static final long GENERAL_GROUP_ID_BEGIN = 13000000000L;
    public static final long GENERAL_GROUP_ID_END = 14000000000L;
    public static final long GROUP_ID_END = 20000000000L;

    public static final int ROLE_CREATOR = 100;
    public static final int ROLE_ADMIN = 10;
    public static final int ROLE_MEMBER = 1;
    public static final int ROLE_GUEST = 0;

    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_MEMBER_LIMIT = "member_limit";
    public static final String COL_IS_STREAM_PUBLIC = "is_stream_public";
    public static final String COL_CAN_SEARCH = "can_search";
    public static final String COL_CAN_VIEW_MEMBERS = "can_view_members";
    public static final String COL_CAN_JOIN = "can_join";
    public static final String COL_CAN_MEMBER_INVITE = "can_member_invite";
    public static final String COL_CAN_MEMBER_APPROVE = "can_member_approve";
    public static final String COL_CAN_MEMBER_POST = "can_member_post";
    public static final String COL_CREATOR = "creator";
    public static final String COL_LABEL = "label";
    public static final String COL_CREATED_TIME = "created_time";
    public static final String COL_UPDATED_TIME = "updated_time";
    public static final String COL_DESTROYED_TIME = "destroyed_time";
    public static final String COL_MEMBERS = "members";

    public static final String COMM_COL_SMALL_IMG_URL = "small_image_url";
    public static final String COMM_COL_IMAGE_URL = "image_url";
    public static final String COMM_COL_LARGE_IMG_URL = "large_image_url";
    public static final String COMM_COL_COMPANY = "company";
    public static final String COMM_COL_DEPARTMENT = "department";
    public static final String COMM_COL_DESCRIPTION = "description";
    public static final String COMM_COL_CONTACT_INFO = "contact_info";
    public static final String COMM_COL_ADDRESS = "address";
    public static final String COMM_COL_WEBSITE = "website";
    public static final String COMM_COL_BULLETIN = "bulletin";
    public static final String COMM_COL_BULLETIN_UPDATED_TIME = "bulletin_updated_time";

    public static final String GROUP_LIGHT_COLS = StringUtils.join(new Object[]{COMM_COL_SMALL_IMG_URL,
            COMM_COL_IMAGE_URL, COMM_COL_LARGE_IMG_URL, COMM_COL_COMPANY, COMM_COL_DEPARTMENT, COMM_COL_DESCRIPTION,
            COMM_COL_CONTACT_INFO, COMM_COL_ADDRESS, COMM_COL_WEBSITE, COMM_COL_BULLETIN, COMM_COL_BULLETIN_UPDATED_TIME}, ",");

    public static final int STATUS_NONE = 0;
    public static final int STATUS_APPLIED = 1;
    public static final int STATUS_INVITED = 2;
    public static final int STATUS_JOINED = 3;
    public static final int STATUS_REJECTED = 4;
    public static final int STATUS_KICKED = 5;
    public static final int STATUS_QUIT = 6;

    public static final int PRIVACY_OPEN = 1;
    public static final int PRIVACY_CLOSED = 2;
    public static final int PRIVACY_SECRET = 3;

    private long groupId;
    private Long createdTime;
    private Long updatedTime;
    private Long destroyedTime;
    private String name;
    private Long creator;
    private String label;
    private Integer memberLimit;
    private Boolean isStreamPublic;
    private Boolean canSearch;
    private Boolean canViewMembers;
    private Boolean canJoin;
    private Boolean canMemberInvite;
    private Boolean canMemberApprove;
    private Boolean canMemberPost;
    private Record properties;

    public Group() {
        this(0L);
    }

    public Group(long groupId) {
        this.groupId = groupId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getDestroyedTime() {
        return destroyedTime;
    }

    public void setDestroyedTime(Long destroyedTime) {
        this.destroyedTime = destroyedTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreator() {
        return creator;
    }

    public void setCreator(Long creator) {
        this.creator = creator;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getMemberLimit() {
        return memberLimit;
    }

    public void setMemberLimit(Integer memberLimit) {
        this.memberLimit = memberLimit;
    }

    public Boolean getStreamPublic() {
        return isStreamPublic;
    }

    public void setStreamPublic(Boolean streamPublic) {
        isStreamPublic = streamPublic;
    }

    public Boolean getCanSearch() {
        return canSearch;
    }

    public void setCanSearch(Boolean canSearch) {
        this.canSearch = canSearch;
    }

    public Boolean getCanViewMembers() {
        return canViewMembers;
    }

    public void setCanViewMembers(Boolean canViewMembers) {
        this.canViewMembers = canViewMembers;
    }

    public Boolean getCanJoin() {
        return canJoin;
    }

    public void setCanJoin(Boolean canJoin) {
        this.canJoin = canJoin;
    }

    public Boolean getCanMemberInvite() {
        return canMemberInvite;
    }

    public void setCanMemberInvite(Boolean canMemberInvite) {
        this.canMemberInvite = canMemberInvite;
    }

    public Boolean getCanMemberApprove() {
        return canMemberApprove;
    }

    public void setCanMemberApprove(Boolean canMemberApprove) {
        this.canMemberApprove = canMemberApprove;
    }

    public Boolean getCanMemberPost() {
        return canMemberPost;
    }

    public void setCanMemberPost(Boolean canMemberPost) {
        this.canMemberPost = canMemberPost;
    }

    public Record getProperties() {
        return properties;
    }

    public void setProperties(Record properties) {
        this.properties = properties;
    }

    public Record toInfo() {
        Record info = new Record();
        info.set(COL_ID, groupId);
        if (name != null)
            info.set(COL_NAME, name);
        if (memberLimit != null)
            info.set(COL_MEMBER_LIMIT, memberLimit);
        if (isStreamPublic != null)
            info.set(COL_IS_STREAM_PUBLIC, isStreamPublic);
        if (canSearch != null)
            info.set(COL_CAN_SEARCH, canSearch);
        if (canViewMembers != null)
            info.set(COL_CAN_VIEW_MEMBERS, canViewMembers);
        if (canJoin != null)
            info.set(COL_CAN_JOIN, canJoin);
        if (canMemberInvite != null)
            info.set(COL_CAN_MEMBER_INVITE, canMemberInvite);
        if (canMemberApprove != null)
            info.set(COL_CAN_MEMBER_APPROVE, canMemberApprove);
        if (canMemberPost != null)
            info.set(COL_CAN_MEMBER_POST, canMemberPost);
        if (creator != null)
            info.set(COL_CREATOR, creator);
        if (label != null)
            info.set(COL_LABEL, label);
        if (createdTime != null)
            info.set(COL_CREATED_TIME, createdTime);
        if (updatedTime != null)
            info.set(COL_UPDATED_TIME, updatedTime);
        if (destroyedTime != null)
            info.set(COL_DESTROYED_TIME, destroyedTime);
        return info;
    }

    public void fromInfo(Record info) {
        if (info.has(COL_ID))
            setGroupId(info.getInt(COL_ID));
        if (info.has(COL_NAME))
            setName(info.getString(COL_NAME));
        if (info.has(COL_MEMBER_LIMIT))
            setMemberLimit((int)info.getInt(COL_MEMBER_LIMIT));
        if (info.has(COL_IS_STREAM_PUBLIC))
            setStreamPublic(info.checkGetBoolean(COL_IS_STREAM_PUBLIC));
        if (info.has(COL_CAN_SEARCH))
            setCanSearch(info.checkGetBoolean(COL_CAN_SEARCH));
        if (info.has(COL_CAN_VIEW_MEMBERS))
            setCanViewMembers(info.checkGetBoolean(COL_CAN_VIEW_MEMBERS));
        if (info.has(COL_CAN_JOIN))
            setCanJoin(info.checkGetBoolean(COL_CAN_JOIN));
        if (info.has(COL_CAN_MEMBER_INVITE))
            setCanMemberInvite(info.checkGetBoolean(COL_CAN_MEMBER_INVITE));
        if (info.has(COL_CAN_MEMBER_APPROVE))
            setCanMemberApprove(info.checkGetBoolean(COL_CAN_MEMBER_APPROVE));
        if (info.has(COL_CAN_MEMBER_POST))
            setCanMemberPost(info.checkGetBoolean(COL_CAN_MEMBER_POST));
        if (info.has(COL_CREATOR))
            setCreator(info.getInt(COL_CREATOR));
        if (info.has(COL_LABEL))
            setLabel(info.getString(COL_LABEL));
        if (info.has(COL_CREATED_TIME))
            setCreatedTime(info.getInt(COL_CREATED_TIME));
        if (info.has(COL_UPDATED_TIME))
            setUpdatedTime(info.getInt(COL_UPDATED_TIME));
        if (info.has(COL_DESTROYED_TIME))
            setDestroyedTime(info.getInt(COL_DESTROYED_TIME));
    }

    public static Group infoToGroup(Record info) {
        Group group = new Group();
        group.fromInfo(info);
        return group;
    }
}
