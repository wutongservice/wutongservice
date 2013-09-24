package com.borqs.server.wutong;


import com.borqs.server.base.util.CollectionUtils2;
import com.borqs.server.base.util.I18nHelper;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.base.util.TextCollection;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class Constants {

    // App Id
    public static final String NULL_APP_ID = "0";

    public static boolean isNullAppId(String appId) {
        return StringUtils.isBlank(appId) || StringUtils.equals(appId, NULL_APP_ID);
    }

    public static final int APP_TYPE_QIUPU = 1;
    public static final int APP_TYPE_BROOK = 2;
    public static final int APP_TYPE_VIDEO = 3;
    public static final int APP_TYPE_MUSIC = 4;
    public static final int APP_TYPE_PHOTO = 5;
    public static final int APP_TYPE_BPC = 9;
    public static int MAX_GUSY_SHARE_TO = 400;

    // User Id
    public static final String NULL_USER_ID = "0";

    public static boolean isNullUserId(String userId) {
        return StringUtils.isBlank(userId) || StringUtils.equals(userId, NULL_USER_ID);
    }

    // friendship relations
    public static final int NONE_RELATION = 0;
    public static final int FRIEND_RELATION = 1;
    public static final int BLOCK_RELATION = 2;

    // suggested user reason type

    public static final String RECOMMENDER_USER = "10";
    public static final String REQUEST_ATTENTION = "12";
    public static final String FROM_ADDRESS = "20";
    public static final String FROM_ADDRESS_HAVEBORQSID = "21";
    public static final String FROM_ADDRESS_HAVECOMMONBORQSID = "22";
    public static final String FROM_ADDRESS_HASMYCONTACTINFO = "23";
    public static final String FROM_USERPROFILE = "30";
    public static final String FROM_USERPROFILE_WORKINFO = "31";
    public static final String FROM_USERPROFILE_EDUINFO = "32";
    public static final String IN_COMMON_FRIENDS = "40";
    public static final String FROM_SYSTEM = "50";

    // User gender
    public static final String MALE = "m";
    public static final String FEMALE = "f";

    // marriage
    public static final String MARRIAGE_UNKNOWN = "n";
    public static final String SPINSTERHOOD = "s";
    public static final String MARRIED = "m";
    public static final String REMARRIAGE = "r";

    // Post id
    public static final String NULL_POST_ID = "0";

    public static boolean isNullPostId(String postId) {
        return StringUtils.isBlank(postId) || StringUtils.equals(postId, NULL_POST_ID);
    }

    // Post type

    public static final int TEXT_POST = 1;
    public static final int PHOTO_POST = 1 << 1;
    public static final int VIDEO_POST = 1 << 2;
    public static final int AUDIO_POST = 1 << 3;
    public static final int BOOK_POST = 1 << 4;
    public static final int APK_POST = 1 << 5;
    public static final int LINK_POST = 1 << 6;
    public static final int APK_LINK_POST = 1 << 7;
    public static final int APK_COMMENT_POST = 1 << 8;
    public static final int APK_LIKE_POST = 1 << 9;
    public static final int BOOK_LIKE_POST = 1 << 10;
    public static final int BOOK_COMMENT_POST = 1 << 11;
    public static final int FRIEND_SET_POST = 1 << 12;
    public static final int MUSIC_POST = 1 << 13;
    public static final int MUSIC_COMMENT_POST = 1 << 14;
    public static final int MUSIC_LIKE_POST = 1 << 15;
    public static final int SIGN_IN_POST = 1 << 16;
    public static final int FILE_POST = 1 << 17;
    public static final int VOTE_POST = 1 << 18;
    public static final int APPLY_POST = 1 << 22;

    public static final int ALL_POST = TEXT_POST | VIDEO_POST
            | AUDIO_POST | BOOK_POST | APK_POST | LINK_POST | APK_LINK_POST | APK_COMMENT_POST | APK_LIKE_POST | BOOK_LIKE_POST | BOOK_COMMENT_POST | FRIEND_SET_POST
            | MUSIC_POST | MUSIC_COMMENT_POST | MUSIC_LIKE_POST | PHOTO_POST | SIGN_IN_POST | FILE_POST | VOTE_POST | APPLY_POST;


    // Object type
    public static final int USER_OBJECT = 1;
    public static final int POST_OBJECT = 2;
    public static final int VIDEO_OBJECT = 3;
    public static final int APK_OBJECT = 4;
    public static final int MUSIC_OBJECT = 5;
    public static final int BOOK_OBJECT = 6;
    public static final int COMMENT_OBJECT = 7;
    public static final int LIKE_OBJECT = 8;
    public static final int LINK_OBJECT = 9;
    public static final int PHOTO_OBJECT = 10;
    public static final int FILE_OBJECT = 11;
    public static final int POLL_OBJECT = 12;
    public static final int PUBLIC_CIRCLE_OBJECT = 13;
    public static final int EVENT_OBJECT = 14;
    public static final int COMPANY_OBJECT = 15;
    public static final int PAGE_OBJECT = 16;
    public static final int LOCAL_CIRCLE_OBJECT = 17;

    public static final TextCollection OBJECTS = TextCollection.of(new Object[][]{
            {"user", USER_OBJECT},
            {"post", POST_OBJECT},
            {"video", VIDEO_OBJECT},
            {"apk", APK_OBJECT},
            {"music", MUSIC_OBJECT},
            {"comment", COMMENT_OBJECT},
            {"like", LIKE_OBJECT},
            {"book", BOOK_OBJECT},
            {"link", LINK_OBJECT},
            {"photo", PHOTO_OBJECT},
            {"file", FILE_OBJECT},
            {"public_circle", PUBLIC_CIRCLE_OBJECT},
            {"event_object", EVENT_OBJECT},
            {"company", COMPANY_OBJECT},
            {"page", PAGE_OBJECT},
            {"poll", POLL_OBJECT},
    });


    public static int getUserTypeById(long id) {
        if (id > 0 && id < GROUP_ID_BEGIN) {
            return USER_OBJECT;
        } else if (id >= GROUP_ID_BEGIN && id < GROUP_ID_END) {
            if (id >= COMPANY_ID_BEGIN && id < COMPANY_ID_END) {
                return COMPANY_OBJECT;
            } else if (id >= PUBLIC_CIRCLE_ID_BEGIN && id < PUBLIC_CIRCLE_ID_END) {
                return PUBLIC_CIRCLE_OBJECT;
            } else if (id >= EVENT_ID_BEGIN && id < EVENT_ID_END) {
                return EVENT_OBJECT;
            } else {
                return 0;
            }
        } else if (id >= PAGE_ID_BEGIN && id < PAGE_ID_END) {
            return PAGE_OBJECT;
        } else {
            return 0;
        }
    }

    public static int REPORT_ABUSE_COUNT = 3;


    public static final int CONVERSATION_SHARE = 1;
    public static final int CONVERSATION_RESHARE = 2;
    public static final int CONVERSATION_COMMENT = 3;
    public static final int CONVERSATION_LIKE = 4;
    public static final int CONVERSATION_FAVORITE = 5;

    public static int FOLDER_TYPE_SHARE_OUT = 1;
    public static int FOLDER_TYPE_RECEIVED = 3;
    public static int FOLDER_TYPE_GROUP = 4;
    public static int FOLDER_TYPE_TO_GROUP = 5;     //              我在group里面发文件创建的文件夹
    public static int FOLDER_TYPE_MY_SYNC = 8;
    public static int FOLDER_TYPE_OTHERS = 9;
    public static int FOLDER_TYPE_CONFIGURATION = 20;

    public static String bucketName = "wutong-data";
    public static String bucketName_photo_key = "media/photo/";
    public static String bucketName_video_key = "media/video/";
    public static String bucketName_audio_key = "media/audio/";
    public static String bucketName_static_file_key = "files/";

    public static int getObjectType(String objectId) {
        return Integer.parseInt(StringUtils.substringBefore(objectId, ":"));
    }

    public static String getObjectKey(String objectId) {
        return StringUtils.substringAfter(objectId, ":");
    }

    public static String objectId(int type, Object id) {
        return new StringBuilder().append(type).append(':').append(id).toString();
    }

    public static String postObjectId(Object postId) {
        return objectId(POST_OBJECT, postId);
    }

    public static String videoObjectId(Object videoId) {
        return objectId(VIDEO_OBJECT, videoId);
    }

    public static String musicObjectId(Object musicId) {
        return objectId(MUSIC_OBJECT, musicId);
    }

    public static String apkObjectId(Object apkId) {
        return objectId(APK_OBJECT, apkId);
    }

    public static String commentObjectId(Object commentId) {
        return objectId(COMMENT_OBJECT, commentId);
    }

    public static String likeObjectId(Object likeId) {
        return objectId(LIKE_OBJECT, likeId);
    }

    public static String userObjectId(Object userId) {
        return objectId(USER_OBJECT, userId);
    }

    public static String bookObjectId(Object bookId) {
        return objectId(BOOK_OBJECT, bookId);
    }

    public static String photoObjectId(Object photoId) {
        return objectId(PHOTO_OBJECT, photoId);
    }

    public static String fileObjectId(Object fileId) {
        return objectId(FILE_OBJECT, fileId);
    }

    public static String pollObjectId(Object pollId) {
        return objectId(POLL_OBJECT, pollId);
    }


    // Resource
//    public static final String RESOURCE_COMMON = "common";
    public static final String RESOURCE_BASIC = "basic";
    public static final String RESOURCE_PHONEBOOK = "phonebook";
    public static final String RESOURCE_BUSINESS = "business";
    public static final String RESOURCE_EDUCATION = "education";
    public static final String RESOURCE_WORK = "work";

    // Notification
    public static final String NTF_QIUPU_UPDATE = "ntf.qiupu_update";
    public static final String NTF_APP_UPDATE = "ntf.app_update";
    public static final String NTF_NEW_AREA = "ntf.new_area";
    public static final String NTF_NEW_APP = "ntf.new_app";
    public static final String NTF_APP_DAREN = "ntf.app_daren";
    public static final String NTF_MY_STREAM_COMMENT = "ntf.my_stream_comment";
    public static final String NTF_MY_STREAM_LIKE = "ntf.my_stream_like";
    public static final String NTF_MY_STREAM_RETWEET = "ntf.my_stream_retweet";
    public static final String NTF_MY_APP_COMMENT = "ntf.my_app_comment";
    public static final String NTF_PHOTO_COMMENT = "ntf.photo_comment";
    public static final String NTF_FILE_SHARE = "ntf.file_share";
    public static final String NTF_BORQS_APPLY = "ntf.borqs_apply";
    public static final String NTF_FILE_COMMENT = "ntf.file_comment";
    public static final String NTF_FILE_LIKE = "ntf.file_like";
    public static final String NTF_PHOTO_LIKE = "ntf.photo_like";
    public static final String NTF_MY_APP_LIKE = "ntf.my_app_like";
    public static final String NTF_MY_APP_RETWEET = "ntf.my_app_retweet";
    public static final String NTF_INVOLVED_STREAM_COMMENT = "ntf.involved_stream_comment";
    public static final String NTF_INVOLVED_STREAM_LIKE = "ntf.involved_stream_like";
    public static final String NTF_INVOLVED_APP_COMMENT = "ntf.involved_app_comment";
    public static final String NTF_INVOLVED_APP_LIKE = "ntf.involved_app_like";
    public static final String NTF_FRIENDS_ONLINE = "ntf.friends_online";
    public static final String NTF_APP_SHARE = "ntf.app_share";
    public static final String NTF_OTHER_SHARE = "ntf.other_share";
    public static final String NTF_PHOTO_SHARE = "ntf.photo_share";
    public static final String NTF_PROFILE_UPDATE = "ntf.profile_update";
    public static final String NTF_NEW_MESSAGE = "ntf.new_message";
    public static final String NTF_NEW_FOLLOWER = "ntf.new_follower";
    public static final String NTF_PEOPLE_YOU_MAY_KNOW = "ntf.people_you_may_know";
    public static final String NTF_SUGGEST_USER = "ntf.suggest_user";
    public static final String NTF_ACCEPT_SUGGEST = "ntf.accept_suggest";
    public static final String NTF_NEW_REQUEST = "ntf.new_request";
    public static final String NTF_BIND_SEND = "ntf.bind_send";
    public static final String NTF_REQUEST_ATTENTION = "ntf.request_attention";
    public static final String NTF_REQ_PROFILE_ACCESS = "ntf.req_profile_access";
    public static final String SOCIALCONTACT_AUTO_ADD = "socialcontact.autoaddfriend";
    public static final String EMAIL_APK_COMMENT = "email.apk_comment";
    public static final String EMAIL_APK_LIKE = "email.apk_like";
    public static final String EMAIL_STREAM_COMMENT = "email.stream_comment";
    public static final String EMAIL_STREAM_LIKE = "email.stream_like";
    public static final String EMAIL_ESSENTIAL = "email.essential";
    public static final String EMAIL_SHARE_TO = "email.share_to";
    public static final String NTF_CREATE_ACCOUNT = "ntf.create_account";
    public static final String NTF_REPORT_ABUSE = "ntf.report_abuse";
    public static final String NTF_GROUP_INVITE = "ntf.group_invite";
    public static final String NTF_GROUP_APPLY = "ntf.group_apply";
    public static final String NTF_GROUP_JOIN = "ntf.group_join";
    public static final String NTF_POLL_INVITE = "ntf.poll_invite";
    public static final String NTF_POLL_COMMENT = "ntf.poll_comment";
    public static final String NTF_POLL_LIKE = "ntf.poll_like";
    public static final String NTF_SUBSCRIBE = "ntf.subscribe";


    public static final String MAIL_A_CREATE_ACCOUNT = "mail.create_account";
    public static final String MAIL_D_REGIST_SUCCESS = "mail.regist_success";
    public static final String MAIL_A_UPDATE_PASSWORD = "mail.update_password";
    public static final String MAIL_A_GET_BACK_PASSWORD = "mail.get_back_password";
    public static final String MAIL_D_NEW_FOLLOWERS = "mail.new_followers";
    public static final String MAIL_D_PEOPLE_YOU_MAY_KNOW = "mail.people_you_may_know";
    public static final String MAIL_C_REQUEST_ATTENTION = "mail.request_attention";
    public static final String MAIL_D_RECOMMEND_USER = "mail.recommend_user";
    public static final String MAIL_C_CIRCLE_REQUEST_JOIN = "mail.circle_request_join";
    public static final String MAIL_D_CIRCLE_REQUEST_JOIN_APPROVED = "mail.circle_request_join_approved";
    public static final String MAIL_D_CIRCLE_INVITED_JOIN = "mail.circle_invited_join";
    public static final String MAIL_B_CIRCLE_NOTICE = "mail.circle_notice";
    public static final String MAIL_D_COMPANY_NEW_USER = "mail.company_new_user";
    public static final String MAIL_B_COMPANY_HR_NOTICE = "mail.company_hr_notice";
    public static final String MAIL_C_EVENT_BEEN_INVITED = "mail.event_been_invited";
    public static final String MAIL_B_EVENT_NOTICE = "mail.event_notice";
    public static final String MAIL_C_POLL_BEEN_INVITED = "mail.poll_been_invited";
    public static final String MAIL_A_POLL_END = "mail.poll_end";
    public static final String MAIL_A_FOLLOW_SHARE = "mail.follow_share";
    public static final String MAIL_A_REPORT_ABUSER_DEAL = "mail.report_abuse_deal";
    public static final String MAIL_A_NEW_VERSION = "mail.new_version";


    //group
    public static final String TYPE_PUBLIC_CIRCLE = "public_circle";
    public static final String TYPE_ACTIVITY = "activity";
    public static final String TYPE_DEPARTMENT = "department";
    public static final String TYPE_COMPANY = "company";
    public static final String TYPE_GENERAL_GROUP = "group";
    public static final String TYPE_EVENT = "event";

    public static final String ROOT_DEPARTMENT_NAME = "$$$";

    public static final long GROUP_ID_BEGIN = 10000000000L;
    public static final long PUBLIC_CIRCLE_ID_BEGIN = 10000000000L;
    public static final long PUBLIC_CIRCLE_ID_END = 11000000000L - 1;
    public static final long ACTIVITY_ID_BEGIN = 11000000000L;
    public static final long DEPARTMENT_ID_BEGIN = 12000000000L;
    public static final long DEPARTMENT_ID_END = 13000000000L;
    public static final long GENERAL_GROUP_ID_BEGIN = 13000000000L;
    public static final long EVENT_ID_BEGIN = 14000000000L;
    public static final long EVENT_ID_END = 15000000000L;
    public static final long COMPANY_ID_BEGIN = 15000000001L;
    public static final long COMPANY_ID_END = 16000000000L;
    public static final long GROUP_ID_END = 20000000000L;
    public static final long PAGE_ID_BEGIN = 20000000001L;
    public static final long PAGE_ID_END = 21000000000L;


    public static final String PUBLIC_CIRCLE_TYPE_FORMAL = "formal";
    public static final String PUBLIC_CIRCLE_SUBTYPE_COMPANY = "company";
    public static final String PUBLIC_CIRCLE_SUBTYPE_SCHOOL = "school";

    public static final int ROLE_CREATOR = 100;
    public static final int ROLE_ADMIN = 10;
    public static final int ROLE_MEMBER = 1;
    public static final int ROLE_GUEST = 0;

    public static final String GRP_COL_ID = "id";
    public static final String GRP_COL_NAME = "name";
    public static final String GRP_COL_MEMBER_LIMIT = "member_limit";
    public static final String GRP_COL_IS_STREAM_PUBLIC = "is_stream_public";
    public static final String GRP_COL_CAN_SEARCH = "can_search";
    public static final String GRP_COL_CAN_VIEW_MEMBERS = "can_view_members";
    public static final String GRP_COL_CAN_JOIN = "can_join";
    public static final String GRP_COL_CAN_MEMBER_INVITE = "can_member_invite";
    public static final String GRP_COL_CAN_MEMBER_APPROVE = "can_member_approve";
    public static final String GRP_COL_CAN_MEMBER_POST = "can_member_post";
    public static final String GRP_COL_CAN_MEMBER_QUIT = "can_member_quit";
    public static final String GRP_COL_NEED_INVITED_CONFIRM = "need_invited_confirm";
    public static final String GRP_COL_CREATOR = "creator";
    public static final String GRP_COL_LABEL = "label";
    public static final String GRP_COL_CREATED_TIME = "created_time";
    public static final String GRP_COL_UPDATED_TIME = "updated_time";
    public static final String GRP_COL_DESTROYED_TIME = "destroyed_time";
    public static final String GRP_COL_MEMBERS = "members";

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
    public static final String COMM_COL_SUBTYPE = "subtype";

    public static final String EMP_COL_NAME = "name";
    public static final String EMP_COL_NAME_EN = "name_en";
    public static final String EMP_COL_EMPLOYEE_ID = "employee_id";
    public static final String EMP_COL_DEPARTMENT = "department";
    public static final String EMP_COL_JOB_TITLE = "job_title";
    public static final String EMP_COL_EMAIL = "email";
    public static final String EMP_COL_TEL = "tel";
    public static final String EMP_COL_MOBILE_TEL = "mobile_tel";
    public static final String EMP_COL_USER_ID = "user_id";

    public static final String COMPANY_COL_ROOT_DEPARTMENT_ID = "root_department_id";

    public static final String DEP_COL_PARENT = "parent_department";
    public static final String DEP_COL_SUB = "sub_departments";
    public static final String DEP_COL_COMPANY = "company";
    public static final String DEP_COL_IS_DEP = "is_department";
    public static final String DEP_COL_IS_ROOT = "is_company_root";

    public static final int PUBLIC_CIRCLE_FORMAL_FREE = 0;//兴趣圈
    public static final int PUBLIC_CIRCLE_FORMAL_TOP = 1;//公司顶级圈子
    public static final int PUBLIC_CIRCLE_FORMAL_SUB = 2;//公司部门

    public static final String GROUP_LIGHT_COLS = StringUtils2.joinIgnoreBlank(",", COMM_COL_SMALL_IMG_URL,
            COMM_COL_IMAGE_URL, COMM_COL_LARGE_IMG_URL, COMM_COL_COMPANY, COMM_COL_DEPARTMENT, COMM_COL_DESCRIPTION,
            COMM_COL_CONTACT_INFO, COMM_COL_ADDRESS, COMM_COL_WEBSITE, COMM_COL_BULLETIN, COMM_COL_BULLETIN_UPDATED_TIME,
            COMM_COL_THEME_ID, COMM_COL_THEME_NAME, COMM_COL_THEME_IMAGE, COMM_COL_TOP_POSTS, COMM_COL_TOP_NAME,COMM_COL_SUBTYPE);

    public static final int STATUS_NONE = 0;
    public static final int STATUS_APPLIED = 1;
    public static final int STATUS_INVITED = 2;
    public static final int STATUS_JOINED = 3;
    public static final int STATUS_REJECTED = 4;
    public static final int STATUS_KICKED = 5;
    public static final int STATUS_QUIT = 6;

    public static final int GRP_PRIVACY_OPEN = 1;
    public static final int GRP_PRIVACY_CLOSED = 2;
    public static final int GRP_PRIVACY_SECRET = 3;

    public static final int CIRCLE_TYPE_LOCAL = 0;
    public static final int CIRCLE_TYPE_PUBLIC = 1;

    //-1 - unknown 0 - borqs id   1 - email  2 - phone  3 - local circle  4- virtual id  5 - group id  6 - page id
    public static final int IDENTIFY_TYPE_UNKNOWN = -1;
    public static final int IDENTIFY_TYPE_BORQS_ID = 0;
    public static final int IDENTIFY_TYPE_EMAIL = 1;
    public static final int IDENTIFY_TYPE_PHONE = 2;
    public static final int IDENTIFY_TYPE_LOCAL_CIRCLE = 3;
    public static final int IDENTIFY_TYPE_VIRTUAL_ID = 4;
    public static final int IDENTIFY_TYPE_GROUP_ID = 5;
    public static final int IDENTIFY_TYPE_PAGE_ID = 6;

    public static HashMap<String, String> col_res = new HashMap<String, String>();
    public static HashMap<String, String> res_col = new HashMap<String, String>();

    static {
        col_res.put("login_email1", "private.loginemail.1");
        col_res.put("login_email2", "private.loginemail.2");
        col_res.put("login_email3", "private.loginemail.3");
        col_res.put("login_phone1", "private.loginphone.1");
        col_res.put("login_phone2", "private.loginphone.2");
        col_res.put("login_phone3", "private.loginphone.3");
        col_res.put("password", "private.password");
//    	col_res.put("first_name", "basic.firstname");
//    	col_res.put("middle_name", "basic.middlename");
//    	col_res.put("last_name", "basic.lastname");   	
//    	col_res.put("birthday", "basic.birthday");
//    	col_res.put("timezone", "basic.timezone");
//    	col_res.put("interests", "basic.interests");
//    	col_res.put("languages", "basic.languages");
//    	col_res.put("religion", "basic.religion");
//    	col_res.put("company", "business.company");
//    	col_res.put("department", "business.department");
//    	col_res.put("job_title", "business.title");
//    	col_res.put("office_address", "business.address");
//    	col_res.put("profession", "business.profession");
//    	col_res.put("job_description", "business.description");
        col_res.put("contact_info", "phonebook");
        col_res.put("address", "phonebook.address");
//    	col_res.put("work_history", "work");
//    	col_res.put("education_history", "education");

        Iterator iter = col_res.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            res_col.put(value, key);
        }
    }

    // Circle and friend
    public static final int PUBLIC_CIRCLE = 0;          // virtual
    public static final int FRIENDS_CIRCLE = 1;         // virtual, finite
    public static final int STRANGER_CIRCLE = 2;        // virtual
    public static final int FOLLOWERS_CIRCLE = 3;       // virtual

    public static final int FRIEND_REASON_INVITE = 1;
    public static final int FRIEND_REASON_RECOMMEND = 2;
    public static final int FRIEND_REASON_SOCIALCONTACT = 4;
    public static final int FRIEND_REASON_SOCIALCONTACT_DELETE = 5;
    public static final int FRIEND_REASON_DEFAULT_DELETE = 6;
    public static final int FRIEND_REASON_MANUALSELECT = 8;
    public static final int FRIEND_REASON_AUTOCREATE = 9;

    public static final int BLOCKED_CIRCLE = 4;         // actual,  finite
    public static final int ADDRESS_BOOK_CIRCLE = 5;    // actual, finite
    public static final int DEFAULT_CIRCLE = 6;         // actual, finite
    public static final int ME_CIRCLE = 7;              // virtual, finite
    public static final int FAMILY_CIRCLE = 9;                 // actual, finite
    public static final int CLOSE_FRIENDS_CIRCLE = 10;  // actual, finite
    public static final int ACQUAINTANCE_CIRCLE = 11; // actual, finite
    public static final int PAGE_CIRCLE = 12; // actual, finite


    public static boolean isVirtualCircle(int circleId) {
        return circleId == PUBLIC_CIRCLE
                || circleId == FRIENDS_CIRCLE
                || circleId == STRANGER_CIRCLE
                || circleId == FOLLOWERS_CIRCLE
                || circleId == ME_CIRCLE;

    }


    public static boolean isActualCircle(int circleId) {
        return !isVirtualCircle(circleId);
    }

    public static boolean isFiniteCircle(int circleId) {
        return circleId == FRIENDS_CIRCLE || circleId == ME_CIRCLE || isActualCircle(circleId);
    }

    // contact types
    // 1 - 19 phone
    // 21 - 29 email
    // 31 - 39 fax
    // 41 - 49 pager
    // 51 - 59 IM
    // 61 - 69 web page
    // 71 - 79 label
    // 91 - 99 imei ~ imsi
//    public static final int CONTACT_UNDEFINED = -1;
//    public static final int CONTACT_ASSISTANT_NUMBER = 19;
//    public static final int CONTACT_BUSINESS_TELEPHONE_NUMBER = 1;
//    public static final int CONTACT_BUSINESS_2_TELEPHONE_NUMBER = 2;
//    public static final int CONTACT_CALLBACK_NUMBER = 18;
//    public static final int CONTACT_CAR_TELEPHONE_NUMBER = 17;
//    public static final int CONTACT_COMPANY_MAIN_TELEPHONE_NUMBER = 16;
//    public static final int CONTACT_EMAIL_ADDRESS = 21;
//    public static final int CONTACT_EMAIL_2_ADDRESS = 22;
//    public static final int CONTACT_EMAIL_3_ADDRESS = 23;
//    public static final int CONTACT_HOME_WEB_PAGE = 61;
//    public static final int CONTACT_HOME_TELEPHONE_NUMBER = 3;
//    public static final int CONTACT_HOME_2_TELEPHONE_NUMBER = 4;
//    public static final int CONTACT_HOME_FAX_NUMBER = 31;
//    public static final int CONTACT_BUSINESS_FAX_NUMBER = 32;
//    public static final int CONTACT_OTHER_FAX_NUMBER = 33;
//    public static final int CONTACT_MOBILE_TELEPHONE_NUMBER = 5;
//    public static final int CONTACT_MOBILE_2_TELEPHONE_NUMBER = 6;
//    public static final int CONTACT_MOBILE_3_TELEPHONE_NUMBER = 7;
//    public static final int CONTACT_OTHER_TELEPHONE_NUMBER = 15;
//    public static final int CONTACT_PAGER_NUMBER = 41;
//    public static final int CONTACT_PRIMARY_TELEPHONE_NUMBER = 14;
//    public static final int CONTACT_WEB_PAGE = 61;
//    public static final int CONTACT_BUSINESS_WEB_PAGE = 62;
//    public static final int CONTACT_IM_QQ = 51;
//    public static final int CONTACT_IM_MSN = 52;
//    public static final int CONTACT_IM_GOOGLE = 53;
//    public static final int CONTACT_BUSINESS_LABEL = 71;
//    public static final int CONTACT_HOME_LABEL = 72;
//    public static final int CONTACT_OTHER_LABEL = 73;
//    public static final int CONTACT_TELEX_NUMBER = 13;
//    public static final int CONTACT_RADIO_TELEPHONE_NUMBER = 12;
//    public static final int CONTACT_MOBILE_TELEPHONE_IMEI = 91;
//    public static final int CONTACT_MOBILE_2_TELEPHONE_IMEI = 92;
//    public static final int CONTACT_MOBILE_3_TELEPHONE_IMEI = 93;
//    public static final int CONTACT_MOBILE_TELEPHONE_IMSI = 94;
//    public static final int CONTACT_MOBILE_2_TELEPHONE_IMSI = 95;
//    public static final int CONTACT_MOBILE_3_TELEPHONE_IMSI = 96;
//    public static final int CONTACT_X_TAG_ACCOUNT_TYPE = 101;

    public static String parseUserAgent(String device, String key) {
        List<String> l = StringUtils2.splitList(device, ";", true);
        for (String str : l) {
            if (str.contains(key)) {
                return StringUtils.substringAfter(str, "=");
            }
        }

        return "";
    }

    public static String parseLocation(String location, String key) {
        if (!StringUtils.isBlank(location)) {
            String l[] = StringUtils2.splitArray(location, ";", true);

            for (int i = 0; i < l.length; i++) {
                if (l[i].toString().contains(key)) {
                    return StringUtils.substringAfter(l[i].toString(), "=");
                }
            }
        }
        return "";
    }

    public static String getBundleString(String ua, String key) {
        String lang = "zh";
        if (StringUtils.isNotBlank(ua)) {
            lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        }
        if (StringUtils.isBlank(lang)) {
            lang = "zh";
        }

        return getBundleStringByLang(lang, key);
    }

    public static String getBundleStringByLang(String lang, String key) {
        ResourceBundle bundle = I18nHelper.getBundle("com.borqs.server.i18n.platform", new Locale(lang));
        return bundle.getString(key);
    }

    // Request type
    public static final String REQUEST_PROFILE_ACCESS = "1";
    public static final String REQUEST_FRIEND_FEEDBACK = "2";
    public static final String REQUEST_ADD_FRIEND = "3";
    public static final String REQUEST_CHANGE_MOBILE_TELEPHONE_NUMBER = "4";
    public static final String REQUEST_CHANGE_MOBILE_2_TELEPHONE_NUMBER = "5";
    public static final String REQUEST_CHANGE_MOBILE_3_TELEPHONE_NUMBER = "6";
    public static final String REQUEST_CHANGE_EMAIL_ADDRESS = "7";
    public static final String REQUEST_CHANGE_EMAIL_2_ADDRESS = "8";
    public static final String REQUEST_CHANGE_EMAIL_3_ADDRESS = "9";
    public static final String REQUEST_PUBLIC_CIRCLE_INVITE = "10";
    public static final String REQUEST_PUBLIC_CIRCLE_JOIN = "11";
    public static final String REQUEST_ACTIVITY_INVITE = "12";
    public static final String REQUEST_ACTIVITY_JOIN = "13";
    public static final String REQUEST_ORGANIZATION_INVITE = "14";
    public static final String REQUEST_ORGANIZATION_JOIN = "15";
    public static final String REQUEST_GENERAL_GROUP_INVITE = "16";
    public static final String REQUEST_GENERAL_GROUP_JOIN = "17";
    public static final String REQUEST_EVENT_INVITE = "18";
    public static final String REQUEST_EVENT_JOIN = "19";

    public static final String REQUEST_FRIEND= "91";
    public static final String REQUEST_EVENT = "92";
    public static final String REQUEST_CIRCLE = "93";
    public static final String REQUEST_PROFILE = "94";



    public static final int C_STREAM_POST = 1;
    public static final int C_STREAM_RESHARE = 2;
    public static final int C_STREAM_COMMENT = 3;
    public static final int C_STREAM_LIKE = 4;
    public static final int C_STREAM_FAVORITE = 5;
    public static final int C_STREAM_IGNORE = 6;
    public static final int C_STREAM_TO = 7;
    public static final int C_STREAM_ADDTO = 8;

    public static final int C_SUBSCRIBE_STREAM = 101;
    public static final int C_SUBSCRIBE_USER = 102;
    public static final int C_SUBSCRIBE_LOCAL_CIRCLE = 103;


    public static final int C_COMMENT_CREATE = 11;
    public static final int C_COMMENT_LIKE = 12;
    public static final int C_COMMENT_TO = 13;
    public static final int C_COMMENT_ADDTO = 14;

    public static final int C_APK_UPLOAD = 21;
    public static final int C_APK_SHARE = 22;
    public static final int C_APK_COMMENT = 23;
    public static final int C_APK_LIKE = 24;
    public static final int C_APK_FAVORITE = 25;
    public static final int C_APK_TO = 26;
    public static final int C_APK_ADDTO = 27;

    public static final int C_LINK_SHARE = 31;
    public static final int C_BOOK_SHARE = 41;
    public static final int C_VIDEO_SHARE = 51;
    public static final int C_MUSIC_SHARE = 61;

    public static final int C_PHOTO_UPLOAD = 71;
    public static final int C_PHOTO_SHARE = 72;
    public static final int C_PHOTO_COMMENT = 73;
    public static final int C_PHOTO_LIKE = 74;
    public static final int C_PHOTO_FAVORITE = 75;
    public static final int C_PHOTO_TO = 76;
    public static final int C_PHOTO_ADDTO = 77;


    public static final int C_FILE_UPLOAD = 81;
    public static final int C_FILE_SHARE = 82;
    public static final int C_FILE_COMMENT = 83;
    public static final int C_FILE_LIKE = 84;
    public static final int C_FILE_FAVORITE = 85;
    public static final int C_FILE_TO = 86;
    public static final int C_FILE_ADDTO = 88;

    public static final int C_POLL_CREATE = 91;
    public static final int C_POLL_COMMENT = 92;
    public static final int C_POLL_LIKE = 93;

    public static final int IGNORE_USER = 1;
    public static final int IGNORE_STREAM = 2;
    public static final int IGNORE_COMMENT = 3;

    public static final int USER_ID_MAX_LEN = 15;

    public static String correctContentType(String expName, String oldContentType) {
        //video
        if (expName.equalsIgnoreCase("mp2"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mpa"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mpe"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mpeg"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mpg"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mpv2"))
            oldContentType = "video/mpeg";
        if (expName.equalsIgnoreCase("mov"))
            oldContentType = "video/quicktime";
        if (expName.equalsIgnoreCase("mov"))
            oldContentType = "video/quicktime";
        if (expName.equalsIgnoreCase("lsf"))
            oldContentType = "video/x-la-asf";
        if (expName.equalsIgnoreCase("lsx"))
            oldContentType = "video/x-la-asf";
        if (expName.equalsIgnoreCase("asf"))
            oldContentType = "video/x-ms-asf";
        if (expName.equalsIgnoreCase("asr"))
            oldContentType = "video/x-ms-asf";
        if (expName.equalsIgnoreCase("asx"))
            oldContentType = "video/x-ms-asf";

        if (expName.equalsIgnoreCase("avi"))
            oldContentType = "video/x-msvideo";
        if (expName.equalsIgnoreCase("movie"))
            oldContentType = "video/x-sgi-movie";
        if (expName.equalsIgnoreCase("wv"))
            oldContentType = "video/wavelet";
        if (expName.equalsIgnoreCase("wvx"))
            oldContentType = "video/x-ms-wvx";
        if (expName.equalsIgnoreCase("wmv"))
            oldContentType = "video/x-ms-wmv";
        if (expName.equalsIgnoreCase("wmx"))
            oldContentType = "video/x-ms-wmx";
        if (expName.equalsIgnoreCase("3gp"))
            oldContentType = "video/3gpp";
        if (expName.equalsIgnoreCase("asf"))
            oldContentType = "video/x-ms-asf";

        if (expName.equalsIgnoreCase("asx"))
            oldContentType = "video/x-ms-asf";
        if (expName.equalsIgnoreCase("avi"))
            oldContentType = "video/x-msvideo";
        if (expName.equalsIgnoreCase("fvi"))
            oldContentType = "video/isivideo";
        if (expName.equalsIgnoreCase("lsf"))
            oldContentType = "video/x-ms-asf";
        if (expName.equalsIgnoreCase("mng"))
            oldContentType = "video/x-mng";
        if (expName.equalsIgnoreCase("mp4"))
            oldContentType = "video/mp4";
        if (expName.equalsIgnoreCase("mpg4"))
            oldContentType = "video/mp4";
        if (expName.equalsIgnoreCase("pvx"))
            oldContentType = " video/x-pv-pvx";
        if (expName.equalsIgnoreCase("qt"))
            oldContentType = "video/quicktime";
        if (expName.equalsIgnoreCase("rv"))
            oldContentType = "video/vnd.rn-realvideo";

        if (expName.equalsIgnoreCase("vdo"))
            oldContentType = "video/vdo";
        if (expName.equalsIgnoreCase("viv"))
            oldContentType = "video/vivo";
        if (expName.equalsIgnoreCase("vivo"))
            oldContentType = "video/vivo";
        if (expName.equalsIgnoreCase("wm"))
            oldContentType = "video/x-ms-wm";
        if (expName.equalsIgnoreCase("wmv"))
            oldContentType = "video/x-ms-wmv";
        if (expName.equalsIgnoreCase("wmx"))
            oldContentType = "video/x-ms-wmx";

        if (expName.equalsIgnoreCase("flv"))
            oldContentType = "video/x-flv";
        if (expName.equalsIgnoreCase("f4v"))
            oldContentType = "video/x-f4v";
        if (expName.equalsIgnoreCase("rm"))
            oldContentType = "video/vnd.rn-realvideo";
        if (expName.equalsIgnoreCase("rmvb"))
            oldContentType = "video/vnd.rn-realvideo";

        //audio
        if (expName.equalsIgnoreCase("aif"))
            oldContentType = "audio/x-aiff";
        if (expName.equalsIgnoreCase("aifc"))
            oldContentType = "audio/x-aiff";
        if (expName.equalsIgnoreCase("aiff"))
            oldContentType = "audio/x-aiff";
        if (expName.equalsIgnoreCase("als"))
            oldContentType = "audio/X-Alpha5";
        if (expName.equalsIgnoreCase("au"))
            oldContentType = "audio/basic";
        if (expName.equalsIgnoreCase("awb"))
            oldContentType = "audio/amr-wb";
        if (expName.equalsIgnoreCase("es"))
            oldContentType = "audio/echospeech";
        if (expName.equalsIgnoreCase("esl"))
            oldContentType = "audio/echospeech";
        if (expName.equalsIgnoreCase("imy"))
            oldContentType = "audio/melody";
        if (expName.equalsIgnoreCase("it"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("itz"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("m15"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("m3u"))
            oldContentType = "audio/x-mpegurl";
        if (expName.equalsIgnoreCase("m3url"))
            oldContentType = "audio/x-mpegurl";
        if (expName.equalsIgnoreCase("ma1"))
            oldContentType = "audio/ma1";
        if (expName.equalsIgnoreCase("ma2"))
            oldContentType = "audio/ma2";
        if (expName.equalsIgnoreCase("ma3"))
            oldContentType = "audio/ma3";
        if (expName.equalsIgnoreCase("ma5"))
            oldContentType = "audio/ma5";
        if (expName.equalsIgnoreCase("mdz"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("mid"))
            oldContentType = "audio/midi";
        if (expName.equalsIgnoreCase("midi"))
            oldContentType = "audio/midi";
        if (expName.equalsIgnoreCase("mio"))
            oldContentType = "audio/x-mio";
        if (expName.equalsIgnoreCase("mod"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("mp2"))
            oldContentType = "audio/x-mpeg";
        if (expName.equalsIgnoreCase("mp3"))
            oldContentType = "audio/x-mpeg";
        if (expName.equalsIgnoreCase("mpga"))
            oldContentType = "audio/mpeg";
        if (expName.equalsIgnoreCase("nsnd"))
            oldContentType = "audio/nsnd";
        if (expName.equalsIgnoreCase("pac"))
            oldContentType = "audio/x-pac";
        if (expName.equalsIgnoreCase("pae"))
            oldContentType = "audio/x-epac";
        if (expName.equalsIgnoreCase("qcp"))
            oldContentType = "audio/vnd.qcelp";
        if (expName.equalsIgnoreCase("ra"))
            oldContentType = "audio/x-pn-realaudio";
        if (expName.equalsIgnoreCase("ram"))
            oldContentType = "audio/x-pn-realaudio";
        if (expName.equalsIgnoreCase("s3m"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("s3z"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("smd"))
            oldContentType = "audio/x-smd";
        if (expName.equalsIgnoreCase("smz"))
            oldContentType = "audio/x-smd";
        if (expName.equalsIgnoreCase("snd"))
            oldContentType = "audio/basic";
        if (expName.equalsIgnoreCase("stm"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("tsi"))
            oldContentType = "audio/tsplayer";
        if (expName.equalsIgnoreCase("ult"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("vib"))
            oldContentType = "audio/vib";
        if (expName.equalsIgnoreCase("wav"))
            oldContentType = "audio/x-wav";
        if (expName.equalsIgnoreCase("wax"))
            oldContentType = "audio/x-ms-wax";
        if (expName.equalsIgnoreCase("wma"))
            oldContentType = "audio/x-ms-wma";
        if (expName.equalsIgnoreCase("xm"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("xmz"))
            oldContentType = "audio/x-mod";
        if (expName.equalsIgnoreCase("rmi"))
            oldContentType = "audio/mid";
        return oldContentType;
    }

    public static final int POST_SOURCE_PEOPLE=1;
    public static final int POST_SOURCE_SYSTEM=2;
    public static final String POST_FULL_COLUMNS = "post_id, source, created_time, updated_time, " +
            "quote, root, mentions, app, type, app_data, message, device, can_comment, can_like,can_reshare,add_to,privince, attachments,destroyed_time,target,location,add_contact,has_contact,longitude,latitude,post_source";


    private static final Map<String, String> POST_COLUMNS = CollectionUtils2.of(
            "full", POST_FULL_COLUMNS);

    public static String parsePostColumns(String cols) {
        return expandColumns(cols, POST_COLUMNS, POST_FULL_COLUMNS);
    }

    private static String expandColumns(String cols, Map<String, String> macros, String def) {
        StringBuilder buff = new StringBuilder();
        for (String col : StringUtils2.splitList(cols, ",", true)) {
            if (col.startsWith("#")) {
                String val = macros.get(StringUtils.removeStart(col, "#"));
                if (val == null)
                    val = def;
                buff.append(val);
            } else {
                buff.append(col);
            }
            buff.append(",");
        }
        return StringUtils2.stripItems(buff.toString(), ",", true);
    }

    public static final String FULL_COMMENT_COLUMNS = "comment_id, target, created_time, " +
            "commenter, commenter_name, message, device, can_like,destroyed_time,add_to";

    public static final String USER_ALL_COLUMNS =
            "user_id, password, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name,  remark,display_name,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";

    public static final String USER_FULL_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";

    public static final String USER_STANDARD_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time,gender, birthday,company, department, job_title, office_address, profession, job_description,  contact_info,  family,  address,   work_history_updated_time, miscellaneous,  in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history,top_posts,top_name,shared_count";


    public static final String USER_LIGHT_COLUMNS =
            "user_id, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3,display_name,perhaps_name, image_url, remark, in_circles, his_friend, bidi";
    public static final String USER_LIGHT_COLUMNS_USER_SHOW =
            "user_id, display_name, image_url, remark, in_circles, his_friend, bidi,perhaps_name";

    public static final String USER_LIGHT_COLUMNS_QIUPU =
            "user_id, display_name, image_url, address,perhaps_name";

    public static final String USER_LIGHT_COLUMNS_LIGHT =
            "user_id, display_name, image_url,perhaps_name";

    public static final String USER_COLUMNS_SHAK =
            "user_id, display_name, remark,perhaps_name,image_url, status, gender, in_circles, his_friend, bidi";

    public static final String QAPK_COLUMNS =
            "package,app_name,version_code,version_name,architecture,target_sdk_version,category,sub_category,"
                    + "created_time,info_updated_time,description,recent_change,rating,"
                    + "download_count,install_count,uninstall_count,favorite_count,upload_user,screen_support,icon_url,price,borqs,"
                    + "market_url,file_size,file_url,tag,screenshots_urls";

    public static final String QAPK_FULL_COLUMNS = QAPK_COLUMNS
            + ",app_comment_count,app_comments,app_like_count,app_liked_users,"
            + "app_likes,compatibility,app_used,app_favorite,lasted_version_code,lasted_version_name";
}
