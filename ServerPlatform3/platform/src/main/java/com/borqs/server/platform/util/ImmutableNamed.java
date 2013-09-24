package com.borqs.server.platform.util;


import org.apache.commons.lang.Validate;

public abstract class ImmutableNamed {
    protected final String name;

    protected ImmutableNamed(String name) {
        Validate.notNull(name);
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
