package com.borqs.server.test.setting.test1;


import com.borqs.server.ServerException;
import com.borqs.server.impl.setting.SettingImpl;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.setting.SettingLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.util.CollectionsHelper;

import java.util.Map;

public class SettingLogicTest1 extends ConfigurableTestCase {

    public static final long USER1_ID = 10001;
    public static final long USER2_ID = 10002;

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(SettingImpl.class);
    }

    private SettingLogic getSetting() {
        return (SettingLogic)getBean("logic.setting");
    }

    public void testSetAndGet() {
        SettingLogic setting = getSetting();
        Context ctx = Context.createForViewer(USER1_ID);

        // sets
        setting.sets(ctx, CollectionsHelper.of("key1", "val1", "key2", "val2"));
        assertEquals("val1", setting.get(ctx, USER1_ID, "key1", null));
        assertEquals("val2", setting.get(ctx, USER1_ID, "key2", null));
        assertEquals("def_val", setting.get(ctx, USER1_ID, "key3", "def_val"));


        // gets
        Map<String, String> m = setting.gets(ctx, USER1_ID, new String[]{"key1", "key3"}, CollectionsHelper.of("key1", "def_val1", "key3", "def_val3"));
        assertEquals(m.get("key1"), "val1");
        assertEquals(m.get("key2"), null);
        assertEquals(m.get("key3"), "def_val3");


        // set
        setting.set(ctx, "key2", "val22");
        assertEquals("val1", setting.get(ctx, USER1_ID, "key1", null));
        assertEquals("val22", setting.get(ctx, USER1_ID, "key2", null));

        // delete
        assertEquals("val1", setting.get(ctx, USER1_ID, "key1", null));
        setting.delete(ctx, "key1", "key2");
        assertEquals("def_val", setting.get(ctx, USER1_ID, "key1", "def_val"));
        assertEquals("def_val", setting.get(ctx, USER1_ID, "key2", "def_val"));

        // set error user
        try {
            setting.set(Context.createForViewer(USER2_ID), "key3", "val3");
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.INVALID_USER);
        }
    }


}
