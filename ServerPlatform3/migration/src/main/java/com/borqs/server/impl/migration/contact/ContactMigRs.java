package com.borqs.server.impl.migration.contact;


import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.ContentTypes;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.util.DateHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactMigRs {


    public static Contact readSocialContact(ResultSet rs, Map<String, String> setting, Map<Long, String> mapAccount) throws SQLException {
        Contact ms = new Contact();
        long owner = rs.getLong("owner");
        if (!mapAccount.containsKey(owner))
            return null;

        ms.setOwner(owner);
        ms.setContent(rs.getString("content"));
        ms.setName(rs.getString("username"));
        ms.setReason(Reasons.UPLOAD_CONTACTS);
        ms.setType(rs.getInt("type"));
        ms.setCreatedTime(DateHelper.nowMillis());
        return ms;
    }

    public static Contact readVirtualFriendContact(ResultSet rs, Map<String, String> setting, Map<Long, String> mapAccount) throws SQLException {
        Contact ms = new Contact();
        long user = rs.getLong("user_id");
        if (!mapAccount.containsKey(user))
            return null;
        ms.setOwner(user);
        ms.setName(rs.getString("name"));
        ms.setReason(Reasons.CONTACTS_FRIEND);

        String content = rs.getString("content");
        ms.setContent(content);
        if(checkEmail(content))
           ms.setType(ContentTypes.CONTACT_CONTENT_EMAIL);
        else
            ms.setType(ContentTypes.CONTACT_CONTENT_TEL);
        
        ms.setCreatedTime(DateHelper.nowMillis());
        return ms;
    }

    public static boolean checkEmail(String mail) {
        String regex = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(mail);
        return m.find();
    }

}
