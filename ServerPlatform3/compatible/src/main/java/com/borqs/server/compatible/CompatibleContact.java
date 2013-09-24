package com.borqs.server.compatible;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.feature.account.User;
import com.borqs.server.platform.feature.account.Users;
import com.borqs.server.platform.feature.contact.Contact;
import com.borqs.server.platform.feature.contact.Contacts;
import com.borqs.server.platform.feature.contact.Reasons;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.json.JsonGenerateHandler;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class CompatibleContact {
    public static final String V1COL_CONTACT_ID = "contact_id";
    public static final String V1COL_USER_ID = "user_id";
    public static final String V1COL_USER_NAME = "username";
    public static final String V1COL_TYPE = "type";
    public static final String V1COL_CONTENT = "content";
    public static final String V1COL_IS_FRIEND = "isfriend";
    public static final String V1COL_DISPLAY_NAME = "display_name";
    public static final String V1COL_IMAGE_URL = "image_url";
    
    
    public static Contacts jsonToContacts(String json, Contacts reuse, long ownerId, long now) {
        if (reuse == null)
            reuse = new Contacts();

        try {
            deserializeContacts(JsonHelper.parse(json), reuse, ownerId, now);
        } catch (IOException e) {
            throw new ServerException(E.JSON, "Deserialize contacts error");
        }

        return reuse;
    }

    public static Contact jsonToContact(String json, long ownerId, long now) {
        Contact contact = new Contact();
        try {
            deserializeContact(JsonHelper.parse(json), contact, ownerId, now);
        } catch (IOException e) {
            throw new ServerException(E.JSON, "Deserialize contact error");
        }
        return contact;
    }

    public static void deserializeContacts(JsonNode jn, Contacts contacts, long ownerId, long now) throws IOException {
        for (int i = 0; i < jn.size(); i++) {
            JsonNode sub = jn.get(i);
            Contact contact = new Contact();
            deserializeContact(sub, contact, ownerId, now);
            contacts.add(contact);
        }
    }

    public static void deserializeContact(JsonNode jn, Contact contact, long ownerId, long now) throws IOException {
        contact.setOwner(ownerId);
        contact.setName(jn.path(V1COL_USER_NAME).getTextValue());
        contact.setType(jn.path(V1COL_TYPE).getIntValue());
        contact.setContent(jn.path(V1COL_CONTENT).getTextValue());
        contact.setLocalId(jn.path(V1COL_CONTACT_ID).getValueAsText());
        contact.setReason(Reasons.UPLOAD_CONTACTS);
        contact.setCreatedTime(now);
    }

    public static void serializeContact(JsonGenerator jg, Contact contact, User user, boolean isFriend) throws IOException {
        jg.writeStartObject();

        jg.writeStringField(V1COL_CONTACT_ID, contact.getLocalId());
        jg.writeStringField(V1COL_USER_ID, ObjectUtils.toString(user.getUserId()));
        jg.writeStringField(V1COL_USER_NAME, contact.getName());
        jg.writeNumberField(V1COL_TYPE, contact.getType());
        jg.writeStringField(V1COL_CONTENT, contact.getContent());
        jg.writeBooleanField(V1COL_IS_FRIEND, isFriend);
        jg.writeStringField(V1COL_DISPLAY_NAME, user.getDisplayName());
        jg.writeStringField(V1COL_IMAGE_URL, user.getPhoto().getMiddleUrl());

        jg.writeEndObject();
    }

    public static void serializeContacts(JsonGenerator jg, Map<Contact, User> cuMap, Map<Contact, Boolean> cfMap) throws IOException {
        jg.writeStartArray();
        if (cuMap != null && cfMap != null) {
            Set<Contact> contacts = cuMap.keySet();
            for (Contact contact : contacts) {
                User user = cuMap.get(contact);
                boolean isFriend = cfMap.get(contact);
                serializeContact(jg, contact, user, isFriend);
            }
        }
        jg.writeEndArray();
    }

    public static void serializeContact(JsonGenerator jg, Contact contact) throws IOException {
        jg.writeStartObject();

        jg.writeStringField(V1COL_CONTACT_ID, contact.getLocalId());
        jg.writeStringField(V1COL_USER_NAME, contact.getName());
        jg.writeNumberField(V1COL_TYPE, contact.getType());
        jg.writeStringField(V1COL_CONTENT, contact.getContent());

        jg.writeEndObject();
    }

    public static void serializeContacts(JsonGenerator jg, Contacts contacts) throws IOException {
        jg.writeStartArray();
        if (CollectionUtils.isNotEmpty(contacts)) {
            for (Contact contact : contacts) {
                if (contact != null)
                    serializeContact(jg, contact);
            }
        }
        jg.writeEndArray();
    }

    public static String contactToJson(final Contact contact, final User user, final boolean isFriend, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeContact(jg, contact, user, isFriend);
            }
        }, human);
    }

    public static String contactsToJson(final Map<Contact, User> cuMap, final Map<Contact, Boolean> cfMap, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeContacts(jg, cuMap, cfMap);
            }
        }, human);
    }

    public static String contactToJson(final Contact contact, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeContact(jg, contact);
            }
        }, human);
    }

    public static String contactsToJson(final Contacts contacts, boolean human) {
        return JsonHelper.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg, Object arg) throws IOException {
                serializeContacts(jg, contacts);
            }
        }, human);
    }
}
