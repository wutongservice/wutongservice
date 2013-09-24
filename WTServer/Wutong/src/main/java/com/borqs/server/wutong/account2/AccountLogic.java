package com.borqs.server.wutong.account2;


import com.borqs.server.base.context.Context;
import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.wutong.account2.user.User;
import org.apache.avro.AvroRemoteException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface AccountLogic {
    String USER_ALL_COLUMNS =
            "user_id, password, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name,  remark,display_name,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";
    String USER_FULL_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time, first_name, middle_name, last_name, gender, birthday, timezone, interests, languages, marriage, religion, about_me, profile_updated_time, company, department, job_title, office_address, profession, job_description, business_updated_time, contact_info, contact_info_updated_time, family, coworker, address, address_updated_time, work_history, work_history_updated_time, education_history, education_history_updated_time, miscellaneous, in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history";
    String USER_STANDARD_COLUMNS =
            "user_id,login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3, domain_name, display_name, remark,perhaps_name,first_name,middle_name,last_name, created_time, last_visited_time, image_url, small_image_url, large_image_url, basic_updated_time, status, status_updated_time,gender, birthday,company, department, job_title, office_address, profession, job_description,  contact_info,  family,  address,   work_history_updated_time, miscellaneous,  in_circles, his_friend, bidi,friends_count,followers_count,favorites_count,work_history,education_history,top_posts,top_name,language";
    String USER_LIGHT_COLUMNS =
            "user_id, login_email1, login_email2, login_email3, login_phone1, login_phone2, login_phone3,display_name,perhaps_name, image_url, small_image_url, large_image_url, remark, in_circles, his_friend, bidi";
    String USER_LIGHT_COLUMNS_USER_SHOW =
            "user_id, display_name, image_url, remark, in_circles, his_friend, bidi,perhaps_name";
    String USER_LIGHT_COLUMNS_QIUPU =
            "user_id, display_name, image_url, address,perhaps_name";
    String USER_LIGHT_COLUMNS_LIGHT =
            "user_id, display_name, image_url,perhaps_name";
    String USER_COLUMNS_SHAK =
            "user_id, display_name, remark,perhaps_name,image_url, status, gender, in_circles, his_friend, bidi";

    //-------------new platform migrate interface below-------------

    User createUser(Context ctx, User user);

    boolean destroyUser(Context ctx, String userId);

    boolean recoverUser(Context ctx, String userId);

    boolean update(Context ctx, User user);

    String resetRandomPassword(Context ctx, String userId);

    void updatePassword(Context ctx, String userId, String oldPwd, String newPwd, boolean verify);

    List<User> getUsers(Context ctx, long... userIds);

    User getUser(Context ctx, long userId);

    String getPassword(Context ctx, long userId);

    boolean hasAllUser(Context ctx, long... userIds);

    boolean hasAnyUser(Context ctx, long... userIds);

    boolean hasUser(Context ctx, long userId);

    long[] getExistsIds(Context ctx, long... userIds) throws SQLException;

    //----------------avro interface below------------------

    Record login(Context ctx, String loginName, String password, String appId);

    boolean logout(Context ctx, String ticket);

    String whoLogined(Context ctx, String ticket);

    RecordSet getLogined(Context ctx, String userId, String appId);

    boolean checkLoginNameNotExists(Context ctx, String uid, String names);

    boolean checkBindNameNotExists(Context ctx, String names);

    //RecordSet getUsersAuths(String userIds);

    //String getNowGenerateUserId();

    String createAccount(Context ctx, Record info);

    boolean destroyAccount(Context ctx, String userId);

    String resetPassword(Context ctx, String loginName);

    boolean updateAccount(Context ctx, String userId, Record info);

    boolean bindUser(Context ctx, String userId, Record info);

    RecordSet findAllUserIds(Context ctx, boolean all);

    RecordSet getUsersPasswordByUserIds(Context ctx, String userIds);

    boolean changePasswordByUserId(Context ctx, String userId, String password);

    RecordSet getUsers(Context ctx, String userIds, String cols);

    RecordSet findUidByMiscellaneous(Context ctx, String miscellaneous);

    RecordSet getUserIds(Context ctx, String loginNames);

    RecordSet getUserIdsByNames(Context ctx, String loginNames);

    RecordSet hasUsers(Context ctx, String userIds);

    RecordSet searchUserByUserName(Context ctx, String username, int page, int count);

    boolean hasOneUsers(Context ctx, String userIds);

    boolean hasAllUsers(Context ctx, String userIds);

    String findUserIdByUserName(Context ctx, String username);

    boolean setPrivacy(Context ctx, String userId, RecordSet privacyItemList);

    RecordSet getAuths(Context ctx, String userId, String resources);

    boolean getDefaultPrivacy(Context ctx, String resource, String circleId);

    boolean saveShortUrl(Context ctx, String long_url, String short_url);

    String findLongUrl(Context ctx, String short_url);

    Record findUidLoginNameNotInID(Context ctx, String name);

    String getBorqsUserIds(Context ctx);

    String getNowGenerateUserId(Context ctx);

    void checkUserIds(Context ctx, String... userIds);

    String createAccount(Context ctx, String login_email1, String login_phone1, String pwd,
                         String displayName, String nickName, String gender, String imei, String imsi, String device, String location) throws IOException;

    Record getUser(Context ctx, String viewerId, String userId, String cols, boolean privacyEnabled);

    Record getUser(Context ctx, String viewerId, String userId, String cols);

    boolean resetPassword(Context ctx, String loginName, String key, String lang) throws AvroRemoteException;

    boolean resetPasswordForPhone(Context ctx, String loginName);

    Record updateUserStatus(Context ctx, String userId, String newStatus, String device, String location, boolean post, boolean can_comment, boolean can_like, boolean can_reshare);

    boolean updateAccount(Context ctx, String userId, Record user, String lang);

    boolean setMiscellaneous(Context ctx, String userId, String phone, String lang);

    int findUidByMiscellaneousPlatform(Context ctx, String miscellaneous);

    RecordSet searchUser(Context ctx, String viewerId, String username, int page, int count);

    boolean bindUserSendVerify(Context ctx, String userId, String phone, String email, String key, String ticket, String lang);

    Record getViewerPrivacyConfig(Context ctx, String viewerId, String resources);

    RecordSet getUsers(Context ctx, String viewerId, String userIds_all, String cols, boolean privacyEnabled, boolean dealTopPosts);

    RecordSet getUsers(Context ctx, String viewerId, String userIds_all, String cols, boolean privacyEnabled);

    RecordSet getUsers(Context ctx, String viewerId, String userIds, String cols);

    String parseUserIds(Context ctx, String viewerId, String userIds);

    String getPerhapsNameP(Context ctx, String url) throws IOException;

    String formatUrlP(Context ctx, String user_id) throws IOException;

    boolean changePassword(Context ctx, String userId, String oldPassword, String newPassword);

    Record genTicketForEmail(Context ctx, String loginName);

    public boolean saveTicket(String ticket, String userId, String appId, int type_);

    boolean updateVisitTime(Context ctx, String userId, long time);

    RecordSet getUserByIdBaseColumns(Context ctx,String userIds);
    RecordSet getAlluser();
    boolean updateAccountForNamSpliter(Context ctx, String userId, Record user);



    RecordSet getUsersBaseColumns(final Context ctx, String userIds_all);
    RecordSet getUsersBaseColumnsContainsFriend(final Context ctx, String viewerId, String userIds_all);
    RecordSet getUsersBaseColumnsContainsRemarkRequest(final Context ctx, String viewerId, String userIds_all);

    List<String> getEmailFromUsers(Context ctx, String userIds);

    List<String> getEmailFromUsers(Context ctx, long userId);
}
