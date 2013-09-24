package com.borqs.server.platform.feature.group;

import com.borqs.server.platform.data.Record;


public class GroupOptions {
    private long begin;
    private int memberLimit;
    private int isStreamPublic;
    private int canSearch;
    private int canViewMembers;
    private int canJoin;
    private int canMemberInvite;
    private int canMemberApprove;
    private int canMemberPost;
    private String label;
    private Record properties;

    public GroupOptions() {
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public int getMemberLimit() {
        return memberLimit;
    }

    public void setMemberLimit(int memberLimit) {
        this.memberLimit = memberLimit;
    }

    public int getStreamPublic() {
        return isStreamPublic;
    }

    public void setStreamPublic(int streamPublic) {
        isStreamPublic = streamPublic;
    }

    public int getCanSearch() {
        return canSearch;
    }

    public void setCanSearch(int canSearch) {
        this.canSearch = canSearch;
    }

    public int getCanViewMembers() {
        return canViewMembers;
    }

    public void setCanViewMembers(int canViewMembers) {
        this.canViewMembers = canViewMembers;
    }

    public int getCanJoin() {
        return canJoin;
    }

    public void setCanJoin(int canJoin) {
        this.canJoin = canJoin;
    }

    public int getCanMemberInvite() {
        return canMemberInvite;
    }

    public void setCanMemberInvite(int canMemberInvite) {
        this.canMemberInvite = canMemberInvite;
    }

    public int getCanMemberApprove() {
        return canMemberApprove;
    }

    public void setCanMemberApprove(int canMemberApprove) {
        this.canMemberApprove = canMemberApprove;
    }

    public int getCanMemberPost() {
        return canMemberPost;
    }

    public void setCanMemberPost(int canMemberPost) {
        this.canMemberPost = canMemberPost;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Record getProperties() {
        return properties;
    }

    public void setProperties(Record properties) {
        this.properties = properties;
    }
}
