package com.borqs.server.platform.test.mock;


import com.borqs.server.platform.feature.account.*;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.Encoders;

public class UserMocks {

    public static User newFullInfoUser(long userId, String pwd, String fullName) {
        // basic
        User user = new User(userId);
        user.setPassword(Encoders.md5Hex(pwd));
        user.setCreatedTime(DateHelper.nowMillis());
        user.setDestroyedTime(0);

        // name
        NameInfo name = NameInfo.split(fullName);
        name.setMiddle("");
        name.setFirstPhonetic("first_phonetic");
        name.setMiddlePhonetic("middle_phonetic");
        name.setLastPhonetic("last_phonetic");
        name.setPrefix("Mr.");
        name.setPostfix("");
        user.setName(name);

        // nickname
        user.setNickname("nickname");

        // photo
        user.setPhoto(new PhotoInfo("http://middle_photo", "http://small_photo", "http://large_url"));

        // profile
        ProfileInfo profile = new ProfileInfo();
        profile.setGender("m");
        profile.setTimezone("timezone");
        profile.setLanguages("chinese");
        profile.setMarriage("y");
        profile.setReligion("no");
        profile.setDescription("hello world");
        profile.setInterests("program");
        user.setProfile(profile);

        // date
        user.setDate(
                new DateInfo(DateInfo.TYPE_ANNIVERSARY, "xxxx"),
                new DateInfo(DateInfo.TYPE_BIRTHDAY, "yyyy"),
                new DateInfo(DateInfo.TYPE_OTHER, "zzz"),
                new DateInfo("x-my", "www", "my")
        );

        // tel
        user.setTel(
                new TelInfo(TelInfo.TYPE_CAR, "xxxx"),
                new TelInfo(TelInfo.TYPE_MOBILE, "13800xxxxx", true, "")
        );

        // email
        user.setEmail(
                new EmailInfo(EmailInfo.TYPE_HOME, "home@abc.com", true, ""),
                new EmailInfo(EmailInfo.TYPE_WORK, "work@abc.com")
        );

        // im
        user.setIm(
                new ImInfo(ImInfo.TYPE_QQ, "xxxxxx")
        );

        // sip_address
        user.setSipAddress(
                new SipAddressInfo(SipAddressInfo.TYPE_HOME, "xxxx"),
                new SipAddressInfo(SipAddressInfo.TYPE_WORK, "yyyy")
        );

        // url
        user.setUrl(
                new UrlInfo(UrlInfo.TYPE_HOMEPAGE, "http://hello_world"),
                new UrlInfo(UrlInfo.TYPE_BLOG, "http://hello_world.blogger.com")
        );

        // org
        user.setOrganization(
                new OrgInfo(OrgInfo.TYPE_WORK, "user", "title", "department", "wangjing", "dev", "symbol", "B-O-R-Q-S", "")
        );

        // address
        user.setAddress(
                new AddressInfo(AddressInfo.TYPE_WORK, "china", "beijing", "beijing", "wangjing", "100000", "pobox", "neighborhood", ""),
                new AddressInfo(AddressInfo.TYPE_HOME, "china", "home", "home", "home", "100000", "pobox", "neighborhood", "")
        );

        // work_history
        user.setWorkHistory(
                new WorkHistory("2011", "2012", "user", "title", "department", "wangjing", "dev", "symbol", "phoneticName")
        );

        // education_history
        user.setEducationHistory(
                new EduHistory("1997", "2001", EduHistory.TYPE_UNIVERSITY, "syiae", "74321", "degree", "major", "label")
        );

        // misc
        MiscInfo miscInfo = new MiscInfo();
        miscInfo.setOpenfacePhone("8989009");
        user.setMiscellaneous(miscInfo);

        return user;
    }
}
