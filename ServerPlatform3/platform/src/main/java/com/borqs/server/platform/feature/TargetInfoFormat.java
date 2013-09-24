package com.borqs.server.platform.feature;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.util.EntityFormat;
import org.apache.commons.lang.ObjectUtils;
import org.apache.tools.ant.UnsupportedAttributeException;

public abstract class TargetInfoFormat implements EntityFormat.Format {

    public static TargetInfoFormat PLAIN = new TargetInfoFormat() {
        @Override
        public String formatTargetInfo(TargetInfo ti) {
            return ti != null ? ObjectUtils.toString(ti.getName()) : "";
        }
    };

    public static TargetInfoFormat ANDROID_LINK = new TargetInfoFormat() {
        @Override
        public String formatTargetInfo(TargetInfo ti) {
            if (ti == null)
                return "";

            StringBuilder buff = new StringBuilder();
            buff.append("<a href=\"");
            switch (ti.getTarget().type) {
                case Target.USER:
                    buff.append("borqs://profile/details?uid=").append(ti.getTarget().getIdAsLong()).append("&tab=2");
                    break;
                case Target.APK:
                    buff.append("borqs://application/details?id=").append(ObjectUtils.toString(ti.getTarget().id));
                    break;
                // TODO: other target type
            }
            buff.append("\">").append(ObjectUtils.toString(ti.getName())).append("</a>");
            return buff.toString();
        }
    };


    protected TargetInfoFormat() {
    }

    public abstract String formatTargetInfo(TargetInfo ti);

    @Override
    public final String format(Object id) {
        return formatTargetInfo((TargetInfo) id);
    }

    protected boolean targetTypeIs(TargetInfo ti, int targetType) {
        if (ti == null)
            return false;
        Target t = ti.getTarget();
        return t != null && t.type == targetType;
    }

    public String formatTarget(Context ctx, Target target) {
        TargetInfo ti = TargetInfoFetcher.getInstance().fetchTargetInfo(ctx, target);
        return formatTargetInfo(ti);
    }

    public String formatTarget(Context ctx, String target) {
        return formatTarget(ctx, Target.parseCompatibleString(target));
    }

    public String formatTargets(Context ctx, String sep, String omit, int max, Target... targets) {
        TargetInfo[] tis = TargetInfoFetcher.getInstance().multiFetchTargetInfo(ctx, targets);
        return EntityFormat.joinFormat(this, sep, omit, max, tis);
    }

    public String formatTargets(Context ctx, int max, Target... targets) {
        TargetInfo[] tis = TargetInfoFetcher.getInstance().multiFetchTargetInfo(ctx, targets);
        return EntityFormat.joinFormat(this, ",", "...", max, tis);
    }

    public String formatTargets(Context ctx, Target... targets) {
        return formatTargets(ctx, 0, targets);
    }

    public String formatTargets(Context ctx, String sep, String omit, int max, int targetType, long[] ids) {
        return formatTargets(ctx, sep, omit, max, Target.array(targetType, ids));
    }

    public String formatTargets(Context ctx, int max, int targetType, long[] ids) {
        return formatTargets(ctx, ",", "...", max, Target.array(targetType, ids));
    }

    public String formatTargets(Context ctx, int targetType, long[] ids) {
        return formatTargets(ctx, 0, targetType, ids);
    }

    public String formatTargets(Context ctx, String sep, String omit, int max, int targetType, String[] ids) {
        return formatTargets(ctx, sep, omit, max, Target.array(targetType, ids));
    }

    public String formatTargets(Context ctx, int max, int targetType, String[] ids) {
        return formatTargets(ctx, ",", "...", max, Target.array(targetType, ids));
    }

    public String formatTargets(Context ctx, int targetType, String[] ids) {
        return formatTargets(ctx, 0, targetType, ids);
    }

    public TargetInfo findTargetInfo(Context ctx, int targetType, String[] ids) {
        TargetInfo[] targetInfos = TargetInfoFetcher.getInstance().multiFetchTargetInfo(ctx, Target.array(targetType, ids));
        if (targetInfos.length == 1)
            return targetInfos[0];
        else
            throw new UnsupportedAttributeException("cannot find the TargetInfo","error");
    }
}
