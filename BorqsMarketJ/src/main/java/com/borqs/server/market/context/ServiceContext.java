package com.borqs.server.market.context;


import com.borqs.server.market.services.UserId;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public class ServiceContext {
    public static final int DEVICE_PHONE = 1;
    public static final int DEVICE_PAD = 2;
    public static final int DEVICE_PC = 3;

    private String clientIP;
    private String clientUserAgent;
    private int clientRole;
    private String clientId;
    private String clientDeviceType;
    private String clientDeviceId;
    private String clientOS;
    private String clientBrowserRenderEngine;
    private String clientLanguage;
    private long accessTime;
    private String[] clientGoogleIds;

    public ServiceContext() {
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public String getClientUserAgent() {
        return clientUserAgent;
    }

    public void setClientUserAgent(String clientUserAgent) {
        this.clientUserAgent = clientUserAgent;
    }

    public int getClientRole() {
        return clientRole;
    }

    public void setClientRole(int clientRole) {
        this.clientRole = clientRole;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientDeviceType() {
        return clientDeviceType;
    }

    public void setClientDeviceType(String clientDeviceType) {
        this.clientDeviceType = clientDeviceType;
    }

    public String getClientDeviceId() {
        return clientDeviceId;
    }

    public void setClientDeviceId(String clientDeviceId) {
        this.clientDeviceId = clientDeviceId;
    }

    public String getClientOS() {
        return clientOS;
    }

    public void setClientOS(String clientOS) {
        this.clientOS = clientOS;
    }

    public String getClientBrowserRenderEngine() {
        return clientBrowserRenderEngine;
    }

    public void setClientBrowserRenderEngine(String clientBrowserRenderEngine) {
        this.clientBrowserRenderEngine = clientBrowserRenderEngine;
    }

    public String getClientLanguage() {
        return clientLanguage;
    }

    public String getClientLanguage(String def) {
        return StringUtils.isEmpty(clientLanguage) ? def : clientLanguage;
    }

    public void setClientLanguage(String clientLanguage) {
        this.clientLanguage = clientLanguage;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public String[] getClientGoogleIds() {
        return clientGoogleIds;
    }

    public List<String> getClientGoogleIdsAsList() {
        return hasClientGoogleIds() ? Arrays.asList(clientGoogleIds) : null;
    }

    public void setClientGoogleIds(String[] clientGoogleIds) {
        this.clientGoogleIds = clientGoogleIds;
    }

    public boolean hasClientId() {
        return StringUtils.isNotEmpty(clientId);
    }

    public boolean hasPurchaserId() {
        return clientRole == UserId.ROLE_PURCHASER && hasClientId();
    }

    public String getPurchaserId() {
        return hasPurchaserId() ? getClientId() : null;
    }

    public boolean hasClientDeviceId() {
        return StringUtils.isNotEmpty(clientDeviceId);
    }

    public boolean hasClientGoogleIds() {
        return ArrayUtils.isNotEmpty(clientGoogleIds);
    }

    public String getClientGoogleId(int n) {
        if (ArrayUtils.isNotEmpty(clientGoogleIds))
            return null;

        if (n >= clientGoogleIds.length)
            return null;

        return clientGoogleIds[n];
    }
}
