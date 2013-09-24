package com.borqs.server.market.services;


import com.borqs.server.market.utils.ObjectUtils2;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class UserId {
    public static final int ROLE_UNKNOWN = 0;
    public static final int ROLE_PURCHASER = 1;
    public static final int ROLE_PUBLISHER = 2;
    public static final int ROLE_DEVELOPER = 3;
    public static final int ROLE_ADMIN = 4;

    private final int role;
    private final String id;

    public UserId(int role, String id) {
        this.role = role;
        this.id = id;
    }

    public int getRole() {
        return role;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UserId other = (UserId) o;
        return role == other.role && StringUtils.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return ObjectUtils2.hashCodeMulti(role, id);
    }

    @Override
    public String toString() {
        switch (role) {
            case ROLE_DEVELOPER:
                return "developer_" + ObjectUtils.toString(id);
            case ROLE_PUBLISHER:
                return "publisher_" + ObjectUtils.toString(id);
            case ROLE_PURCHASER:
                return "purchaser_" + ObjectUtils.toString(id);
            default:
                return role + "_" + ObjectUtils.toString(id);
        }
    }
}
