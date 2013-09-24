package com.borqs.server.test.app.test1;


import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.app.App;
import com.borqs.server.platform.feature.app.AppLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;

public class SimpleAppLogicTest1 extends ConfigurableTestCase {

    public static final int APP1_ID = 1;
    public static final String APP1_SECRET = "app1Secret";
    public static final String APP1_NAME = "Qiupu";

    public static final int APP2_ID = 2;
    public static final String APP2_SECRET = "app2Secret";
    public static final String APP2_NAME = "Brook";

    public static final int APP3_ID = 3;

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return new DBSchemaBuilder.Script[0];
    }

    private AppLogic getApp() {
        return (AppLogic)getBean("logic.app");
    }

    public void testGetApp() {
        Context ctx = Context.create();
        AppLogic app = getApp();


        App app1 = app.getApp(ctx, APP1_ID);
        assertEquals(app1.getAppId(), APP1_ID);
        assertEquals(app1.getSecret(), APP1_SECRET);
        assertEquals(app1.getName(), APP1_NAME);
        assertTrue(app.hasApp(ctx, APP1_ID));

        App app2 = app.getApp(ctx, APP2_ID);
        assertEquals(app2.getAppId(), APP2_ID);
        assertEquals(app2.getSecret(), APP2_SECRET);
        assertEquals(app2.getName(), APP2_NAME);
        assertTrue(app.hasApp(ctx, APP2_ID));

        assertNull(app.getApp(ctx, APP3_ID));
        assertFalse(app.hasApp(ctx, APP3_ID));
    }

}
