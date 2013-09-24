package com.borqs.server.platform.feature.account;



import java.util.Map;

public interface PropertyBundle {
    Map<Integer, Object> writeProperties(Map<Integer, Object> reuse);
    void readProperties(Map<Integer, Object> props, boolean partial);
}
