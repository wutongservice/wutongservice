package com.borqs.server.service.platform;


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

    public static final int ALL_POST = TEXT_POST | VIDEO_POST
            | AUDIO_POST | BOOK_POST | APK_POST | LINK_POST | APK_LINK_POST | APK_COMMENT_POST | APK_LIKE_POST | BOOK_LIKE_POST | BOOK_COMMENT_POST | FRIEND_SET_POST
            | MUSIC_POST | MUSIC_COMMENT_POST | MUSIC_LIKE_POST | PHOTO_POST | SIGN_IN_POST;


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
    public static final TextCollection OBJECTS = TextCollection.of(
            "user", USER_OBJECT, "post", POST_OBJECT, "video", VIDEO_OBJECT,
            "apk", APK_OBJECT, "music", MUSIC_OBJECT, "comment", COMMENT_OBJECT, "like", LIKE_OBJECT, "book", BOOK_OBJECT, "link", LINK_OBJECT, "photo", PHOTO_OBJECT);
    

    public static final int CONVERSATION_SHARE = 1;
    public static final int CONVERSATION_RESHARE = 2;
    public static final int CONVERSATION_COMMENT = 3;
    public static final int CONVERSATION_LIKE = 4;
    public static final int CONVERSATION_FAVORITE = 5;

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
    public static final String NTF_MY_APP_LIKE = "ntf.my_app_like";
    public static final String NTF_MY_APP_RETWEET = "ntf.my_app_retweet";
    public static final String NTF_INVOLVED_STREAM_COMMENT = "ntf.involved_stream_comment";
    public static final String NTF_INVOLVED_STREAM_LIKE = "ntf.involved_stream_like";
    public static final String NTF_INVOLVED_APP_COMMENT = "ntf.involved_app_comment";
    public static final String NTF_INVOLVED_APP_LIKE = "ntf.involved_app_like";
    public static final String NTF_FRIENDS_ONLINE = "ntf.friends_online";
    public static final String NTF_APP_SHARE = "ntf.app_share";
    public static final String NTF_OTHER_SHARE = "ntf.other_share";
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

    //group
    public static final long PUBLIC_CIRCLE_ID_BEGIN = 10000000000L;
    public static final long ACTIVITY_ID_BEGIN = 12000000000L;
    public static final long ORGANIZATION_ID_BEGIN = 14000000000L;
    public static final long GROUP_ID_END = 20000000000L;

    public static final int ROLE_CREATOR = 100;
    public static final int ROLE_ADMIN = 10;
    public static final int ROLE_MEMBER = 1;

    public static final String GRP_COL_ID = "id";
    public static final String GRP_COL_NAME = "name";
    public static final String GRP_COL_MEMBER_LIMIT = "member_limit";
    public static final String GRP_COL_IS_STREAM_PUBLIC = "is_stream_public";
    public static final String GRP_COL_CAN_SEARCH = "can_search";
    public static final String GRP_COL_CAN_VIEW_MEMBERS = "can_view_members";
    public static final String GRP_COL_CAN_JOIN = "can_join";
    public static final String GRP_COL_CREATOR = "creator";
    public static final String GRP_COL_CREATED_TIME = "created_time";
    public static final String GRP_COL_UPDATED_TIME = "updated_time";
    public static final String GRP_COL_DESTROYED_TIME = "destroyed_time";
    public static final String GRP_COL_MEMBERS = "members";
    
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
		while(iter.hasNext())
		{
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
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

    public static String parseUserAgent(String device, String key)
    {
    	List<String> l = StringUtils2.splitList(device, ";", true);
    	for(String str : l)
    	{
    		if(str.contains(key))
    		{
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

    public static String getBundleString(String ua, String key)
    {
        String lang = "zh";
        if(StringUtils.isNotBlank(ua))
        {
           lang = Constants.parseUserAgent(ua, "lang").equalsIgnoreCase("US") ? "en" : "zh";
        }
        if(StringUtils.isBlank(lang))
        {
            lang = "zh";
        }

        return getBundleStringByLang(lang, key);
    }
    
    public static String getBundleStringByLang(String lang, String key)
    {
        ResourceBundle bundle = I18nHelper.getBundle("com.borqs.server.platform.i18n.platform", new Locale(lang));
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

    public static final int C_STREAM_POST = 1;
    public static final int C_STREAM_RESHARE = 2;
    public static final int C_STREAM_COMMENT = 3;
    public static final int C_STREAM_LIKE = 4;
    public static final int C_STREAM_FAVORITE = 5;
    public static final int C_STREAM_IGNORE = 6;
    public static final int C_STREAM_TO = 7;
    public static final int C_STREAM_ADDTO = 8;

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

    public static final int IGNORE_USER = 1;
    public static final int IGNORE_STREAM = 2;
    public static final int IGNORE_COMMENT = 3;
}
