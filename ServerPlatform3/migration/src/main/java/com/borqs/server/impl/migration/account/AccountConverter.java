package com.borqs.server.impl.migration.account;


import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AccountConverter {
    private static final Logger L = LoggerFactory.getLogger(AccountConverter.class);
    public static final String HTTP_APITEST_BORQS_COM_PROFILE_IMAGE = "http://apitest.borqs.com/profile_image";
    public static final String HTTP_APITEST_BORQS_COM_SYS_ICON = "http://apitest.borqs.com/sys/icon";


    private static void splitPath(ResultSet record) throws SQLException {
        if (record == null)
            return;

        String image_url = "";
        String small_image_url = "";
        String large_image_url = "";

        if (record.getString("image_url") != null && record.getString("image_url").startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            image_url = changePath((String) record.getString("image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.updateString("image_url", image_url);
        }
        if (record.getString("image_url") != null && record.getString("image_url").startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.updateString("image_url", "");
        }
        if (record.getString("small_image_url") != null && record.getString("small_image_url").startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            small_image_url = changePath((String) record.getString("small_image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.updateString("small_image_url", small_image_url);
        }
        if (record.getString("small_image_url") != null && record.getString("small_image_url").startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.updateString("small_image_url", "");
        }

        if (record.getString("large_image_url") != null && record.getString("large_image_url").startsWith(HTTP_APITEST_BORQS_COM_PROFILE_IMAGE)) {
            large_image_url = changePath((String) record.getString("large_image_url"), HTTP_APITEST_BORQS_COM_PROFILE_IMAGE);
            record.updateString("large_image_url", large_image_url);
        }
        if (record.getString("large_image_url") != null && record.getString("large_image_url").startsWith(HTTP_APITEST_BORQS_COM_SYS_ICON)) {
            record.updateString("large_image_url", "");
        }
    }

    private static String changePath(String path, String modelStr) {
        if (path.indexOf(modelStr) == -1)
            return path;
        return path.substring(modelStr.length() + 1, path.length());
    }


    public static User converterRecord2User(ResultSet record) {
        User user = new User();
        //take out the url path like "Http://"
        try {
            splitPath(record);


            if (StringUtils.isNotEmpty(record.getString("user_id")))
                user.setUserId(record.getLong("user_id"));
            if (StringUtils.isNotEmpty(record.getString("password")))
                user.setPassword(record.getString("password"));
            if (StringUtils.isNotEmpty(record.getString("created_time")))
                user.setCreatedTime(record.getLong("created_time"));
            if (StringUtils.isNotEmpty(record.getString("destroyed_time")))
                user.setDestroyedTime(record.getLong("destroyed_time"));
            if (StringUtils.isNotEmpty(record.getString("nick_name")))
                user.setNickname(record.getString("nick_name"));

            /*if (StringUtils.isNotEmpty(record.getString("login_email1")))
            user.setLoginEmail1(record.getString("login_email1"));
        if (StringUtils.isNotEmpty(record.getString("login_email2")))
            user.setLoginEmail2(record.getString("login_email2"));
        if (StringUtils.isNotEmpty(record.getString("login_email3")))
            user.setLoginEmail3(record.getString("login_email3"));
        if (StringUtils.isNotEmpty(record.getString("login_phone1")))
            user.setLoginPhone1(record.getString("login_phone1"));
        if (StringUtils.isNotEmpty(record.getString("login_phone2")))
            user.setLoginPhone2(record.getString("login_phone2"));
        if (StringUtils.isNotEmpty(record.getString("login_phone3")))
            user.setLoginPhone3(record.getString("login_phone3"));*/


            if (StringUtils.isNotEmpty(record.getString("display_name")))
                user.setName(NameInfo.split(record.getString("display_name")));

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
            if (StringUtils.isNotEmpty(record.getString("contact_info"))) {
                String contact_info = record.getString("contact_info");
                if (contact_info != null || contact_info.length() > 1) {
                    JsonNode jn = JsonHelper.parse(contact_info);
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
                    if (isTel)
                        user.setTel(telInfoList);
                    if (isEmail)
                        user.setEmail(emailInfoList);
                }
            }

            //deal with old version address json Object
            List<AddressInfo> addressInfoList = new ArrayList<AddressInfo>();
            if (StringUtils.isNotEmpty(record.getString("address"))) {
                String address = record.getString("address");
                if (address != null || address.length() > 1) {
                    JsonNode jsonNode = JsonHelper.parse(address);
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
            if (StringUtils.isNotEmpty(record.getString("work_history"))) {
                List<WorkHistory> workHistoryList = new ArrayList<WorkHistory>();
                String wrokHistory = record.getString("work_history");
                if (wrokHistory != null || wrokHistory.length() > 1) {
                    JsonNode jsonNode = JsonHelper.parse(wrokHistory);
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

            if (StringUtils.isNotEmpty(record.getString("education_history"))) {
                List<EduHistory> eduHistoryList = new ArrayList<EduHistory>();
                String eduHistoryStr = record.getString("education_history");
                if (eduHistoryStr != null || eduHistoryStr.length() > 1) {
                    JsonNode jsonNode = JsonHelper.parse(eduHistoryStr);
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

                            /*if (jn.get("type") != null && StringUtils.isNotEmpty(jn.get("type").getTextValue()))
                                eduHistory.setType(jn.get("type").getTextValue());*/
                            if (eduHistory.toString().length() > 0)
                                eduHistoryList.add(eduHistory);
                        }
                    }
                }
                if (eduHistoryList.size() > 0)
                    user.setEducationHistory(eduHistoryList);
            }

            if (StringUtils.isNotEmpty(record.getString("miscellaneous"))) {
                String miscell = record.getString("miscellaneous");
                if (miscell != null || miscell.length() > 1) {
                    JsonNode jsonNode = JsonHelper.parse(miscell);

                    MiscInfo miscInfo = new MiscInfo();
                    if (jsonNode.get(MiscInfo.COL_OPENFACE_PHONE) != null && StringUtils.isNotEmpty(jsonNode.get(MiscInfo.COL_OPENFACE_PHONE).getTextValue()))
                        miscInfo.setOpenfacePhone(jsonNode.get(MiscInfo.COL_OPENFACE_PHONE).getTextValue());

                    user.setMiscellaneous(miscInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }


}
