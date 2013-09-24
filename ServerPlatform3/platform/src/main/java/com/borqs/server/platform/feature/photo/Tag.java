package com.borqs.server.platform.feature.photo;


public class Tag {

    private int x;
    private int y;
    private long user_id;
    private String tag_text;



    public Tag() {
    }

    public Tag(int x, int y, long user_id, String tag_text) {
        this.x = x;
        this.y = y;
        this.user_id = user_id;
        this.tag_text = tag_text;
    }



    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getTag_text() {
        return tag_text;
    }

    public void setTag_text(String tag_text) {
        this.tag_text = tag_text;
    }
}
