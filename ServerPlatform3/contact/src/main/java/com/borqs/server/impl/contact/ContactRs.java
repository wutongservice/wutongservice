package com.borqs.server.impl.contact;

import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.Contacts;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ContactRs {
    public static Contacts read(ResultSet rs) throws SQLException {
        Contacts contacts = new Contacts();
        while (rs.next()) {
            contacts.add(new Contact(rs.getLong("owner"), rs.getString("name"),
                    rs.getInt("type"), rs.getString("content"),
                    rs.getInt("reason"), rs.getLong("created_time")));
        }

        return contacts;
    }
}
