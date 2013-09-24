package com.borqs.server.platform.feature.conversation;


import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;

public class Conversation extends ConversationBase {
    protected long user;
    protected long createdTime;

    public Conversation() {
        this(0, null, Actions.NONE, 0L);
    }

    public Conversation(long user, Target target, int reason, long createdTime) {
        super(target, reason);
        this.user = user;
        this.createdTime = createdTime;
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Conversation other = (Conversation) o;
        return user == other.user && target.equals(other.target)
                && reason == other.reason;
    }
}
