package com.borqs.server.test.cibind.test1;


import com.borqs.server.ServerException;
import com.borqs.server.impl.cibind.CibindDb;
import com.borqs.server.platform.E;
import com.borqs.server.platform.context.Context;
import com.borqs.server.platform.feature.cibind.BindingInfo;
import com.borqs.server.platform.feature.cibind.CibindLogic;
import com.borqs.server.platform.sql.DBSchemaBuilder;
import com.borqs.server.platform.test.ConfigurableTestCase;
import com.borqs.server.platform.util.CollectionsHelper;
import org.apache.commons.lang.ArrayUtils;

public class CibindLogicTest1 extends ConfigurableTestCase {

    public static final long USER1_ID = 10001;
    public static final long USER2_ID = 10002;

    @Override
    protected DBSchemaBuilder.Script[] buildSqls() {
        return dbScriptsInClasspath(CibindDb.class);
    }

    private CibindLogic getCibindLogic() {
        return (CibindLogic)getBean("logic.cibind");
    }

    public void testCibind() {
        Context ctx = Context.createForViewer(USER1_ID);
        CibindLogic cibind = getCibindLogic();

        final String PHONE1 = "13800000001";
        final String PHONE2 = "13800000002";
        final String PHONE3 = "13800000003";
        final String EMAIL1 = "mytest@borqs.com";

        // bind for invalid user
        try {
            cibind.bind(Context.createForViewer(USER2_ID), BindingInfo.MOBILE_TEL, PHONE1);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.INVALID_USER);
        }

        // bind phone1
        cibind.bind(ctx, BindingInfo.MOBILE_TEL, PHONE1);

        // rebind phone1 error
        try {
            cibind.bind(ctx, BindingInfo.MOBILE_TEL, PHONE1);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.BINDING_EXISTS);
        }

        // bind phone2
        cibind.bind(ctx, BindingInfo.MOBILE_TEL, PHONE2);

        // bind phone3 error
        try {
            cibind.bind(ctx, BindingInfo.MOBILE_TEL, PHONE3);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.TOO_MANY_BINDING);
        }

        // bind email1
        cibind.bind(ctx, BindingInfo.EMAIL, EMAIL1);

        // bind other contact_info error
        try {
            cibind.bind(ctx, "other_contact_info", PHONE1);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.INVALID_BINDING_TYPE);
        }

        // validate bindings
        String[] phoneBindings = cibind.getBindings(ctx, USER1_ID, BindingInfo.MOBILE_TEL);
        assertTrue(CollectionsHelper.setEquals(CollectionsHelper.asSet(phoneBindings), CollectionsHelper.asSet(PHONE1, PHONE2)));
        String[] emailBindings = cibind.getBindings(ctx, USER1_ID, BindingInfo.EMAIL);
        assertTrue(CollectionsHelper.setEquals(CollectionsHelper.asSet(emailBindings), CollectionsHelper.asSet(EMAIL1)));
        assertTrue(cibind.hasBinding(ctx, PHONE1));
        assertTrue(cibind.hasBinding(ctx, PHONE2));
        assertTrue(cibind.hasBinding(ctx, EMAIL1));
        assertFalse(cibind.hasBinding(ctx, PHONE3));
        assertTrue(cibind.hasBinding(ctx, USER1_ID, PHONE1));
        assertTrue(cibind.hasBinding(ctx, USER1_ID, PHONE2));
        assertTrue(cibind.hasBinding(ctx, USER1_ID, EMAIL1));
        assertFalse(cibind.hasBinding(ctx, USER1_ID, PHONE3));


        // unbind phone1
        assertTrue(cibind.unbind(ctx, PHONE1));
        phoneBindings = cibind.getBindings(ctx, USER1_ID, BindingInfo.MOBILE_TEL);
        assertTrue(CollectionsHelper.setEquals(CollectionsHelper.asSet(phoneBindings), CollectionsHelper.asSet(PHONE2)));

        // unbind phone2
        assertTrue(cibind.unbind(ctx, PHONE2));
        phoneBindings = cibind.getBindings(ctx, USER1_ID, BindingInfo.MOBILE_TEL);
        assertTrue(ArrayUtils.isEmpty(phoneBindings));

        // unbind email1
        assertTrue(cibind.unbind(ctx, EMAIL1));
        emailBindings = cibind.getBindings(ctx, USER1_ID, BindingInfo.EMAIL);
        assertTrue(ArrayUtils.isEmpty(emailBindings));

        // unbind other
        assertFalse(cibind.unbind(ctx, PHONE3));

        // now all bindings be deleted
        assertTrue(ArrayUtils.isEmpty(cibind.getBindings(ctx, USER1_ID)));

        // unbind for invalid user
        try {
            cibind.unbind(Context.createForViewer(USER2_ID), PHONE1);
        } catch (ServerException e) {
            assertEquals(e.getCode(), E.INVALID_USER);
        }
    }
}
