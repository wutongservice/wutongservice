package com.borqs.server.platform.feature.conversation;

import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;

public class ConversationBase {

    protected Target target;
    protected int reason;

    public ConversationBase() {
        this(null, Actions.NONE);
    }

    public ConversationBase(Target target, int reason) {
        this.target = target;
        this.reason = reason;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }
}
