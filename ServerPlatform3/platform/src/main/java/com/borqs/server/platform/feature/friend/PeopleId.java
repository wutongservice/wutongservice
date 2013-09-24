package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.feature.Target;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public class PeopleId extends Target {

    public PeopleId(int type, String id) {
        super(checkType(type), id);
    }

    private static int checkType(int type) {
        if (type == USER || type == CONTACT)
            return type;
        else
            throw new IllegalArgumentException("Invalid people type " + type);
    }


    public static PeopleId user(long userId) {
        return new PeopleId(USER, Long.toString(userId));
    }

    public static PeopleId contact(String contactId) {
        Validate.notNull(contactId);
        return new PeopleId(CONTACT, contactId);
    }

    @Override
    public PeopleId copy() {
        return new PeopleId(type, id);
    }

    @Override
    public String toString() {
        if (type == USER) {
            return StringUtils.isEmpty(id) ? "0" : id;
        } else if (type == CONTACT) {
            return "contact:" + ObjectUtils.toString(id);
        } else {
            return "unknown:" + ObjectUtils.toString(id);
        }
    }

    public String toStringId() {
        if (isUser()) {
            return StringUtils.isNotEmpty(id) ? id : "0";
        } else {
            String typeStr = type == CONTACT ? "c:" : "u:";
            return typeStr + id;
        }
    }

    public static PeopleId parseStringId(String s) {
        if (s.startsWith("c:")) {
            return contact(StringUtils.removeStart(s, "c:"));
        } else {
            long id = 0;
            try {
                id = Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
            if (id <= 0)
                throw new IllegalArgumentException("Illegal user id " + s);

            return user(id);
        }
    }

    public Object toId() {
        if (isUser()) {
            return getIdAsLong();
        } else {
            String typeStr = type == CONTACT ? "c:" : "u:";
            return typeStr + id;
        }
    }

    public static PeopleId fromId(Object o) {
        if (o instanceof Number) {
            return user(((Number) o).longValue());
        } else if (o != null) {
            return parseStringId(o.toString());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean isUser() {
        return type == USER;
    }

    public boolean isContact() {
        return type == CONTACT;
    }
}
