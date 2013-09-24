package com.borqs.server.platform.account2;


public class PropertyEntry {
    public final int key;
    public final int sub;
    public final int index;
    public final Object value;
    public final long updatedTime;

    public PropertyEntry(int key, int sub, int index, Object value, long updatedTime) {
        this.key = key;
        this.sub = sub;
        this.index = index;
        this.value = value;
        this.updatedTime = updatedTime;
    }
}
