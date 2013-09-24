package com.borqs.server.market.context;


import com.borqs.server.market.models.IPSource;
import com.borqs.server.market.utils.mybatis.record.RecordSession;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ServiceContext {
    private String clientIP;
    private String clientUserAgent;
    private String accountId;
    private String accountName;
    private String accountEmail;
    private boolean borqs = false;
    private boolean boss = false;
    private String ticket;
    private String clientDeviceType;
    private String clientDeviceId;
    private String clientOS;
    private String clientBrowserRenderEngine;
    private String clientLocale;
    private long accessTime;
    private int roles = 0;
    private final Map<String, Object> sessions = new HashMap<String, Object>();

    public ServiceContext() {
    }

    public String getClientIP() {
        return clientIP;
    }

    public String getClientIP(String def) {
        return ObjectUtils.toString(clientIP, def);
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public String getClientUserAgent() {
        return clientUserAgent;
    }

    public String getClientUserAgent(String def) {
        return ObjectUtils.toString(clientUserAgent, def);
    }

    public void setClientUserAgent(String clientUserAgent) {
        this.clientUserAgent = clientUserAgent;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public boolean isBorqs() {
        return borqs;
    }

    public void setBorqs(boolean borqs) {
        this.borqs = borqs;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
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

    public String getClientDeviceId(String def) {
        return ObjectUtils.toString(clientDeviceId, def);
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

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getClientLocale() {
        return clientLocale;
    }

    public String getClientLocale(String def) {
        return ObjectUtils.toString(clientLocale, def);
    }

    public void setClientLocale(String clientLocale) {
        this.clientLocale = clientLocale;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public boolean hasAccountId() {
        return StringUtils.isNotEmpty(accountId);
    }

    public boolean hasClientDeviceId() {
        return StringUtils.isNotEmpty(clientDeviceId);
    }

    public void setSession(String key, Object val) {
        sessions.put(key, val);
    }

    public Object getSession(String key) {
        return sessions.get(key);
    }

    public boolean hasSession(String key) {
        return sessions.containsKey(key);
    }

    public int getRoles() {
        return roles;
    }

    public void setRoles(int roles) {
        this.roles = roles;
    }

    public String getCountry() {
        try {
            return LocaleUtils.toLocale(getClientLocale()).getCountry();
        } catch (Exception e) {
            return "";
        }
    }

    public String getCountryByIp(RecordSession session) {
        return IPSource.getCountry(session, getClientIP());
    }
}
