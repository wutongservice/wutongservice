package com.borqs.server.compatible;


import com.borqs.server.platform.data.Addons;
import com.borqs.server.platform.data.Record;
import com.borqs.server.platform.feature.account.MiscInfo;
import com.borqs.server.platform.feature.account.OrgInfo;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.friend.RelUserExpansion;
import com.borqs.server.platform.feature.privacy.PrivacyControlUserExpansion;
import com.borqs.server.platform.feature.privacy.PrivacyResources;
import com.borqs.server.platform.feature.qiupu.AbstractQiupuUserExpansion;
import com.borqs.server.platform.feature.request.RequestUserExpansion;
import com.borqs.server.platform.feature.status.StatusUserExpansion;
import com.borqs.server.platform.util.ColumnsExpander;
import com.borqs.server.platform.util.StringHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;

public class CompatibleUser {

    // basic columns
    public static final String V1COL_USER_ID = "user_id";
    public static final String V1COL_PASSWORD = "password";
    public static final String V1COL_LOGIN_EMAIL1 = "login_email1";
    public static final String V1COL_LOGIN_EMAIL2 = "login_email2";
    public static final String V1COL_LOGIN_EMAIL3 = "login_email3";
    public static final String V1COL_LOGIN_PHONE1 = "login_phone1";
    public static final String V1COL_LOGIN_PHONE2 = "login_phone2";
    public static final String V1COL_LOGIN_PHONE3 = "login_phone3";
    public static final String V1COL_DOMAIN_NAME = "domain_name";
    public static final String V1COL_DISPLAY_NAME = "display_name";
    public static final String V1COL_PERHAPS_NAME = "perhaps_name";
    public static final String V1COL_FIRST_NAME = "first_name";
    public static final String V1COL_MIDDLE_NAME = "middle_name";
    public static final String V1COL_LAST_NAME = "last_name";
    public static final String V1COL_CREATED_TIME = "created_time";
    public static final String V1COL_DESTROYED_TIME = "destroyed_time";
    public static final String V1COL_LAST_VISITED_TIME = "last_visited_time";
    public static final String V1COL_IMAGE_URL = "image_url";
    public static final String V1COL_SMALL_IMAGE_URL = "small_image_url";
    public static final String V1COL_LARGE_IMAGE_URL = "large_image_url";
    public static final String V1COL_BASIC_UPDATED_TIME = "basic_updated_time";

    // status
    public static final String V1COL_STATUS = "status";
    public static final String V1COL_STATUS_UPDATED_TIME = "status_updated_time";

    // profile
    public static final String V1COL_GENDER = "gender";
    public static final String V1COL_BIRTHDAY = "birthday";
    public static final String V1COL_TIMEZONE = "timezone";
    public static final String V1COL_INTERESTS = "interests";
    public static final String V1COL_LANGUAGES = "languages";
    public static final String V1COL_MARRIAGE = "marriage";
    public static final String V1COL_RELIGION = "religion";
    public static final String V1COL_ABOUT_ME = "about_me";
    public static final String V1COL_PROFILE_UPDATED_TIME = "profile_updated_time";

    // business
    public static final String V1COL_COMPANY = "company";
    public static final String V1COL_DEPARTMENT = "department";
    public static final String V1COL_JOB_TITLE = "job_title";
    public static final String V1COL_OFFICE_ADDRESS = "office_address";
    public static final String V1COL_PROFESSION = "profession";
    public static final String V1COL_JOB_DESCRIPTION = "job_description";
    public static final String V1COL_BUSINESS_UPDATED_TIME = "business_updated_time";

    // contact info
    public static final String V1COL_CONTACT_INFO = "contact_info";
    public static final String V1COL_CONTACT_INFO_UPDATED_TIME = "contact_info_updated_time";

    // address
    public static final String V1COL_ADDRESS = "address";
    public static final String V1COL_ADDRESS_UPDATED_TIME = "address_updated_time";

    // work history
    public static final String V1COL_WORK_HISTORY = "work_history";
    public static final String V1COL_WORK_HISTORY_UPDATED_TIME = "work_history_updated_time";

    // education history
    public static final String V1COL_EDUCATION_HISTORY = "education_history";
    public static final String V1COL_EDUCATION_HISTORY_UPDATED_TIME = "education_history_updated_time";

    // other
    public static final String V1COL_FAMILY = "family";
    public static final String V1COL_COWORKER = "coworker";
    public static final String V1COL_MISCELLANEOUS = "miscellaneous";

    // friends
    public static final String V1COL_REMARK = "remark";
    public static final String V1COL_IN_CIRCLES = "in_circles";
    public static final String V1COL_HIS_FRIEND = "his_friend";
    public static final String V1COL_BIDI = "bidi";
    public static final String V1COL_FRIENDS_COUNT = "friends_count";
    public static final String V1COL_FOLLOWERS_COUNT = "followers_count";

    // qiupu
    public static final String V1COL_FAVORITES_COUNT = "favorites_count";

    // privacy
    public static final String V1COL_PROFILE_PRIVACY = "profile_privacy";

    // request
    public static final String V1COL_PENDING_REQUESTS = "pedding_requests";

    public static final String[] V1_LIGHT_COLUMNS = {
            V1COL_USER_ID,
            V1COL_LOGIN_EMAIL1,
            V1COL_LOGIN_EMAIL2,
            V1COL_LOGIN_EMAIL3,
            V1COL_LOGIN_PHONE1,
            V1COL_LOGIN_PHONE2,
            V1COL_PERHAPS_NAME,
            V1COL_LOGIN_PHONE3,
            V1COL_DISPLAY_NAME,
            V1COL_DISPLAY_NAME,
            V1COL_IMAGE_URL,
            V1COL_REMARK,
            V1COL_IN_CIRCLES,
            V1COL_HIS_FRIEND,
            V1COL_BIDI,
    };


    public static final String[] V1_FULL_COLUMNS = {
            V1COL_USER_ID,
            V1COL_LOGIN_EMAIL1,
            V1COL_LOGIN_EMAIL2,
            V1COL_LOGIN_EMAIL3,
            V1COL_LOGIN_PHONE1,
            V1COL_LOGIN_PHONE2,
            V1COL_LOGIN_PHONE3,
            V1COL_DOMAIN_NAME,
            V1COL_PERHAPS_NAME,
            V1COL_DISPLAY_NAME,
            V1COL_FIRST_NAME,
            V1COL_MIDDLE_NAME,
            V1COL_LAST_NAME,
            V1COL_CREATED_TIME,
            V1COL_LAST_VISITED_TIME,
            V1COL_IMAGE_URL,
            V1COL_SMALL_IMAGE_URL,
            V1COL_LARGE_IMAGE_URL,
            V1COL_BASIC_UPDATED_TIME,
            V1COL_STATUS,
            V1COL_STATUS_UPDATED_TIME,
            V1COL_GENDER,
            V1COL_BIRTHDAY,
            V1COL_DEPARTMENT,
            V1COL_JOB_TITLE,
            V1COL_OFFICE_ADDRESS,
            V1COL_PROFESSION,
            V1COL_JOB_DESCRIPTION,
            V1COL_CONTACT_INFO,
            V1COL_FAMILY,
            V1COL_ADDRESS,
            V1COL_WORK_HISTORY_UPDATED_TIME,
            V1COL_MISCELLANEOUS,
            V1COL_IN_CIRCLES,
            V1COL_HIS_FRIEND,
            V1COL_BIDI,
            V1COL_FRIENDS_COUNT,
            V1COL_FOLLOWERS_COUNT,
            V1COL_FAVORITES_COUNT,
            V1COL_WORK_HISTORY,
            V1COL_EDUCATION_HISTORY,
    };


    public static String[] expandV1Columns(String[] v1Cols) {
        return ColumnsExpander.expand(v1Cols, "#light", V1_LIGHT_COLUMNS, "#full", V1_FULL_COLUMNS);
    }

    public static String[] v1ToV2Columns(String[] v1Cols) {
        LinkedHashSet<String> l = new LinkedHashSet<String>();

        // basic
        if (ArrayUtils.contains(v1Cols, V1COL_USER_ID))
            l.add(User.COL_USER_ID);
        if (ArrayUtils.contains(v1Cols, V1COL_PASSWORD))
            l.add(User.COL_PASSWORD);
        if (ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL1)
                || ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL2)
                || ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL3))
            l.add(User.COL_EMAIL);

        if (ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE1)
                || ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE2)
                || ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE3))
            l.add(User.COL_TEL);
        if (ArrayUtils.contains(v1Cols, V1COL_DOMAIN_NAME))
            l.add(User.COL_USER_ID); // dummy
        if (ArrayUtils.contains(v1Cols, V1COL_PERHAPS_NAME))
            l.add(User.COL_PERHAPS_NAME);
        if (ArrayUtils.contains(v1Cols, V1COL_DISPLAY_NAME))
            l.add(User.COL_DISPLAY_NAME);
        if (ArrayUtils.contains(v1Cols, V1COL_FIRST_NAME)
                || ArrayUtils.contains(v1Cols, V1COL_MIDDLE_NAME)
                || ArrayUtils.contains(v1Cols, V1COL_LAST_NAME))
            l.add(User.COL_NAME);
        if (ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            l.add(User.COL_CREATED_TIME);
        if (ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            l.add(User.COL_DESTROYED_TIME);
        //if (ArrayUtils.contains(v1Cols, V1COL_LAST_VISITED_TIME))
        //    ;
        if (ArrayUtils.contains(v1Cols, V1COL_IMAGE_URL)
                || ArrayUtils.contains(v1Cols, V1COL_SMALL_IMAGE_URL)
                || ArrayUtils.contains(v1Cols, V1COL_LARGE_IMAGE_URL))
            l.add(User.COL_PHOTO);
        if (ArrayUtils.contains(v1Cols, V1COL_BASIC_UPDATED_TIME))
            Collections.addAll(l, User.COL_TEL, User.COL_EMAIL, User.COL_NAME, User.COL_CREATED_TIME, User.COL_PHOTO);

        // status
        if (ArrayUtils.contains(v1Cols, V1COL_STATUS))
            l.add(StatusUserExpansion.COL_STATUS);
        if (ArrayUtils.contains(v1Cols, V1COL_STATUS_UPDATED_TIME))
            l.add(StatusUserExpansion.COL_STATUS_UPDATED_TIME);

        // profile
        if (ArrayUtils.contains(v1Cols, V1COL_GENDER))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_BIRTHDAY))
            l.add(User.COL_DATE);
        if (ArrayUtils.contains(v1Cols, V1COL_TIMEZONE))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_INTERESTS))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_LANGUAGES))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_MARRIAGE))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_RELIGION))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_ABOUT_ME))
            l.add(User.COL_PROFILE);
        if (ArrayUtils.contains(v1Cols, V1COL_PROFILE_UPDATED_TIME))
            Collections.addAll(l, User.COL_DATE, User.COL_PROFILE);

        // business
        if (ArrayUtils.contains(v1Cols, V1COL_COMPANY))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_DEPARTMENT))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_JOB_TITLE))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_OFFICE_ADDRESS))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_PROFESSION))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_JOB_DESCRIPTION))
            l.add(User.COL_ORGANIZATION);
        if (ArrayUtils.contains(v1Cols, V1COL_BUSINESS_UPDATED_TIME))
            l.add(User.COL_ORGANIZATION);

        // contact info
        if (ArrayUtils.contains(v1Cols, V1COL_CONTACT_INFO) || ArrayUtils.contains(v1Cols, V1COL_CONTACT_INFO_UPDATED_TIME))
            Collections.addAll(l, User.COL_TEL, User.COL_EMAIL, User.COL_IM);

        // address
        if (ArrayUtils.contains(v1Cols, V1COL_ADDRESS) || ArrayUtils.contains(v1Cols, V1COL_ADDRESS_UPDATED_TIME))
            l.add(User.COL_ADDRESS);

        // work history
        if (ArrayUtils.contains(v1Cols, V1COL_WORK_HISTORY) || ArrayUtils.contains(v1Cols, V1COL_WORK_HISTORY_UPDATED_TIME))
            l.add(User.COL_WORK_HISTORY);

        // education history
        if (ArrayUtils.contains(v1Cols, V1COL_EDUCATION_HISTORY) || ArrayUtils.contains(v1Cols, V1COL_EDUCATION_HISTORY_UPDATED_TIME))
            l.add(User.COL_EDUCATION_HISTORY);

        // family && coworker

        // misc
        if (ArrayUtils.contains(v1Cols, V1COL_MISCELLANEOUS))
            l.add(User.COL_MISCELLANEOUS);

        // friends
        if (ArrayUtils.contains(v1Cols, V1COL_REMARK))
            l.add(RelUserExpansion.COL_REMARK);
        if (ArrayUtils.contains(v1Cols, V1COL_IN_CIRCLES))
            l.add(RelUserExpansion.COL_IN_CIRCLES);
        if (ArrayUtils.contains(v1Cols, V1COL_HIS_FRIEND))
            l.add(RelUserExpansion.COL_HIS_FRIEND);
        if (ArrayUtils.contains(v1Cols, V1COL_BIDI))
            l.add(RelUserExpansion.COL_BIDI);
        if (ArrayUtils.contains(v1Cols, V1COL_FRIENDS_COUNT))
            l.add(RelUserExpansion.COL_FRIENDS_COUNT);
        if (ArrayUtils.contains(v1Cols, V1COL_FOLLOWERS_COUNT))
            l.add(RelUserExpansion.COL_FOLLOWERS_COUNT);

        // qiupu
        if (ArrayUtils.contains(v1Cols, V1COL_FAVORITES_COUNT))
            l.add(AbstractQiupuUserExpansion.COL_FAVORITE_APP_COUNT);

        // privacy
        // V1COL_PRIVACY_ENABLED
        l.add(PrivacyControlUserExpansion.COL_HE_ALLOWED);

        // request
        // V1COL_PENDING_REQUESTS
        l.add(RequestUserExpansion.COL_PENDING_REQUESTS);


        return l.toArray(new String[l.size()]);
    }

    public static String userToJson(final User user, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeUser(jg, user, v1Cols);
            }
        }, human);
    }

    public static String usersToJson(final Users users, final String[] v1Cols, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeUsers(jg, users, v1Cols);
            }
        }, human);
    }


    public static void serializeUser(JsonGenerator jg, User user, String[] v1Cols) throws IOException {
        jg.writeStartObject();

        // basic
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_USER_ID))
            jg.writeNumberField(V1COL_USER_ID, user.getUserId());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PASSWORD))
            jg.writeStringField(V1COL_PASSWORD, ObjectUtils.toString(user.getPassword()));

        Record loginEmailsAndPhones = CompatibleContactInfo.getLoginEmailsAndPhones(user.getTel(), user.getEmail(), user.getIm(), user.getSipAddress());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL1))
            jg.writeStringField(V1COL_LOGIN_EMAIL1, loginEmailsAndPhones.getString(V1COL_LOGIN_EMAIL1, ""));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL2))
            jg.writeStringField(V1COL_LOGIN_EMAIL2, loginEmailsAndPhones.getString(V1COL_LOGIN_EMAIL2, ""));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_EMAIL3))
            jg.writeStringField(V1COL_LOGIN_EMAIL3, loginEmailsAndPhones.getString(V1COL_LOGIN_EMAIL3, ""));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE1))
            jg.writeStringField(V1COL_LOGIN_PHONE1, loginEmailsAndPhones.getString(V1COL_LOGIN_PHONE1, ""));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE2))
            jg.writeStringField(V1COL_LOGIN_PHONE2, loginEmailsAndPhones.getString(V1COL_LOGIN_PHONE2, ""));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LOGIN_PHONE3))
            jg.writeStringField(V1COL_LOGIN_PHONE3, loginEmailsAndPhones.getString(V1COL_LOGIN_PHONE3, ""));

        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DOMAIN_NAME))
            jg.writeStringField(V1COL_DOMAIN_NAME, Long.toString(user.getUserId()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DISPLAY_NAME))
            jg.writeStringField(V1COL_DISPLAY_NAME, ObjectUtils.toString(user.getDisplayName()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PERHAPS_NAME))
            jg.writeStringField(V1COL_PERHAPS_NAME, ObjectUtils.toString(user.getPerhapsname()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FIRST_NAME))
            jg.writeStringField(V1COL_FIRST_NAME, user.getName() != null ? ObjectUtils.toString(user.getName().getFirst()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MIDDLE_NAME))
            jg.writeStringField(V1COL_MIDDLE_NAME, user.getName() != null ? ObjectUtils.toString(user.getName().getMiddle()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LAST_NAME))
            jg.writeStringField(V1COL_LAST_NAME, user.getName() != null ? ObjectUtils.toString(user.getName().getLast()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CREATED_TIME))
            jg.writeNumberField(V1COL_CREATED_TIME, user.getCreatedTime());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DESTROYED_TIME))
            jg.writeNumberField(V1COL_DESTROYED_TIME, user.getDestroyedTime());
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LAST_VISITED_TIME))
            jg.writeNumberField(V1COL_LAST_VISITED_TIME, 0L);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_IMAGE_URL))
            jg.writeStringField(V1COL_IMAGE_URL, user.getPhoto() != null ? ObjectUtils.toString(user.getPhoto().getMiddleUrl()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_SMALL_IMAGE_URL))
            jg.writeStringField(V1COL_SMALL_IMAGE_URL, user.getPhoto() != null ? ObjectUtils.toString(user.getPhoto().getSmallUrl()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LARGE_IMAGE_URL))
            jg.writeStringField(V1COL_LARGE_IMAGE_URL, user.getPhoto() != null ? ObjectUtils.toString(user.getPhoto().getLargeUrl()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_BASIC_UPDATED_TIME))
            jg.writeNumberField(V1COL_BASIC_UPDATED_TIME, user.getMaxPropertyUpdatedTime(User.COL_NAME, User.COL_PHOTO));

        // status
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_STATUS))
            user.writeAddonJsonAs(jg, StatusUserExpansion.COL_STATUS, V1COL_STATUS);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_STATUS_UPDATED_TIME))
            user.writeAddonJsonAs(jg, StatusUserExpansion.COL_STATUS_UPDATED_TIME, V1COL_STATUS_UPDATED_TIME);

        // profile
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_GENDER))
            jg.writeStringField(V1COL_GENDER, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getGender()) : "u");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_BIRTHDAY))
            jg.writeStringField(V1COL_BIRTHDAY, ObjectUtils.toString(user.getBirthday()));
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_TIMEZONE))
            jg.writeStringField(V1COL_TIMEZONE, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getTimezone()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_INTERESTS))
            jg.writeStringField(V1COL_INTERESTS, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getInterests()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_LANGUAGES))
            jg.writeStringField(V1COL_LANGUAGES, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getLanguages()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MARRIAGE))
            jg.writeStringField(V1COL_MARRIAGE, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getMarriage()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_RELIGION))
            jg.writeStringField(V1COL_RELIGION, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getReligion()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ABOUT_ME))
            jg.writeStringField(V1COL_ABOUT_ME, user.getProfile() != null ? ObjectUtils.toString(user.getProfile().getDescription()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PROFILE_UPDATED_TIME))
            jg.writeNumberField(V1COL_PROFILE_UPDATED_TIME, user.getPropertyUpdatedTime(User.COL_PROFILE));

        // business
        OrgInfo org = CollectionUtils.isNotEmpty(user.getOrganization()) ? user.getOrganization().get(0) : null;
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COMPANY))
            jg.writeStringField(V1COL_COMPANY, org != null ? ObjectUtils.toString(org.getCompany()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_DEPARTMENT))
            jg.writeStringField(V1COL_DEPARTMENT, org != null ? ObjectUtils.toString(org.getDepartment()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_JOB_TITLE))
            jg.writeStringField(V1COL_JOB_TITLE, org != null ? ObjectUtils.toString(org.getTitle()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_OFFICE_ADDRESS))
            jg.writeStringField(V1COL_OFFICE_ADDRESS, org != null ? ObjectUtils.toString(org.getOfficeLocation()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_PROFESSION))
            jg.writeStringField(V1COL_PROFESSION, "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_JOB_DESCRIPTION))
            jg.writeStringField(V1COL_JOB_DESCRIPTION, org != null ? ObjectUtils.toString(org.getJobDescription()) : "");
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_BUSINESS_UPDATED_TIME))
            jg.writeNumberField(V1COL_BUSINESS_UPDATED_TIME, user.getPropertyUpdatedTime(User.COL_ORGANIZATION));

        // contact info
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CONTACT_INFO)) {
            jg.writeFieldName(V1COL_CONTACT_INFO);
            CompatibleContactInfo.serializeContactInfo(jg, user.getTel(), user.getEmail(), user.getIm(), user.getSipAddress());
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_CONTACT_INFO_UPDATED_TIME))
            jg.writeNumberField(V1COL_CONTACT_INFO_UPDATED_TIME, user.getMaxPropertyUpdatedTime(User.COL_TEL, User.COL_EMAIL));

        // address
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADDRESS)) {
            jg.writeFieldName(V1COL_ADDRESS);
            CompatibleAddressInfo.serializeAddressInfo(jg, user.getAddress());
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_ADDRESS_UPDATED_TIME))
            jg.writeNumberField(V1COL_ADDRESS_UPDATED_TIME, user.getPropertyUpdatedTime(User.COL_ADDRESS));

        // work history
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_WORK_HISTORY)) {
            jg.writeFieldName(V1COL_WORK_HISTORY);
            CompatibleWorkHistory.serializeWorkHistories(jg, user.getWorkHistory());
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_WORK_HISTORY_UPDATED_TIME))
            jg.writeNumberField(V1COL_WORK_HISTORY_UPDATED_TIME, user.getPropertyUpdatedTime(User.COL_WORK_HISTORY));

        // education history
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_EDUCATION_HISTORY)) {
            jg.writeFieldName(V1COL_EDUCATION_HISTORY);
            CompatibleEduHistory.serializeEduHistories(jg, user.getEducationHistory());
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_EDUCATION_HISTORY_UPDATED_TIME))
            jg.writeNumberField(V1COL_EDUCATION_HISTORY_UPDATED_TIME, user.getPropertyUpdatedTime(User.COL_EDUCATION_HISTORY));

        // other
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FAMILY)) {
            jg.writeFieldName(V1COL_FAMILY);
            jg.writeRawValue("[]");
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_COWORKER)) {
            jg.writeFieldName(V1COL_COWORKER);
            jg.writeRawValue("[]");
        }
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_MISCELLANEOUS)) {
            jg.writeFieldName(V1COL_MISCELLANEOUS);
            MiscInfo misc = user.getMiscellaneous();
            if (misc != null)
                CompatibleMiscInfo.serializeMisc(jg, user.getMiscellaneous());
            else
                jg.writeRawValue("{}");
        }

        // friend
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_REMARK))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_REMARK, V1COL_REMARK);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_IN_CIRCLES))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_IN_CIRCLES, V1COL_IN_CIRCLES, CIRCLE_TRANSFORMER);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_HIS_FRIEND))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_HIS_FRIEND, V1COL_HIS_FRIEND);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_BIDI))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_BIDI, V1COL_BIDI);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FRIENDS_COUNT))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_FRIENDS_COUNT, V1COL_FRIENDS_COUNT);
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FOLLOWERS_COUNT))
            user.writeAddonJsonAs(jg, RelUserExpansion.COL_FOLLOWERS_COUNT, V1COL_FOLLOWERS_COUNT);

        // qiupu
        if (v1Cols == null || ArrayUtils.contains(v1Cols, V1COL_FAVORITES_COUNT))
            user.writeAddonJsonAs(jg, AbstractQiupuUserExpansion.COL_FAVORITE_APP_COUNT, V1COL_FAVORITES_COUNT);

        // privacy
        // always add profile_privacy
        String heAllowedStr = user.getAddonAsString(PrivacyControlUserExpansion.COL_HE_ALLOWED, "");
        jg.writeBooleanField(V1COL_PROFILE_PRIVACY, !StringHelper.splitSet(heAllowedStr, ",", true).contains(PrivacyResources.RES_VCARD));


        // pending_request
        // always add pedding_requests
        jg.writeFieldName(V1COL_PENDING_REQUESTS);
        jg.writeRawValue("[]"); // TODO: fill pending_request

        if (v1Cols == null || ArrayUtils.contains(v1Cols, CompatiblePeopleSuggest.V1COL_SUGGEST_TYPE)) {
            if (user.hasAddon(CompatiblePeopleSuggest.V1COL_SUGGEST_TYPE))
                user.writeAddonJson(jg, CompatiblePeopleSuggest.V1COL_SUGGEST_TYPE);
        }

        if (v1Cols == null || ArrayUtils.contains(v1Cols, CompatiblePeopleSuggest.V1COL_SUGGEST_REASON)) {
            if (user.hasAddon(CompatiblePeopleSuggest.V1COL_SUGGEST_REASON))
                user.writeAddonJson(jg, CompatiblePeopleSuggest.V1COL_SUGGEST_REASON);
        }

        jg.writeEndObject();
    }

    private static CompatibleCircle.CircleJsonTransformer CIRCLE_TRANSFORMER =
            new CompatibleCircle.CircleJsonTransformer(CompatibleCircle.CIRCLE_COLUMNS);


    public static void serializeUsers(JsonGenerator jg, Users users, final String[] v1Cols) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(users)) {
            for (User user : users) {
                if (user != null)
                    serializeUser(jg, user, v1Cols);
            }
        }
        jg.writeEndArray();
    }

    public static String v2JsonNodeToV1Json(JsonNode jn, String[] v1Cols, boolean human) {
        if (jn.isArray()) {
            final Users users = Users.fromJsonNode(null, jn);
            return usersToJson(users, v1Cols, human);
        } else if (jn.isObject()) {
            final User user = User.fromJsonNode(jn);
            return userToJson(user, v1Cols, human);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String v2ToV1Json(String json, String[] v1Cols, boolean human) {
        JsonNode jn = JsonHelper.parse(json);
        return v2JsonNodeToV1Json(jn, v1Cols, human);
    }

    public static class UserJsonTransformer implements Addons.AddonValueTransformer {

        public final String[] userV1Columns;

        public UserJsonTransformer(String[] userV1Columns) {
            this.userV1Columns = userV1Columns;
        }

        @Override
        public Object transform(Object old) {
            String json = ObjectUtils.toString(old);
            return Addons.jsonAddonValue(CompatibleUser.v2ToV1Json(json, userV1Columns, true));
        }
    }
}
