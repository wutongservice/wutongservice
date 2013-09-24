package com.borqs.server.platform.rpc;


public class ServiceDelegate {
    private String category;
    private String delegateInterface;
    private Object delegateObject;

    public ServiceDelegate() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDelegateInterface() {
        return delegateInterface;
    }

    public void setDelegateInterface(String delegateInterface) {
        this.delegateInterface = delegateInterface;
    }

    public Object getDelegateObject() {
        return delegateObject;
    }

    public void setDelegateObject(Object delegateObject) {
        this.delegateObject = delegateObject;
    }
}
