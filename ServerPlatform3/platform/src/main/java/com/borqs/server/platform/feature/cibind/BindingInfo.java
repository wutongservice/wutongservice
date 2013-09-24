package com.borqs.server.platform.feature.cibind;


public class BindingInfo {

    public static final String MOBILE_TEL = "mobile";
    public static final String EMAIL = "email";

    private String type;
    private String info;

    public BindingInfo() {
        this("", "");
    }

    public BindingInfo(String type, String info) {
        this.type = type;
        this.info = info;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
