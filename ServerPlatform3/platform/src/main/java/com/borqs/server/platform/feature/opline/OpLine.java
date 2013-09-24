package com.borqs.server.platform.feature.opline;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Actions;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.log.Logger;
import com.borqs.server.platform.util.DateHelper;
import org.apache.commons.lang.ObjectUtils;

public class OpLine {
    private static final Logger L = Logger.get(OpLine.class);

    private static volatile OpLineLogic instance = null;

    private OpLine() {
    }

    public static OpLineLogic getInstance() {
        return instance;
    }

    public static void setInstance(OpLineLogic instance) {
        OpLine.instance = instance;
    }

    public static void appends(Context ctx, Operation... opers) {
        logOper(ctx, opers);

        OpLineLogic opline = instance;
        if (opline == null)
            return;

        opline.appends(ctx, opers);
    }

    public static void append2(Context ctx,
                               int action1, Object info1, Target action1Target,
                               int action2, Object info2, Target action2Target) {

        long now = DateHelper.nowMillis();
        Operation[] opers = new Operation[2];
        opers[0] = Operation.newOperation(ctx, now, action1, ObjectUtils.toString(info1), action1Target);
        opers[1] = Operation.newOperation(ctx, now, action2, ObjectUtils.toString(info2), action2Target);
        logOper(ctx, opers);

        OpLineLogic opline = instance;
        if (opline == null)
            return;

        opline.appends(ctx, opers);
    }

    public static void append(Context ctx, int action, Object info, Target... targets) {
        Operation op = Operation.newOperation(ctx, DateHelper.nowMillis(), action, ObjectUtils.toString(info), targets);
        logOper(ctx, op);

        OpLineLogic opline = instance;
        if (opline == null)
            return;

        opline.appends(ctx, op);
    }

    private static void logOper(Context ctx, Operation... opers) {
        for (Operation op : opers) {
            if (op != null)
                L.oper(ctx, Actions.actionToStr(op.getAction()), op.toOperLogString());
        }
    }
}
