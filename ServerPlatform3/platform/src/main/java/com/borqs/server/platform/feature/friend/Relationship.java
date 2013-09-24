package com.borqs.server.platform.feature.friend;


import com.borqs.server.platform.util.Copyable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public class Relationship {

    private PeopleId viewerId;
    private PeopleId targetId;

    private List<InCircle> targetInViewerCircles;
    private List<InCircle> viewerInTargetCircles;

    public Relationship() {
    }

    public Relationship(PeopleId viewerId, PeopleId targetId, List<InCircle> targetInViewerCircles, List<InCircle> viewerInTargetCircles) {
        this.viewerId = viewerId;
        this.targetId = targetId;
        this.targetInViewerCircles = targetInViewerCircles;
        this.viewerInTargetCircles = viewerInTargetCircles;
    }

    public PeopleId getViewerId() {
        return viewerId;
    }

    public void setViewerId(PeopleId viewerId) {
        this.viewerId = viewerId;
    }

    public PeopleId getTargetId() {
        return targetId;
    }

    public void setTargetId(PeopleId targetId) {
        this.targetId = targetId;
    }

    public List<InCircle> getTargetInViewerCircles() {
        return targetInViewerCircles;
    }

    public void setTargetInViewerCircles(List<InCircle> targetInViewerCircles) {
        this.targetInViewerCircles = targetInViewerCircles;
    }

    public List<InCircle> getViewerInTargetCircles() {
        return viewerInTargetCircles;
    }

    public void setViewerInTargetCircles(List<InCircle> viewerInTargetCircles) {
        this.viewerInTargetCircles = viewerInTargetCircles;
    }

    public void addTargetInViewer(int circleId, int reason, long updatedTime) {
        if (targetInViewerCircles == null)
            targetInViewerCircles = new ArrayList<InCircle>();

        targetInViewerCircles.add(new InCircle(circleId, reason, updatedTime));
    }

    public void addViewerInTarget(int circleId, int reason, long updatedTime) {
        if (viewerInTargetCircles == null)
            viewerInTargetCircles = new ArrayList<InCircle>();

        viewerInTargetCircles.add(new InCircle(circleId, reason, updatedTime));
    }

    public boolean isDisrelated() {
        return CollectionUtils.isEmpty(viewerInTargetCircles) && CollectionUtils.isEmpty(targetInViewerCircles);
    }

    public boolean isMutual() {
        return CollectionUtils.isNotEmpty(viewerInTargetCircles) && CollectionUtils.isNotEmpty(targetInViewerCircles);
    }

    public boolean isBidi() {
        return isMutual();
    }

    public boolean isTargetFriend() {
        if (CollectionUtils.isEmpty(viewerInTargetCircles))
            return false;

        for (InCircle inCircle : viewerInTargetCircles) {
            if (inCircle.circleId == Circle.CIRCLE_BLOCKED)
                return false;
        }
        return true;
    }

    public boolean isViewerFriend() {
        if (CollectionUtils.isEmpty(targetInViewerCircles))
            return false;

        for (InCircle inCircle : targetInViewerCircles) {
            if (inCircle.circleId == Circle.CIRCLE_BLOCKED)
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("rel: ");
        if (isDisrelated()) {
            buff.append(ObjectUtils.toString(viewerId)).append(" and ").append(ObjectUtils.toString(targetId)).append(" disrelated");
        } else {
            buff.append("{");
            buff.append(ObjectUtils.toString(viewerId)).append(" in ").append(ObjectUtils.toString(targetId));
            inCirclesToStr(buff, viewerInTargetCircles);
            buff.append(" | ");
            buff.append(ObjectUtils.toString(targetId)).append(" in ").append(ObjectUtils.toString(viewerId));
            inCirclesToStr(buff, targetInViewerCircles);
            buff.append("}");
        }
        return buff.toString();
    }

    private void inCirclesToStr(StringBuilder buff, List<InCircle> inCircles) {
        buff.append("(");
        if (CollectionUtils.isNotEmpty(inCircles)) {
            for (int i = 0; i < inCircles.size(); i++) {
                if (i > 0)
                    buff.append(",");
                buff.append(inCircles.get(i).circleId);
            }
        }
        buff.append(")");
    }

    public int[] getTargetInViewerCircleIds() {
        return getCircleIds(targetInViewerCircles);
    }

    public int[] getViewerInTargetCircleIds() {
        return getCircleIds(viewerInTargetCircles);
    }

    private static int[] getCircleIds(List<InCircle> inCircles) {
        if (CollectionUtils.isEmpty(inCircles)) {
            return new int[0];
        } else {
            int len = inCircles.size();
            int[] a = new int[len];
            for (int i = 0; i < len; i++)
                a[i] = inCircles.get(i).circleId;
            return a;
        }
    }

    public static Relationship disrelated(PeopleId viewer, PeopleId target) {
        return new Relationship(viewer, target, null, null);
    }


    public static class InCircle implements Copyable<InCircle> {
        public final int circleId;
        public final int reason;
        public final long updatedTime;

        public InCircle(int circleId, int reason, long updatedTime) {
            this.circleId = circleId;
            this.reason = reason;
            this.updatedTime = updatedTime;
        }

        @Override
        public InCircle copy() {
            return new InCircle(circleId, reason, updatedTime);
        }
    }
}
