package com.borqs.server.test.ignore.test1;

import com.borqs.server.impl.ignore.IgnoreDb;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.Target;
import com.borqs.server.platform.feature.ignore.IgnoreLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.test.mock.SteveAndBill;

import java.util.HashMap;
import java.util.Map;

public class IgnoreLogicTest1 extends ConfigurableTestCase {
    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(IgnoreDb.class);
    }

    private IgnoreLogic getIgnoreLogic() {
        return (IgnoreLogic) getBean("logic.ignore");
    }

    Context ctx = Context.createForViewer(SteveAndBill.STEVE_ID);

    public void testIgnore() {
        IgnoreLogic ignoreLogic = this.getIgnoreLogic();
        Target target = new Target(Target.APK, "11111");
        Target target2 = new Target(Target.APK, "22222");
        Target target3 = new Target(Target.APK, "33333");
        Target target4 = new Target(Target.APK, "44444");
        Target target5 = new Target(Target.APK, "55555");
        Target target6 = new Target(Target.APK, "66666");
        Target[] targets = {target, target2, target3, target4, target5, target6};
        ignoreLogic.ignore(ctx, 1, targets);

        Map<String, Target> map = new HashMap<String, Target>();
        for (Target t : targets)
            map.put(t.id, t);
        Target[] targetArray = ignoreLogic.getIgnored(ctx, ctx.getViewer(), 1);
        for (Target t : targetArray) {
            assertTrue(map.containsValue(t));
        }

    }

    public void testUnIgnore() {
        IgnoreLogic ignoreLogic = this.getIgnoreLogic();
        Target target = new Target(Target.APK, "11111");
        Target target2 = new Target(Target.APK, "22222");
        Target target3 = new Target(Target.APK, "33333");
        Target target4 = new Target(Target.APK, "44444");
        Target target5 = new Target(Target.APK, "55555");
        Target target6 = new Target(Target.APK, "66666");
        Target[] targets = {target, target2, target3, target4, target5, target6};
        ignoreLogic.unignore(ctx, 1, targets);
    }

    public void testGetIgnore() {
        IgnoreLogic ignoreLogic = this.getIgnoreLogic();
        Target target = new Target(Target.APK, "11111");
        Target target2 = new Target(Target.APK, "22222");
        Target target3 = new Target(Target.APK, "33333");
        Target target4 = new Target(Target.APK, "44444");
        Target target5 = new Target(Target.APK, "55555");
        Target target6 = new Target(Target.APK, "66666");
        Target[] targets = {target, target2, target3, target4, target5, target6};
        ignoreLogic.ignore(ctx, 1, targets);

        Map<String, Target> map = new HashMap<String, Target>();
        for (Target t : targets)
            map.put(t.id, t);
        Target[] targetArray = ignoreLogic.getIgnored(ctx, ctx.getViewer(), 1);
        for (Target t : targetArray) {
            assertTrue(map.containsValue(t));
        }
    }


}
