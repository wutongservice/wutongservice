package com.borqs.server.wutong.account2;

import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.RecordSet;
import com.borqs.server.base.log.Logger;
import com.borqs.server.base.util.json.JsonUtils;
import com.borqs.server.wutong.account2.user.*;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import java.util.*;

public class AccountConverter {
    private static final Logger L = Logger.getLogger(AccountConverter.class);
    public static final String HTTP_APITEST_BORQS_COM_PROFILE_IMAGE = "http://apitest.borqs.com/profile_image";
    public static final String HTTP_APITEST_BORQS_COM_SYS_ICON = "http://apitest.borqs.com/sys/icon";


    private static void splitPath(Record record) {
        if (record == null)
            return;

        String image_url = "";
        String small_image_url = "";
        String large_image_url = "";

        if (record.get("image_url") != null && record.get("image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            image_url = changePath((String) record.get("image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.set("image_url", image_url);
        }
        if (record.get("image_url") != null && record.get("image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.set("image_url", "");
        }
        if (record.get("small_image_url") != null && record.get("small_image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            small_image_url = changePath((String) record.get("small_image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.set("small_image_url", small_image_url);
        }
        if (record.get("small_image_url") != null && record.get("small_image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.set("small_image_url", "");
        }

        if (record.get("large_image_url") != null && record.get("large_image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            large_image_url = changePath((String) record.get("large_image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.set("large_image_url", large_image_url);
        }
        if (record.get("large_image_url") != null && record.get("large_image_url").toString().startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.set("large_image_url", "");
        }
    }

    private static String changePath(String path, String modelStr) {
        if (path.indexOf(modelStr) == -1)
            return path;
        return path.substring(modelStr.length() + 1, path.length());
    }


    public static List<User> converterRecordSet2UserList(RecordSet recordSet) {
        if (recordSet == null || recordSet.size() < 1)
            return null;
        List<User> userList = new ArrayList<User>();
        for (Record record : recordSet) {
            userList.add(converterRecord2User(record));
        }
        return userList;
    }

    public static User converterRecord2User(Record record) {
        User user = new User();
        //take out the url path like "Http://"
        splitPath(record);

        if (StringUtils.isNotEmpty(record.getString("user_id")))
            user.setUserId(record.getInt("user_id"));
        if (StringUtils.isNotEmpty(record.getString("password")))
            user.setPassword(record.getString("password"));
        if (StringUtils.isNotEmpty(record.getString("created_time")))
            user.setCreatedTime(record.getInt("created_time"));
        if (StringUtils.isNotEmpty(record.getString("destroy_time")))
            user.setDestroyedTime(record.getInt("destroy_time"));
        if (StringUtils.isNotEmpty(record.getString("nick_name")))
            user.setNickname(record.getString("nick_name"));

        if (StringUtils.isNotEmpty(record.getString("top_posts")))
            user.setTopPosts(record.getString("top_posts"));
        if (StringUtils.isNotEmpty(record.getString("top_name")))
            user.setTopName(record.getString("top_name"));
        if (StringUtils.isNotEmpty(record.getString("language")))
            user.setLanguage(record.getString("language"));

        if (StringUtils.isNotEmpty(record.getString("status")))
            user.setStatus(record.getString("status"));
        if (StringUtils.isNotEmpty(record.getString("perhaps_name")))
            user.setAddon("perhaps_name", record.getString("perhaps_name"));
        if (StringUtils.isNotEmpty(record.getString("sort_key")))
            user.setAddon("sort_key", record.getString("sort_key"));


        if (record.has("login_email1"))
            user.setLoginEmail1(record.getString("login_email1"));
        if (record.has("login_email2"))
            user.setLoginEmail2(record.getString("login_email2"));
        if (record.has("login_email3"))
            user.setLoginEmail3(record.getString("login_email3"));
        if (record.has("login_phone1"))
            user.setLoginPhone1(record.getString("login_phone1"));
        if (record.has("login_phone2"))
            user.setLoginPhone2(record.getString("login_phone2"));
        if (record.has("login_phone3"))
            user.setLoginPhone3(record.getString("login_phone3"));

        String firstName = record.getString("first_name");
        String lastName = record.getString("last_name");
        String middleName = record.getString("middle_name");

        if (StringUtils.isNotEmpty(firstName) || StringUtils.isNotEmpty(middleName)
                || StringUtils.isNotEmpty(lastName)) {
            NameInfo nameInfo = new NameInfo();
            nameInfo.setFirst(firstName);
            nameInfo.setMiddle(middleName);
            nameInfo.setLast(lastName);
            user.setName(nameInfo);
        }


        if (StringUtils.isNotEmpty(record.getString("display_name"))){
            user.setName(NameInfo.split(record.getString("display_name")));
            user.setAddon("display_name",record.getString("display_name"));
        }


        PhotoInfo photoInfo = new PhotoInfo();
        boolean isPhoto = false;
        if (StringUtils.isNotEmpty(record.getString("small_image_url"))) {
            photoInfo.setSmallUrl(record.getString("small_image_url"));
            isPhoto = true;
        }
        if (StringUtils.isNotEmpty(record.getString("image_url"))) {
            photoInfo.setMiddleUrl(record.getString("image_url"));
            isPhoto = true;
        }
        if (StringUtils.isNotEmpty(record.getString("large_image_url"))) {
            photoInfo.setLargeUrl(record.getString("large_image_url"));
            isPhoto = true;
        }
        if (isPhoto)
            user.setPhoto(photoInfo);

        ProfileInfo profileInfo = new ProfileInfo();
        boolean isprofile = false;
        if (StringUtils.isNotEmpty(record.getString("gender"))) {
            profileInfo.setGender(record.getString("gender"));
            isprofile = true;
        }
        if (StringUtils.isNotEmpty(record.getString("interests"))) {
            profileInfo.setInterests(record.getString("interests"));
            isprofile = true;
        }
        if (StringUtils.isNotEmpty(record.getString("languages"))) {
            profileInfo.setLanguages(record.getString("languages"));
            isprofile = true;
        }
        if (StringUtils.isNotEmpty(record.getString("marriage"))) {
            profileInfo.setMarriage(record.getString("marriage"));
            isprofile = true;
        }
        if (StringUtils.isNotEmpty(record.getString("religion"))) {
            profileInfo.setReligion((record.getString("religion")));
            isprofile = true;
        }
        if (StringUtils.isNotEmpty(record.getString("timezone"))) {
            profileInfo.setTimezone((record.getString("timezone")));
            isprofile = true;
        }
        if (isprofile)
            user.setProfile(profileInfo);

        List<OrgInfo> orgInfoList = new ArrayList<OrgInfo>();
        OrgInfo orgInfo = new OrgInfo();
        boolean isOrg = false;
        if (StringUtils.isNotEmpty(record.getString("company"))) {
            orgInfo.setCompany(record.getString("company"));
            isOrg = true;
        }
        if (StringUtils.isNotEmpty(record.getString("department"))) {
            orgInfo.setDepartment((record.getString("department")));
            isOrg = true;
        }
        if (StringUtils.isNotEmpty(record.getString("job_description"))) {
            orgInfo.setJobDescription((record.getString("job_description")));
            isOrg = true;
        }
        if (StringUtils.isNotEmpty(record.getString("office_address"))) {
            orgInfo.setOfficeLocation((record.getString("office_address")));
            isOrg = true;
        }
        if (StringUtils.isNotEmpty(record.getString("job_title"))) {
            orgInfo.setTitle((record.getString("job_title")));
            isOrg = true;
        }
        if (isOrg) {
            orgInfoList.add(orgInfo);
            user.setOrganization(orgInfoList);
        }


        //deal with old contact_info json Object
        if (record.has("contact_info")) {
            String contact_info = record.getString("contact_info");
            if (contact_info != null || contact_info.length() > 1) {
                JsonNode jn = JsonUtils.parse(contact_info);
                Iterator<String> fieldNamesIter = jn.getFieldNames();

                List<EmailInfo> emailInfoList = new ArrayList<EmailInfo>();
                List<TelInfo> telInfoList = new ArrayList<TelInfo>();
                boolean isEmail = false;
                boolean isTel = false;
                while (fieldNamesIter.hasNext()) {

                    String fieldName = fieldNamesIter.next();
                    if (fieldName.contains("email")) {
                        EmailInfo emailInfo = new EmailInfo();
                        //notice! the default email type is home!
                        emailInfo.setType(EmailInfo.TYPE_HOME);
                        emailInfo.setInfo(jn.get(fieldName).getTextValue());
                        emailInfoList.add(emailInfo);
                        isEmail = true;
                    } else if (fieldName.contains("mobile")) {
                        TelInfo telInfo = new TelInfo();
                        //notice! the default phone type is home!
                        telInfo.setType(TelInfo.TYPE_HOME);
                        telInfo.setInfo(jn.get(fieldName).getTextValue());
                        telInfoList.add(telInfo);
                        isTel = true;
                    }

                }
                if (isTel){
                    user.setTel(telInfoList);
                    if (!isEmail) {
                        List<EmailInfo> list = new ArrayList<EmailInfo>();
                        EmailInfo ei = new EmailInfo();
                        list.add(ei);
                        user.setEmail(list);
                    }
                }
                if (isEmail){
                    if(!isTel){
                        List<TelInfo> list = new ArrayList<TelInfo>();
                        TelInfo ti = new TelInfo();
                        list.add(ti);
                        user.setTel(list);
                    }
                    user.setEmail(emailInfoList);
                }
            }
        }

        //deal with old version address json Object
        List<AddressInfo> addressInfoList = new ArrayList<AddressInfo>();
        if (record.has("address")) {
            String address = record.getString("address");
            if (address != null || address.length() > 1) {
                JsonNode jsonNode = JsonUtils.parse(address);
                if (jsonNode.isArray()) {
                    for (Iterator<JsonNode> jns = jsonNode.iterator(); jns.hasNext(); ) {
                        JsonNode jn = jns.next();
                        AddressInfo addressInfo = new AddressInfo();
                        if (jn.get("city") != null && StringUtils.isNotEmpty(jn.get("city").getTextValue()))
                            addressInfo.setCity(jn.get("city").getTextValue());
                        if (jn.get("country") != null && StringUtils.isNotEmpty(jn.get("country").getTextValue()))
                            addressInfo.setCountry(jn.get("country").getTextValue());
                        if (jn.get("po_box") != null && StringUtils.isNotEmpty(jn.get("po_box").getTextValue()))
                            addressInfo.setPoBox(jn.get("po_box").getTextValue());
                        if (jn.get("postal_code") != null && StringUtils.isNotEmpty(jn.get("postal_code").getTextValue()))
                            addressInfo.setZipCode(jn.get("postal_code").getTextValue());
                        if (jn.get("street") != null && StringUtils.isNotEmpty(jn.get("street").getTextValue()))
                            addressInfo.setStreet(jn.get("street").getTextValue());
                        if (jn.get("type") != null && StringUtils.isNotEmpty(jn.get("type").getTextValue()))
                            addressInfo.setType(jn.get("type").getTextValue());
                        if (jn.get("state") != null && StringUtils.isNotEmpty(jn.get("state").getTextValue()))
                            addressInfo.setProvince(jn.get("state").getTextValue());
                        if (addressInfo.toString().length() > 0)
                            addressInfoList.add(addressInfo);
                    }
                } else {
                    throw new RuntimeException("--------------------Analyse Address error!_______________");
                }
                if (addressInfoList.size() > 0)
                    user.setAddress(addressInfoList);
            }
        }

        DateInfo dateInfo = new DateInfo();
        dateInfo.setType(DateInfo.TYPE_BIRTHDAY);
        dateInfo.setInfo(record.getString("birthday"));
        if (StringUtils.isNotEmpty(record.getString("birthday")))
            user.setDate(dateInfo);


        //---deal with work_history json Object
        if (record.has("work_history")) {
            List<WorkHistory> workHistoryList = new ArrayList<WorkHistory>();
            String wrokHistory = record.getString("work_history");
            if (wrokHistory != null || wrokHistory.length() > 1) {
                JsonNode jsonNode = JsonUtils.parse(wrokHistory);
                if (jsonNode.isArray()) {
                    for (Iterator<JsonNode> jns = jsonNode.iterator(); jns.hasNext(); ) {
                        JsonNode jn = jns.next();
                        WorkHistory workHistory = new WorkHistory();
                        if (jn.get("company") != null && StringUtils.isNotEmpty(jn.get("company").getTextValue()))
                            workHistory.setCompany(jn.get("company").getTextValue());
                        if (jn.get("from") != null && StringUtils.isNotEmpty(jn.get("from").getTextValue()))
                            workHistory.setFrom(jn.get("from").getTextValue());
                        if (jn.get("title") != null && StringUtils.isNotEmpty(jn.get("title").getTextValue()))
                            workHistory.setTitle(jn.get("title").getTextValue());
                        if (jn.get("to") != null && StringUtils.isNotEmpty(jn.get("to").getTextValue()))
                            workHistory.setTo(jn.get("to").getTextValue());
                        if (jn.get("description") != null && StringUtils.isNotEmpty(jn.get("description").getTextValue()))
                            workHistory.setJobDescription(jn.get("description").getTextValue());
                        if (jn.get("address") != null && StringUtils.isNotEmpty(jn.get("address").getTextValue()))
                            workHistory.setOfficeLocation(jn.get("address").getTextValue());
                        if (jn.get("profession") != null && StringUtils.isNotEmpty(jn.get("profession").getTextValue()))
                            workHistory.setDepartment(jn.get("profession").getTextValue());
                        if (workHistory.toString().length() > 0)
                            workHistoryList.add(workHistory);
                    }
                }
            }
            if (workHistoryList.size() > 0)
                user.setWorkHistory(workHistoryList);
        }

        if (record.has("education_history")) {
            List<EduHistory> eduHistoryList = new ArrayList<EduHistory>();
            String eduHistoryStr = record.getString("education_history");
            if (eduHistoryStr != null || eduHistoryStr.length() > 1) {
                JsonNode jsonNode = JsonUtils.parse(eduHistoryStr);
                if (jsonNode.isArray()) {
                    for (Iterator<JsonNode> jns = jsonNode.iterator(); jns.hasNext(); ) {
                        JsonNode jn = jns.next();
                        EduHistory eduHistory = new EduHistory();
                        if (jn.get("degree") != null && StringUtils.isNotEmpty(jn.get("degree").getTextValue()))
                            eduHistory.setDegree(jn.get("degree").getTextValue());
                        if (jn.get("from") != null && StringUtils.isNotEmpty(jn.get("from").getTextValue()))
                            eduHistory.setFrom(jn.get("from").getTextValue());
                        if (jn.get("class") != null && StringUtils.isNotEmpty(jn.get("class").getTextValue()))
                            eduHistory.setKlass(jn.get("class").getTextValue());
                        if (jn.get("to") != null && StringUtils.isNotEmpty(jn.get("to").getTextValue()))
                            eduHistory.setTo(jn.get("to").getTextValue());
                        if (jn.get("major") != null && StringUtils.isNotEmpty(jn.get("major").getTextValue()))
                            eduHistory.setMajor(jn.get("major").getTextValue());
                        if (jn.get("school") != null && StringUtils.isNotEmpty(jn.get("school").getTextValue()))
                            eduHistory.setSchool(jn.get("school").getTextValue());
                        if (jn.get("type") != null && StringUtils.isNotEmpty(jn.get("type").getTextValue())) {
                            //XX
                        }
                        //eduHistory.setType(jn.get("type").getTextValue());
                        //eduHistory.setType();
                        if (eduHistory.toString().length() > 0)
                            eduHistoryList.add(eduHistory);
                    }
                }
            }
            if (eduHistoryList.size() > 0)
                user.setEducationHistory(eduHistoryList);
        }

        if (record.has("miscellaneous")) {
            String miscell = record.getString("miscellaneous");
            if (miscell != null || miscell.length() > 1) {
                JsonNode jsonNode = JsonUtils.parse(miscell);

                MiscInfo miscInfo = new MiscInfo();
                if (jsonNode.get(MiscInfo.COL_OPENFACE_PHONE) != null && StringUtils.isNotEmpty(jsonNode.get(MiscInfo.COL_OPENFACE_PHONE).getTextValue()))
                    miscInfo.setOpenfacePhone(jsonNode.get(MiscInfo.COL_OPENFACE_PHONE).getTextValue());

                user.setMiscellaneous(miscInfo);
            }
        }

        return user;
    }

    /**
     * @param users
     * @param cols
     * @return
     */

    public static RecordSet convertUserList2RecordSet(List<User> users, String... cols) {
        if (users == null || users.size() < 1)
            return null;
        RecordSet recordSet = new RecordSet();
        for (User user : users) {
            recordSet.add(converUser2Record(user, cols));
        }
        return recordSet;
    }

    private static boolean checkCols(String colName, Map mapContain, boolean isCheck) {
        if (StringUtils.isBlank(colName) || mapContain == null)
            return false;
        if (isCheck) {
            if (mapContain.containsKey(colName))
                return true;
            else
                return false;
        } else {
            return true;
        }
    }

    public static Record converUser2Record(User user, String... cols) {
        if (user == null)
            return null;

        //init the converter column
        Map<String, String> mapContain = new HashMap<String, String>();
        boolean isCheck = false;
        if (cols != null && cols.length > 1) {
            for (String str : cols) {
                mapContain.put(str, str);
            }
            isCheck = true;
        }

        Record record = new Record();
        record.set("user_id", user.getUserId());
        record.set("password", user.getPassword());
        record.set("created_time", user.getCreatedTime());
        if (checkCols("nick_name", mapContain, isCheck)) {
            if (user.hasProperty("nick_name"))
                record.set("nick_name", user.getNickname());
            else
                record.set("nick_name", "");
        } else {
            record.set("nick_name", "");
        }

        if (checkCols("top_posts", mapContain, isCheck)) {
            if (user.hasProperty("top_posts"))
                record.set("top_posts", user.getTopPosts());
            else
                record.set("top_posts", "");
        } else {
            record.set("top_posts", "");
        }

        if (checkCols("top_name", mapContain, isCheck)) {
            if (user.hasProperty("top_name"))
                record.set("top_name", user.getTopName());
            else
                record.set("top_name", "");
        } else {
            record.set("top_name", "");
        }

        if (checkCols("language", mapContain, isCheck)) {
            if (user.hasProperty("language"))
                record.set("language", user.getLanguage());
            else
                record.set("language", "en");
        } else {
            record.set("language", "en");
        }

        record.set("destroyed_time", user.getDestroyedTime());
        record.set("last_visited_time", user.getCreatedTime());
        record.set("domain_name", "" + user.getUserId());
        record.set("status", "");
        if (StringUtils.isNotEmpty(user.getDisplayName()) && checkCols("display_name", mapContain, isCheck))
            record.set("display_name", user.getDisplayName());

        if (StringUtils.isNotBlank(user.getLoginEmail1()) && checkCols("login_email1", mapContain, isCheck))
            record.set("login_email1", user.getLoginEmail1());
        else
            record.set("login_email1", "");
        if (StringUtils.isNotBlank(user.getLoginEmail2()) && checkCols("login_email2", mapContain, isCheck))
            record.set("login_email2", user.getLoginEmail2());
        else
            record.set("login_email2", "");
        if (StringUtils.isNotBlank(user.getLoginEmail3()) && checkCols("login_email3", mapContain, isCheck))
            record.set("login_email3", user.getLoginEmail3());
        else
            record.set("login_email3", "");

        if (StringUtils.isNotBlank(user.getLoginPhone1()) && checkCols("login_phone1", mapContain, isCheck))
            record.set("login_phone1", user.getLoginPhone1());
        else
            record.set("login_phone1", "");
        if (StringUtils.isNotBlank(user.getLoginPhone2()) && checkCols("login_phone2", mapContain, isCheck))
            record.set("login_phone2", user.getLoginPhone2());
        else
            record.set("login_phone2", "");
        if (StringUtils.isNotBlank(user.getLoginPhone3()) && checkCols("login_phone3", mapContain, isCheck))
            record.set("login_phone3", user.getLoginPhone3());
        else
            record.set("login_phone3", "");

        if (StringUtils.isNotBlank(user.getStatus()) && checkCols("status", mapContain, isCheck))
            record.set("status", user.getStatus());
        else
            record.set("status", "");
        if (user.getStatusUpdatedTime() != 0 && checkCols("status_updated_time", mapContain, isCheck))
            record.set("status_updated_time", user.getStatusUpdatedTime());
        else
            record.set("status_updated_time", 0);

        if (StringUtils.isNotBlank((String) user.getAddon("sort_key", "")) && checkCols("sort_key", mapContain, isCheck))
            record.set("sort_key", user.getAddon("sort_key", ""));
        else
            record.set("sort_key", "");

        if (StringUtils.isNotBlank((String) user.getAddon("perhaps_name", "")) && !((String) user.getAddon("perhaps_name", "")).equals("[]") && checkCols("perhaps_name", mapContain, isCheck)) {
            JsonNode jn = JsonHelper.parse((String) user.getAddon("perhaps_name", ""));


            RecordSet rs = new RecordSet();
            if (jn.isArray()) {

                Record r = new Record();
                r.put("fullname", jn.findValue("fullname"));
                r.put("count", jn.findValue("count"));
                rs.add(r);
            }
            record.put("perhaps_name", rs);
        } else {
            RecordSet rs = new RecordSet();
            record.put("perhaps_name", rs);
        }

        //analyze the updatetime of "basic_updated_time"
        long basic_updated_time = 0;

        if (user.getPhoto() != null) {
            if (StringUtils.isNotBlank(user.getPhoto().getSmallUrl()) && checkCols("small_image_url", mapContain, isCheck))
                record.set("small_image_url", user.getPhoto().getSmallUrl());
            else
                record.set("small_image_url", "");
            if (StringUtils.isNotBlank(user.getPhoto().getMiddleUrl()) && checkCols("image_url", mapContain, isCheck))
                record.set("image_url", user.getPhoto().getMiddleUrl());
            else
                record.set("image_url", "");
            if (StringUtils.isNotBlank(user.getPhoto().getLargeUrl()) && checkCols("large_image_url", mapContain, isCheck))
                record.set("large_image_url", user.getPhoto().getLargeUrl());
            else
                record.set("large_image_url", "");

            basic_updated_time = user.getPropertyUpdatedTime(User.COL_PHOTO);
            record.set("basic_updated_time", basic_updated_time);
        } else {
            record.set("small_image_url", "");
            record.set("image_url", "");
            record.set("large_image_url", "");

        }
        if (user.getName() != null) {
            if (StringUtils.isNotBlank(user.getName().getFirst()) && checkCols("first_name", mapContain, isCheck))
                record.set("first_name", user.getName().getFirst());
            else
                record.set("first_name", "");
            if (StringUtils.isNotBlank(user.getName().getMiddle()) && checkCols("middle_name", mapContain, isCheck))
                record.set("middle_name", user.getName().getMiddle());
            else
                record.set("middle_name", "");
            if (StringUtils.isNotBlank(user.getName().getLast()) && checkCols("last_name", mapContain, isCheck))
                record.set("last_name", user.getName().getLast());
            else
                record.set("last_name", "");

            long name_update_time = user.getPropertyUpdatedTime(User.COL_NAME);
            if (name_update_time > basic_updated_time) {
                basic_updated_time = name_update_time;
                record.set("basic_updated_time", basic_updated_time);
            } else
                record.set("basic_updated_time", basic_updated_time);
        } else {
            record.set("first_name", "");
            record.set("middle_name", "");
            record.set("last_name", "");
        }

        long profile_updated_time = 0;
        if (user.getProfile() != null) {
            if (StringUtils.isNotBlank(user.getProfile().getGender()) && checkCols("gender", mapContain, isCheck))
                record.set("gender", user.getProfile().getGender());
            else
                record.set("gender", "");
            if (StringUtils.isNotBlank(user.getProfile().getTimezone()) && checkCols("timezone", mapContain, isCheck))
                record.set("timezone", user.getProfile().getTimezone());
            else
                record.set("timezone", "");


            if (StringUtils.isNotBlank(user.getProfile().getInterests()) && checkCols("interests", mapContain, isCheck))
                record.set("interests", user.getProfile().getInterests());
            else
                record.set("interests", "");
            if (StringUtils.isNotBlank(user.getProfile().getLanguages()) && checkCols("languages", mapContain, isCheck))
                record.set("languages", user.getProfile().getLanguages());
            else
                record.set("languages", "");
            if (StringUtils.isNotBlank(user.getProfile().getMarriage()) && checkCols("marriage", mapContain, isCheck))
                record.set("marriage", user.getProfile().getMarriage());
            else
                record.set("marriage", "");
            if (StringUtils.isNotBlank(user.getProfile().getReligion()) && checkCols("religion", mapContain, isCheck))
                record.set("religion", user.getProfile().getReligion());
            else
                record.set("religion", "");

            profile_updated_time = user.getPropertyUpdatedTime(User.COL_PROFILE);
            record.set("profile_updated_time", profile_updated_time);
        } else {
            record.set("gender", "");
            record.set("timezone", "");
            record.set("interests", "");
            record.set("languages", "");
            record.set("marriage", "");
            record.set("religion", "");
        }
        if (user.getDate() != null) {
            for (DateInfo dateInfo : user.getDate()) {
                if (StringUtils.isNotBlank(dateInfo.getType()) && dateInfo.getType().equals("birthday") && checkCols("birthday", mapContain, isCheck))
                    record.set("birthday", dateInfo.getInfo());
                else
                    record.set("birthday", "");
            }
        } else
            record.set("birthday", "");
        if (user.getOrganization() != null) {
            for (OrgInfo orgInfo : user.getOrganization()) {
                if (StringUtils.isNotBlank(orgInfo.getCompany()) && checkCols("company", mapContain, isCheck))
                    record.set("company", orgInfo.getCompany());
                else
                    record.set("company", "");
                if (StringUtils.isNotBlank(orgInfo.getDepartment()) && checkCols("department", mapContain, isCheck))
                    record.set("department", orgInfo.getDepartment());
                else
                    record.set("department", "");
                if (StringUtils.isNotBlank(orgInfo.getTitle()) && checkCols("job_title", mapContain, isCheck))
                    record.set("job_title", orgInfo.getTitle());
                else
                    record.set("job_title", "");
                if (StringUtils.isNotBlank(orgInfo.getOfficeLocation()) && checkCols("office_address", mapContain, isCheck))
                    record.set("office_address", orgInfo.getOfficeLocation());
                else
                    record.set("office_address", "");
                if (StringUtils.isNotBlank(orgInfo.getJobDescription()) && checkCols("job_description", mapContain, isCheck))
                    record.set("job_description", orgInfo.getJobDescription());
                else
                    record.set("job_description", "");
            }
        } else {

            record.set("company", "");
            record.set("department", "");
            record.set("job_title", "");
            record.set("office_address", "");
            record.set("job_description", "");
        }
        long contact_info_updated_time = 0;
        Record contactInfoRecord = new Record();
        // deal with the email
        int emailNum = 1;
        if (user.getEmail() != null)
            for (EmailInfo emailInfo : user.getEmail()) {
                if (StringUtils.isNotBlank(emailInfo.getType())) {
                    if (emailNum == 1)
                        contactInfoRecord.set("email_address", emailInfo.getInfo());
                    else
                        contactInfoRecord.set("email_" + emailNum + "_address", emailInfo.getInfo());
                    emailNum++;
                }

                contact_info_updated_time = user.getPropertyUpdatedTime(User.COL_EMAIL);
                record.set("contact_info_updated_time", contact_info_updated_time);
            }
        //deal with the phone
        int phoneNum = 1;
        if (user.getTel() != null) {
            for (TelInfo telInfo : user.getTel()) {
                if (StringUtils.isNotBlank(telInfo.getType())) {
                    if (phoneNum == 1)
                        contactInfoRecord.set("mobile_telephone_number", telInfo.getInfo());
                    else
                        contactInfoRecord.set("mobile_" + phoneNum + "_telephone_number", telInfo.getInfo());
                    phoneNum++;
                }
            }
            long tel_update_time = user.getPropertyUpdatedTime(User.COL_TEL);
            if (tel_update_time > contact_info_updated_time) {
                contact_info_updated_time = tel_update_time;
                record.set("contact_info_updated_time", contact_info_updated_time);
            }
            record.set("contact_info_updated_time", contact_info_updated_time);
        }
        if (contactInfoRecord.size() > 0 && checkCols("contact_info", mapContain, isCheck))
            record.putMissing("contact_info", JsonUtils.parse(JsonUtils.toJson(contactInfoRecord, false)));

        // deal with AddressInfo
        long address_updated_time = 0;
        if (user.getAddress() != null) {
            RecordSet list = new RecordSet();
            for (AddressInfo addressInfo : user.getAddress()) {
                Record addressInfoRecord = new Record();
                addressInfoRecord.set("city", addressInfo.getCity());
                addressInfoRecord.set("country", addressInfo.getCountry());
                addressInfoRecord.set("postal_code", addressInfo.getZipCode());
                addressInfoRecord.set("street", addressInfo.getStreet());
                addressInfoRecord.set("state", addressInfo.getProvince());
                addressInfoRecord.set("type", addressInfo.getType());
                addressInfoRecord.set("po_box", addressInfo.getPoBox());
                list.add(addressInfoRecord);
            }
            record.putMissing("address", list);
            address_updated_time = user.getPropertyUpdatedTime(User.COL_ADDRESS);
            record.set("address_updated_time", address_updated_time);
        } else {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            record.putMissing("address", jnf.arrayNode());
        }


        if (user.getWorkHistory() != null) {
            RecordSet list = new RecordSet();

            for (WorkHistory workHistory : user.getWorkHistory()) {
                Record workHistoryRecord = new Record();
                workHistoryRecord.set("company", workHistory.getCompany());
                workHistoryRecord.set("description", workHistory.getJobDescription());
                workHistoryRecord.set("address", workHistory.getOfficeLocation());
                workHistoryRecord.set("from", workHistory.getFrom());
                workHistoryRecord.set("to", workHistory.getTo());
                workHistoryRecord.set("title", workHistory.getTitle());
                workHistoryRecord.set("profession", workHistory.getDepartment());
                list.add(workHistoryRecord);
            }
            record.putMissing("work_history", list);
        } else {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            record.putMissing("work_history", jnf.arrayNode());
        }
        if (user.getEducationHistory() != null) {
            RecordSet list = new RecordSet();


            for (EduHistory eduHistory : user.getEducationHistory()) {
                Record eduHistoryRecord = new Record();
                eduHistoryRecord.set("degree", eduHistory.getDegree());
                eduHistoryRecord.set("major", eduHistory.getMajor());
                eduHistoryRecord.set("school", eduHistory.getSchool());
                eduHistoryRecord.set("class", eduHistory.getKlass());
                eduHistoryRecord.set("from", eduHistory.getFrom());
                eduHistoryRecord.set("to", eduHistory.getTo());
                eduHistoryRecord.set("type", eduHistory.getType());
                list.add(eduHistoryRecord);
            }
            record.putMissing("education_history", list.toJsonNode());
        } else {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            record.putMissing("education_history", jnf.arrayNode());
        }

        if (user.getMiscellaneous() != null) {
            Record recordMiscell = new Record();
            if (StringUtils.isNotBlank(user.getMiscellaneous().getOpenfacePhone()) && checkCols("miscellaneous", mapContain, isCheck))
                recordMiscell.set(MiscInfo.COL_OPENFACE_PHONE, user.getMiscellaneous().getOpenfacePhone());
            record.setMissing("miscellaneous", recordMiscell);
        } else {
            JsonNodeFactory jnf = JsonNodeFactory.instance;
            record.putMissing("miscellaneous", jnf.arrayNode());
        }

        /*record.set("basic_updated_time", 1329792757738l);
        record.set("status", "testting");
        record.set("status_updated_time", 1329792757738l);
        record.set("work_history_updated_time", 0);
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        record.putMissing("family", jnf.arrayNode());*/

        return record;
    }


}
