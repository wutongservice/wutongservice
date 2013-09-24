package com.borqs.server.platform.group;

import com.borqs.server.base.util.StringUtils2;

public class GroupConstants {
    public static final String TYPE_PUBLIC_CIRCLE = "public_circle";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_DEPARTMENT = "department";
    public static final String TYPE_COMPANY = "company";
    public static final String TYPE_GENERAL_GROUP = "group";
    public static final String TYPE_EVENT = "event";

    public static final long PUBLIC_CIRCLE_ID_BEGIN = 10000000000L;
    public static final long ACTIVITY_ID_BEGIN = 11000000000L;
    public static final long DEPARTMENT_ID_BEGIN = 12000000000L;
    public static final long GENERAL_GROUP_ID_BEGIN = 13000000000L;
    public static final long EVENT_ID_BEGIN = 14000000000L;
    public static final long EVENT_ID_END = 15000000000L;
    public static final long COMPANY_ID_BEGIN = 15000000001L;
    public static final long COMPANY_ID_END = 16000000000L;
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
    public static final String COL_CAN_MEMBER_QUIT = "can_member_quit";
    public static final String COL_NEED_INVITED_CONFIRM = "need_invited_confirm";
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
    public static final String COMM_COL_TOP_POSTS = "top_posts";
    public static final String COMM_COL_TOP_NAME = "top_name";
    public static final String COMM_COL_THEME_ID = "theme_id";
    public static final String COMM_COL_THEME_NAME = "theme_name";
    public static final String COMM_COL_THEME_IMAGE = "theme_image";

    public static final String DEP_COL_PARENT = "parent_departments";
    public static final String DEP_COL_SUB = "sub_departments";
    public static final String COMPANY_COL_ROOT_DEPARTMENT_ID = "root_department_id";
    
    public static final String GROUP_LIGHT_COLS = StringUtils2.joinIgnoreBlank(",", COMM_COL_SMALL_IMG_URL,
            COMM_COL_IMAGE_URL, COMM_COL_LARGE_IMG_URL, COMM_COL_COMPANY, COMM_COL_DEPARTMENT, COMM_COL_DESCRIPTION,
            COMM_COL_CONTACT_INFO, COMM_COL_ADDRESS, COMM_COL_WEBSITE, COMM_COL_BULLETIN, COMM_COL_BULLETIN_UPDATED_TIME,
            COMM_COL_THEME_ID, COMM_COL_THEME_NAME, COMM_COL_THEME_IMAGE, COMM_COL_TOP_POSTS, COMM_COL_TOP_NAME);
    
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
}
