package com.borqs.server.platform.util;


import com.borqs.server.platform.data.Values;

public final class ObjectHolder<T> {
    public T value;

    public ObjectHolder() {
        this(null);
    }

    public ObjectHolder(T value) {
        this.value = value;
    }

    public long toInt() {
        return Values.toInt(value);
    }

    @Override
    public String toString() {
        return Values.toString(value);
    }
}
