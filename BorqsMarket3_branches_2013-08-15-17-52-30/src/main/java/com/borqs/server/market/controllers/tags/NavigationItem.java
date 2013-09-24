package com.borqs.server.market.controllers.tags;


import org.apache.commons.lang.ObjectUtils;

public class NavigationItem {

    public static final NavigationItem NULL = new NavigationItem("null", "null", "");

    private final String id;
    private final String name;
    private final String link;

    public NavigationItem(String id, String name, String link) {
        this.id = ObjectUtils.toString(id);
        this.name = ObjectUtils.toString(name);
        this.link = ObjectUtils.toString(link);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

}
