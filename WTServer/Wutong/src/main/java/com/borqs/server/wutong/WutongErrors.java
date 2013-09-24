package com.borqs.server.wutong;


import com.borqs.server.Errors;

public class WutongErrors extends Errors {
    //==============================NEW ERROR CODE================================
    //==============================NEW ERROR CODE================================
    //==============================NEW ERROR CODE================================
    //==============================NEW ERROR CODE================================
    //SYSTEM ERROR
    public static final int SYSTEM_ERROR_CODE = 10000;

    public static final int SYSTEM_ERROR = SYSTEM_ERROR_CODE + 1;// 10001 //System error //系统错误
    public static final int SYSTEM_DB_ERROR = SYSTEM_ERROR_CODE + 2;//10002//DB error//数据库操作错误
    public static final int SYSTEM_SERVICE_UNAVAILABLE = SYSTEM_ERROR_CODE + 2;//10002 //Service unavailable //服务暂停
    public static final int SYSTEM_REMOTE_SERVICE_ERROR = SYSTEM_ERROR_CODE + 3;//10003 //Remote service error //远程服务错误
    public static final int SYSTEM_IP_LIMIT = SYSTEM_ERROR_CODE + 4;//10004 //IP limit //IP限制不能请求该资源
    public static final int SYSTEM_PERMISSION_DENIED = SYSTEM_ERROR_CODE + 5;//10005 //Permission denied, need a high level app id//该资源需要appkey拥有授权
    public static final int SYSTEM_MISSING_PARAMETER = SYSTEM_ERROR_CODE + 6;//10006 //Source paramter (app id) is missing //缺少app id
    public static final int SYSTEM_UNSUPPORTED_MEDIA_TYPE = SYSTEM_ERROR_CODE + 7;//10007 //Unsupport mediatype (%s) //不支持的MediaType (%s)
    public static final int SYSTEM_TOO_MANY_TASKS = SYSTEM_ERROR_CODE + 9;//10009 //Too many pending tasks, system is busy //任务过多，系统繁忙
    public static final int SYSTEM_JOB_EXPIRED = SYSTEM_ERROR_CODE + 10;//10010 //Job expired //任务超时
    public static final int SYSTEM_ILLEGAL_REQUEST = SYSTEM_ERROR_CODE + 11;//10012 //Illegal request //非法请求
    public static final int SYSTEM_MISS_REQUIRED_PARAMETER = SYSTEM_ERROR_CODE + 12;//10016 //Miss required parameter (%s) //缺失必选参数 (%s)
    public static final int SYSTEM_PARAMETER_TYPE_ERROR = SYSTEM_ERROR_CODE + 17;//10017 //Parameter (%s)'s value invalid, expect (%s) , but get (%s) , see doc for more info //参数值非法，需为 (%s)，实际为 (%s)
    public static final int SYSTEM_REQUEST_BODY_OVER_LIMIT = SYSTEM_ERROR_CODE + 18;//10018 //Request body length over limit //请求长度超过限制
    public static final int SYSTEM_REQUEST_API_NOT_FOUND = SYSTEM_ERROR_CODE + 20;//10020 //Request api not found //接口不存在
    public static final int SYSTEM_HTTP_METHOD_NOT_SUPPORT = SYSTEM_ERROR_CODE + 21;//10021 //HTTP method is not supported for this request //请求的HTTP METHOD不支持，请检查是否选择了正确的POST/GET方式
    public static final int SYSTEM_IP_REQUEST_LIMIT = SYSTEM_ERROR_CODE + 22;//10022 //IP requests out of rate limit //IP请求频次超过上限
    public static final int SYSTEM_USER_REQUEST_OUT_OF_LIMIT = SYSTEM_ERROR_CODE + 23;//10023 //User requests out of rate limit //用户请求频次超过上限
    public static final int SYSTEM_MESSAGE_GATEWAY_HOST_ERROR = SYSTEM_ERROR_CODE + 25;//10025//Message Gateway error//短信网关工作异常
    public static final int SYSTEM_MESSAGE_GATEWAY_SEND_ERROR = SYSTEM_ERROR_CODE + 26;//10025//Message Gateway error//短信网关工作异常

    public static final int SERVICE_ERROR_CODE = 20000;

    //AUTH ERROR   20100
    public static final int AUTH_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 100;
    public static final int AUTH_FAILED = AUTH_SERVICE_ERROR_CODE + 1;//20101//Auth faild //认证失败
    public static final int AUTH_LOGIN_ERROR = AUTH_SERVICE_ERROR_CODE + 2;//20102//Username or password error //用户名或密码不正确
    public static final int AUTH_OUT_OF_LIMIT = AUTH_SERVICE_ERROR_CODE + 3;//20103//Username and pwd auth out of rate limit //用户名密码认证超过请求限制
    public static final int AUTH_SIGNATURE_ERROR = AUTH_SERVICE_ERROR_CODE + 4;//20104//Signature invalid //签名值不合法
    public static final int AUTH_TICKET_USED = AUTH_SERVICE_ERROR_CODE + 5;//20105//Token used //Token已经被使用
    public static final int AUTH_TICKET_EXPIRED = AUTH_SERVICE_ERROR_CODE + 6;//20106//Token expired //Token已经过期
    //public static final int AUTH_TICKET_INVALID = AUTH_SERVICE_ERROR_CODE + 7;//106//Token revoked //Token不合法
    public static final int AUTH_TICKET_INVALID = 106;
    public static final int MD5_INVALID = 107;
    public static final int AUTH_LOGIN_NAME_EXISTS = AUTH_SERVICE_ERROR_CODE + 8;
    public static final int AUTH_REQUEST_TOO_FREQUENT = AUTH_SERVICE_ERROR_CODE + 9;
    public static final int AUTH_VERIFICATION_ERROR = AUTH_SERVICE_ERROR_CODE + 10;
    public static final int AUTH_VERIFY_TOO_FREQUENT = AUTH_SERVICE_ERROR_CODE + 11;
    public static final int AUTH_APP_VERIFY_ERROR = AUTH_SERVICE_ERROR_CODE + 12;


    //用户相关 编码为20200
    //USER ERROR
    public static final int USER_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 200;
    public static final int USER_NOT_EXISTS = 211;//211//User does not exists //用户不存在或已经被删除   OLD
    public static final int USER_NAME_PASSWORD_ERROR = 209;//209//User NAME OR PWD ERROR //用户名或密码错误   OLD
    public static final int USER_EMAIL_NOT_EXISTS = USER_SERVICE_ERROR_CODE + 2;//20202//Email address does not exists//不存在的email地址
    public static final int USER_PHONE_NUMBER_NOT_EXISTS = USER_SERVICE_ERROR_CODE + 3;//20203//Phone number does not exists//不存在的手机号码
    public static final int USER_EMAIL_USED = USER_SERVICE_ERROR_CODE + 4;//20204//The email has been used //该email已经被使用
    public static final int USER_PHONE_USED = USER_SERVICE_ERROR_CODE + 5;//20205//The phone number has been used //该手机号已经被使用
    public static final int USER_HAS_BIND_EMAIL = USER_SERVICE_ERROR_CODE + 6;//20206//The account has been bind phone //该用户已经绑定email
    public static final int USER_HAS_BIND_PHONE = USER_SERVICE_ERROR_CODE + 7;//20207//The account has been bind phone //该用户已经绑定手机
    public static final int USER_LASTED_BIND_ITEM = USER_SERVICE_ERROR_CODE + 8;//20208//The lasted bind//最后一个绑定项，不允许解绑
    public static final int USER_GENERAL_ID_ERROR = USER_SERVICE_ERROR_CODE + 9;//20209//
    public static final int USER_GENERAL_SESSION_ERROR = USER_SERVICE_ERROR_CODE + 10;//20210//
    public static final int USER_LOGIN_NAME_EXISTED = USER_SERVICE_ERROR_CODE + 11;//20211//
    public static final int USER_VERIFY_LINK_EXPIRED = USER_SERVICE_ERROR_CODE + 12;//20211//
    public static final int USER_UPDATE_INFO_ERROR_CANT_ACTION_COLUMNS = USER_SERVICE_ERROR_CODE + 13;//20211//
    public static final int USER_UPDATE_INFO_ERROR_ACTION_ERROR = USER_SERVICE_ERROR_CODE + 14;//20211//
    public static final int USER_PHONE_EMAIL_BIND_BY_OTHERS = USER_SERVICE_ERROR_CODE + 15;//20209//
    public static final int USER_PHONE_EMAIL_BIND_LIMIT_COUNT = USER_SERVICE_ERROR_CODE + 16;//20209//
    public static final int USER_UPDATE_PWD_ERROR = USER_SERVICE_ERROR_CODE + 17;//20209//
    public static final int USER_CHECK_ERROR = USER_SERVICE_ERROR_CODE + 25;//20225//

    //Stream相关 编码为20300
    //STREAM ERROR
    public static final int STREAM_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 300;
    public static final int STREAM_NOT_EXISTS = STREAM_SERVICE_ERROR_CODE + 1;//20301//Target post does not exist //Stream不存在
    public static final int STREAM_DELETED = 119;//119//Target post does not exist //Stream被删除
    public static final int STREAM_EMPTY_CONTENT = STREAM_SERVICE_ERROR_CODE + 3;//20303//Content is null //内容为空
    public static final int STREAM_IDS_TOO_LONG = STREAM_SERVICE_ERROR_CODE + 4;//20304//IDs is too many //IDs参数太长了
    public static final int STREAM_IDS_TOO_LONG_MAX = STREAM_SERVICE_ERROR_CODE + 5;//20305//Text too long, please input text less than 4000 characters //输入文字太长，请确认不超过4000个字符
    public static final int STREAM_TEXT_TOO_LONG_LIGHT = STREAM_SERVICE_ERROR_CODE + 6;//20306//Text too long, please input text less than 300 characters //输入文字太长，请确认不超过300个字符
    public static final int STREAM_ACCOUNT_ILLEGAL = STREAM_SERVICE_ERROR_CODE + 7;//20307//Account or ip or app is illgal, can not continue //账号、IP或应用非法，暂时无法完成此操作
    public static final int STREAM_OUT_OF_LIMIT = STREAM_SERVICE_ERROR_CODE + 8;//20308//Out of limit //发布内容过于频繁
    public static final int STREAM_CONTAIN_ILLEGAL_WEBSITE = STREAM_SERVICE_ERROR_CODE + 9;//20309//Contain illegal website //包含非法网址
    public static final int STREAM_REPEAT_CONTENT = STREAM_SERVICE_ERROR_CODE + 10;//20310//Repeat conetnt //提交相同的信息
    public static final int STREAM_CONTAIN_ADVERTISING = STREAM_SERVICE_ERROR_CODE + 11;//20311//Contain advertising //包含广告信息
    public static final int STREAM_CONTENT_ILLEGAL = STREAM_SERVICE_ERROR_CODE + 12;//20312//Content is illegal //包含非法内容
    public static final int STREAM_IP_ACTION_ILLEGAL = STREAM_SERVICE_ERROR_CODE + 13;//20313//Your ip's behave in a comic boisterous or unruly manner //此IP地址上的行为异常
    public static final int STREAM_NOT_YOUR_OWN_POST = STREAM_SERVICE_ERROR_CODE + 14;//20314//Not your own post//不是你发布的stream
    public static final int STREAM_CANT_REPOST = STREAM_SERVICE_ERROR_CODE + 15;//20315//Can't repost yourself post //不能转发的stream
    public static final int STREAM_CANT_COMMENT = STREAM_SERVICE_ERROR_CODE + 16;//20316//Can't comment //不能评论的stream
    public static final int STREAM_CANT_LIKE = STREAM_SERVICE_ERROR_CODE + 17;//20317//Can't like //不能赞的stream
    public static final int STREAM_ILLEGAL = STREAM_SERVICE_ERROR_CODE + 18;//20318//Illegal post//不合法的stream
    public static final int STREAM_CANT_SHARE_TOO_MANY_PEOPLE = STREAM_SERVICE_ERROR_CODE + 19;//20319//Can't  shared to too many people (400)//最多分享给400个人


    //comment相关 编码为20400
    public static final int COMMENT_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 400;
    public static final int COMMENT_NOT_EXISTS = COMMENT_SERVICE_ERROR_CODE + 1;//20401//Target omment does not exist //不存在的评论
    public static final int COMMENT_ILLEGAL = COMMENT_SERVICE_ERROR_CODE + 2;//20402//Illegal comment //不合法的评论
    public static final int COMMENT_NOT_YOUR_OWN = COMMENT_SERVICE_ERROR_CODE + 3;//20403//Not your own comment //不是你发布的评论
    public static final int COMMENT_CANT_COMMENT = COMMENT_SERVICE_ERROR_CODE + 4;//20404//can't comment //不能评论
    public static final int COMMENT_CANT_LIKE = COMMENT_SERVICE_ERROR_CODE + 5;//20405//can't like//不能赞
    public static final int COMMENT_REPEAT_CONTENT = COMMENT_SERVICE_ERROR_CODE + 6;//20405//重复内容


    //私信相关 编码为20500（暂时没有，预留）
    public static final int MESSAGE_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 500;
    public static final int MESSAGE_NOT_EXISTS = MESSAGE_SERVICE_ERROR_CODE + 1;//20501//Target message  does not exist //不存在的私信
    public static final int MESSAGE_CANT_SEND_FOLLOWER = MESSAGE_SERVICE_ERROR_CODE + 2;//20502//Can't send direct message to user who is not your follower //不能给不是你粉丝的人发私信
    public static final int MESSAGE_ILLEGAL = MESSAGE_SERVICE_ERROR_CODE + 3;//20503//Illegal direct message //不合法的私信
    public static final int MESSAGE_NOT_YOURS = MESSAGE_SERVICE_ERROR_CODE + 4;//20504//Not your own direct message //不是属于你的私信
    public static final int MESSAGE_REPEAT_CONTENT = MESSAGE_SERVICE_ERROR_CODE + 5;//20505//Repeated direct message text //不能发布相同的私信


    //图片相关 编码为20600
    public static final int PHOTO_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 600;
    public static final int PHOTO_ALBUM_NOT_EXISTS = PHOTO_SERVICE_ERROR_CODE + 1;//20601//Target album  does not exist //不存在的相册
    public static final int PHOTO_NOT_EXISTS = PHOTO_SERVICE_ERROR_CODE + 2;//20602//Target photo  does not exist //不存在的照片
    public static final int PHOTO_CONTENT_TYPE_ERROR = PHOTO_SERVICE_ERROR_CODE + 3;//20603//Unsupported image type, only suport JPG, GIF, PNG //不支持的图片类型，仅仅支持JPG、GIF、PNG
    public static final int PHOTO_TOO_LARGE_SIZE = PHOTO_SERVICE_ERROR_CODE + 4;//20604//Image size too large //图片太大
    public static final int PHOTO_UPLOAD_METHOD_ERROR = PHOTO_SERVICE_ERROR_CODE + 5;//20605//Does multipart has image //请确保使用multpart上传图片
    public static final int PHOTO_ALBUM_TYPE_ERROR = PHOTO_SERVICE_ERROR_CODE + 6;//20606//相册类型错误
    public static final int PHOTO_CANT_ACTION = PHOTO_SERVICE_ERROR_CODE + 7;//20607//不允许操作
    public static final int PHOTO_PRIVACY_TYPE_ERROR = PHOTO_SERVICE_ERROR_CODE + 8;//20608//隐私类型错误
    public static final int PHOTO_UPLOAD_OR_RESIZE_ERROR = PHOTO_SERVICE_ERROR_CODE + 12;//20612//图片上传错误


    //Files相关编码为20700
    public static final int FILE_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 700;
    public static final int FILE_FOLDER_NOT_EXISTS = FILE_SERVICE_ERROR_CODE + 1;//20701//Target folder does not exist //不存在的文件夹
    public static final int FILE_NOT_EXISTS = FILE_SERVICE_ERROR_CODE + 2;//20702//Target file  does not exist //不存在的文件
    public static final int FILE_TOO_LARGE_SIZE = FILE_SERVICE_ERROR_CODE + 3;//20703//File size too large //文件太大
    public static final int FILE_UPLOAD_ERROR = FILE_SERVICE_ERROR_CODE + 4;//20704//Upload file error//上传文件失败
    public static final int FOLDER_CANT_UPDATE = FILE_SERVICE_ERROR_CODE + 5;//20705//CANT UPDATE
    public static final int FOLDER_PRIVACY_ERROR = FILE_SERVICE_ERROR_CODE + 6;//20706//PRIVACY ERROR
    public static final int FOLDER_CANT_DELETE = FILE_SERVICE_ERROR_CODE + 7;//20707//CANT DELETE

    //audio相关编码为28000
    public static final int AUDIO_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 800;
    public static final int AUDIO_NOT_EXISTS = AUDIO_SERVICE_ERROR_CODE + 1;//20801//Target audio does not exist //不存在的音频文件
    public static final int AUDIO_TOO_LARGE_SIZE = AUDIO_SERVICE_ERROR_CODE + 2;//20802 //Audio size too large //音频文件太大


    //video相关编码为20900
    public static final int VIDEO_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 900;
    public static final int VIDEO_NOT_EXISTS = VIDEO_SERVICE_ERROR_CODE + 1;//20901//Target video does not exist //不存在的视频文件
    public static final int VIDEO_TOO_LARGE_SIZE = VIDEO_SERVICE_ERROR_CODE + 2;//20902//Video size too large //视频文件太大


    //好友编码为21000
    public static final int FRIENDSHIP_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1000;
    public static final int FRIEND_CANT_FOLLOW_YOURSELF = FRIENDSHIP_SERVICE_ERROR_CODE + 1;//21001//Can not follow yourself //你不能关注自己
    public static final int FRIEND_OUT_OF_REQUEST_LIMIT = FRIENDSHIP_SERVICE_ERROR_CODE + 2;//21002//Social graph updates out of rate limit //加关注请求超过上限
    public static final int FRIEND_FOLLOWED_HIM = FRIENDSHIP_SERVICE_ERROR_CODE + 3;//21003//Already followed //已经关注此用户
    public static final int FRIEND_IN_YOUR_BLACKLIST = FRIENDSHIP_SERVICE_ERROR_CODE + 4;//21004//Please delete the user from you blacklist before you follow the user //你已经把此用户加入黑名单，加关注前请先解除
    public static final int FRIEND_NOT_FOLLOW_THIS_USER = FRIENDSHIP_SERVICE_ERROR_CODE + 5;//21005//Not followed //还未关注此用户
    public static final int FRIEND_NOT_FOLLOWER = FRIENDSHIP_SERVICE_ERROR_CODE + 6;//21006//Not followers //还不是粉丝
    public static final int FRIEND_GENERATE_CIRCLE_ID = FRIENDSHIP_SERVICE_ERROR_CODE + 7;       // 生成CircleID错误
    public static final int FRIEND_CIRCLE_EXISTS = FRIENDSHIP_SERVICE_ERROR_CODE + 8;            // Circle已经存在
    public static final int FRIEND_TOO_MANY_CIRCLES = FRIENDSHIP_SERVICE_ERROR_CODE + 9;         // Circle太多
    public static final int FRIEND_SAVE_CIRCLE = FRIENDSHIP_SERVICE_ERROR_CODE + 10;              // 保存Circle错误
    public static final int FRIEND_ILLEGAL_CIRCLE = FRIENDSHIP_SERVICE_ERROR_CODE + 11;           // Circle不存在

    //GROUP 编码为21100
    public static final int GROUP_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1100;
    public static final int GROUP_CREATE_ERROR = GROUP_SERVICE_ERROR_CODE + 1;
    public static final int GROUP_NOT_EXISTS = GROUP_SERVICE_ERROR_CODE + 2;
    public static final int GROUP_ADMIN_QUIT_ERROR = GROUP_SERVICE_ERROR_CODE + 3;
    public static final int GROUP_RIGHT_ERROR = GROUP_SERVICE_ERROR_CODE + 4;
    public static final int GROUP_CANNOT_APPLY = GROUP_SERVICE_ERROR_CODE + 5;
    public static final int GROUP_MEMBER_OUT_OF_LIMIT = GROUP_SERVICE_ERROR_CODE + 6;
    public static final int GROUP_NOT_FORMAL = GROUP_SERVICE_ERROR_CODE + 7;
    public static final int GROUP_NOT_FOUND_MEMBER_IN_MEMBER_LIST = GROUP_SERVICE_ERROR_CODE + 11;


    //POLL 编码为21200
    public static final int POLL_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1200;
    public static final int POLL_NOT_EXISTS = POLL_SERVICE_ERROR_CODE + 1;
    public static final int POLL_CANT_COMMENT = POLL_SERVICE_ERROR_CODE + 2;
    public static final int POLL_VOTE_ITEMS_OUT_OF_LIMIT = POLL_SERVICE_ERROR_CODE + 3;
    public static final int POLL_ADD_ITEMS_ERROR = POLL_SERVICE_ERROR_CODE + 4;

    //COMPANY 编码为21300
    public static final int COMPANY_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1300;
    public static final int COMPANY_EMPLOYEE_ERROR = COMPANY_SERVICE_ERROR_CODE + 1;


    //NOTIFICATION 编码为21400
    public static final int NOTIFICATION_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1400;
    public static final int NOTIFICATION_SEND_ERROR = NOTIFICATION_SERVICE_ERROR_CODE + 1;

    //LIKE 编码为21500
    public static final int LIKE_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 1500;
    public static final int LIKE_OBJECT_CANT_LIKE = LIKE_SERVICE_ERROR_CODE + 1;

    //Application相关编码为26000
    public static final int APPLICATION_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 6000;
    public static final int APPLICATION_NOT_EXISTS = APPLICATION_SERVICE_ERROR_CODE + 1;//26001//Target application does not exist//应用不存在
    public static final int APPLICATION_BACKUP_FAILED = APPLICATION_SERVICE_ERROR_CODE + 2;//26002//Application backup error//应用备份失败

    // Page相关编码为27000
    public static final int PAGE_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 7000;
    public static final int PAGE_CREATE_ERROR = PAGE_SERVICE_ERROR_CODE + 1;
    public static final int PAGE_ILLEGAL_PERMISSION = PAGE_SERVICE_ERROR_CODE + 2;
    public static final int PAGE_ILLEGAL = PAGE_SERVICE_ERROR_CODE + 3;
    public static final int PAGE_ILLEGAL_ASSOCIATED_ID = PAGE_SERVICE_ERROR_CODE + 4;
    public static final int PAGE_CIRCLE_ASSOCIATED = PAGE_SERVICE_ERROR_CODE + 5;
    public static final int PAGE_ASSOCIATED = PAGE_SERVICE_ERROR_CODE + 6;

    // Search
    public static final int SEARCH_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 8000;
    public static final int SEARCH_QUERY_ERROR = SEARCH_SERVICE_ERROR_CODE + 1;
    public static final int SEARCH_ADD_ERROR = SEARCH_SERVICE_ERROR_CODE + 2;
    public static final int SEARCH_DELETE_ERROR = SEARCH_SERVICE_ERROR_CODE + 3;

    //其他功能相关编码为29000
    public static final int COMMON_SERVICE_ERROR_CODE = SERVICE_ERROR_CODE + 9000;
    public static final int COMMON_GEO_ERROR = COMMON_SERVICE_ERROR_CODE + 1;//29001//Geo code input error //地理信息输入错误
    public static final int FORCE_VERSION_UPDATE = 391;//29002//Force version update//强制用户升级
    public static final int CONVERSATION_NOT_EXISTS = COMMON_SERVICE_ERROR_CODE + 3;


}
