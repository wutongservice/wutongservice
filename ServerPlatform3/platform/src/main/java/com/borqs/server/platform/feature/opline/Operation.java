package com.borqs.server.platform.feature.opline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.friend.PeopleId;
import com.borqs.server.platform.util.DateHelper;
import com.borqs.server.platform.util.ObjectHelper;
import com.borqs.server.platform.util.RandomHelper;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;


public class Operation {

    private long operId;
    private long userId;
    private String asId;
    private int action;
    private Target[] targets;
    private int flag;
    private String info;

    public Operation() {
        this(0, 0L, "", Actions.NONE, null, 0, "");
    }

    public Operation(long operId, long userId, String asId, int action, Target[] targets, int flag, String info) {
        this.operId = operId;
        this.userId = userId;
        this.asId = asId;
        this.action = action;
        this.targets = targets;
        this.flag = flag;
        this.info = info;
    }

    public static Operation newOperation(Context ctx, long time, int action, String info, Target... targets) {
        return new Operation(RandomHelper.generateId(time), ctx.getViewer(), "", action, targets, 0, info);
    }

    public static Operation newOperation(Context ctx, int action, String info,Target... targets) {
        return newOperation(ctx, DateHelper.nowMillis(), action, info, targets);
    }

    public long getOperId() {
        return operId;
    }

    public void setOperId(long operId) {
        this.operId = operId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAsId() {
        return asId;
    }

    public void setAsId(String asId) {
        this.asId = asId;
    }

    public long getTime() {
        return RandomHelper.getTimestamp(operId);
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public Target[] getTargets() {
        return targets;
    }

    public void setTargets(Target[] targets) {
        this.targets = targets;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Operation other = (Operation) o;

        return action == other.action
                && flag == other.flag
                && operId == other.operId
                && userId == other.userId
                && StringUtils.equals(asId, other.asId)
                && StringUtils.equals(info, other.info)
                && ObjectUtils.equals(targets, other.targets);
    }

    @Override
    public int hashCode() {
        return ObjectHelper.hashCode(operId, userId, action, asId, targets, flag, info);
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("oper:").append(operId);
        buff.append("user:").append(userId);
        buff.append("as:").append(asId);
        buff.append("action:").append(Actions.actionToStr(action));
        buff.append("targets:").append(Target.toCompatibleString(targets, ","));
        buff.append("info:").append(info);
        return buff.toString();
    }

    public String toOperLogString() {
        StringBuilder buff = new StringBuilder();
        buff.append("oper:").append(operId);
        buff.append("as:").append(asId);
        buff.append("targets:").append(Target.toCompatibleString(targets, ","));
        buff.append("info:").append(info);
        return buff.toString();
    }
}
