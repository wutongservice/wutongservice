package com.borqs.server.base.util;
import com.borqs.server.base.data.Values;

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
